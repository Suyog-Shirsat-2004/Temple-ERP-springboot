package com.temple.erp.controller.api;

import com.temple.erp.model.LoginRecord;
import com.temple.erp.model.User;
import com.temple.erp.repository.LoginRecordRepository;
import com.temple.erp.repository.UserRepository;
import com.temple.erp.security.AuthPrincipal;
import com.temple.erp.security.JwtService;
import com.temple.erp.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final UserRepository userRepository;
    private final LoginRecordRepository loginRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public AuthApiController(UserRepository userRepository, LoginRecordRepository loginRecordRepository,
                             PasswordEncoder passwordEncoder, JwtService jwtService,
                             FileStorageService fileStorageService,
                             @Value("${app.admin.username:Admin123}") String adminUsername,
                             @Value("${app.admin.email:templeerp2027@gmail.com}") String adminEmail,
                             @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.loginRecordRepository = loginRecordRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.fileStorageService = fileStorageService;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        String password2 = body.get("password2");
        String firstName = body.get("first_name");
        String lastName = body.get("last_name");
        String username = body.get("username");
        String email = body.get("email");
        if (password == null || !password.equals(password2)) return ApiUtil.fail(400, "Passwords do not match");
        if (firstName == null || lastName == null || firstName.isBlank() || lastName.isBlank())
            return ApiUtil.fail(400, "First and Last Name are required");
        if (username != null && userRepository.existsByUsername(username)) return ApiUtil.fail(400, "Username already exists");
        // Multiple accounts can share the same email ID
        User user = new User();
        user.setName(firstName + " " + lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("message", "Registration successful");
        res.put("user", Map.of("id", user.getId(), "name", user.getName(), "email", nz(user.getEmail())));
        return ResponseEntity.status(201).body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String username = asString(body.get("username"));
        String password = asString(body.get("password"));
        boolean remember = Boolean.TRUE.equals(body.get("remember")) || "true".equals(String.valueOf(body.get("remember")));
        if (username == null || password == null || username.isBlank() || password.isBlank())
            return ApiUtil.fail(400, "Please enter username and password");
        Optional<User> userOpt = userRepository.findFirstByEmailOrUsername(username, username);
        if (userOpt.isEmpty()) userOpt = userRepository.findByEmailOrUsername(username, username);
        if (userOpt.isEmpty()) {
            recordLogin(null, username, req, "password", false, null);
            return ApiUtil.fail(401, "User not found. Please register first.");
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordLogin(user.getId(), nz(user.getUsername(), user.getEmail()), req, "password", false, null);
            return ApiUtil.fail(401, "Invalid password. Please try again.");
        }
        recordLogin(user.getId(), nz(user.getUsername(), user.getEmail()), req, "password", true, parseDeviceInfo(req.getHeader("User-Agent")));
        String token = jwtService.generateUserToken(user.getId(), remember);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("message", "Welcome back, " + user.getName() + "!");
        res.put("token", token);
        res.put("user", userPayload(user));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody Map<String, String> body, HttpServletRequest req) {
        String username = body.get("username");
        String password = body.get("password");
        boolean idOk = username != null && (adminEmail.equalsIgnoreCase(username) || adminUsername.equalsIgnoreCase(username));
        if (idOk && adminPassword.equals(password)) {
            recordLogin(null, "admin", req, "admin", true, parseDeviceInfo(req.getHeader("User-Agent")));
            String token = jwtService.generateAdminToken();
            Map<String, Object> res = ApiUtil.body(true);
            res.put("message", "Admin login successful");
            res.put("token", token);
            res.put("user", Map.of("id", "admin", "name", adminUsername, "role", "admin"));
            return ResponseEntity.ok(res);
        }
        recordLogin(null, username == null ? "unknown" : username, req, "admin", false, null);
        return ApiUtil.fail(401, "Invalid admin credentials");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newPassword = body.get("new_password");
        String confirm = body.get("confirm_password");
        if (email == null || email.isBlank()) return ApiUtil.fail(400, "Email is required");
        if (newPassword == null || !newPassword.equals(confirm)) return ApiUtil.fail(400, "Passwords do not match");
        if (newPassword.length() < 6) return ApiUtil.fail(400, "Password must be at least 6 characters");
        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        if (userOpt.isEmpty()) userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ApiUtil.fail(404, "Email not found");
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ApiUtil.ok("Password reset successfully");
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        if (p.isAdmin()) return ApiUtil.fail(400, "Admin password cannot be changed here");
        String current = body.get("current_password");
        String next = body.get("new_password");
        String confirm = body.get("confirm_password");
        if (next == null || !next.equals(confirm)) return ApiUtil.fail(400, "Passwords do not match");
        User user = userRepository.findById(p.getId()).orElse(null);
        if (user == null) return ApiUtil.fail(404, "User not found");
        if (!passwordEncoder.matches(current, user.getPassword())) return ApiUtil.fail(400, "Current password is incorrect");
        user.setPassword(passwordEncoder.encode(next));
        userRepository.save(user);
        return ApiUtil.ok("Password changed successfully");
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Map<String, Object> res = ApiUtil.body(true);
        if (p.isAdmin()) {
            res.put("user", Map.of("id", "admin", "name", "Admin", "role", "admin", "email", adminEmail));
            return ResponseEntity.ok(res);
        }
        User user = userRepository.findById(p.getId()).orElse(null);
        if (user == null) return ApiUtil.fail(404, "User not found");
        res.put("user", user);
        return ResponseEntity.ok(res);
    }

    @PutMapping(value = "/profile", consumes = {"multipart/form-data", "application/x-www-form-urlencoded"})
    public ResponseEntity<Map<String, Object>> updateProfile(Authentication auth,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String address,
            @RequestParam(required = false, name = "profile_pic") MultipartFile profilePic) throws Exception {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        if (p.isAdmin()) return ApiUtil.fail(400, "Admin profile update not supported");
        User user = userRepository.findById(p.getId()).orElse(null);
        if (user == null) return ApiUtil.fail(404, "User not found");
        if (name != null) user.setName(name);
        if (email != null) user.setEmail(email);
        if (mobile != null) user.setMobile(mobile);
        if (address != null) user.setAddress(address);
        if (profilePic != null && !profilePic.isEmpty()) {
            user.setProfilePic(fileStorageService.store(profilePic, "profile", "profile_pic"));
        }
        userRepository.save(user);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("message", "Profile updated successfully");
        res.put("user", user);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/login-history")
    public ResponseEntity<Map<String, Object>> loginHistory(Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Page<LoginRecord> records;
        PageRequest pr = PageRequest.of(Math.max(0, page - 1), limit);
        if (p.isAdmin()) records = loginRecordRepository.findAllByOrderByLoginAtDesc(pr);
        else records = loginRecordRepository.findByUserIdOrderByLoginAtDesc(p.getId(), pr);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("records", records.getContent());
        res.put("pagination", Map.of("page", page, "limit", limit, "total", records.getTotalElements(), "pages", records.getTotalPages()));
        return ResponseEntity.ok(res);
    }

    private void recordLogin(String userId, String username, HttpServletRequest req, String method, boolean success, String deviceInfo) {
        try {
            LoginRecord r = new LoginRecord();
            r.setUserId(userId);
            r.setUsername(username);
            r.setIpAddress(req.getRemoteAddr());
            r.setUserAgent(req.getHeader("User-Agent"));
            r.setLoginMethod(method);
            r.setSuccess(success);
            r.setDeviceInfo(deviceInfo);
            loginRecordRepository.save(r);
        } catch (Exception ignored) {}
    }

    private String parseDeviceInfo(String ua) {
        if (ua == null) return "Unknown";
        StringBuilder sb = new StringBuilder();
        if (ua.toLowerCase().contains("windows")) sb.append("Windows");
        else if (ua.toLowerCase().contains("macintosh") || ua.toLowerCase().contains("mac os")) sb.append("macOS");
        else if (ua.toLowerCase().contains("linux")) sb.append("Linux");
        String l = ua.toLowerCase();
        if (l.contains("chrome") && !l.contains("edge")) append(sb, "Chrome");
        else if (l.contains("firefox")) append(sb, "Firefox");
        else if (l.contains("safari") && !l.contains("chrome")) append(sb, "Safari");
        else if (l.contains("edge")) append(sb, "Edge");
        return sb.length() == 0 ? ua.substring(0, Math.min(50, ua.length())) : sb.toString();
    }
    private void append(StringBuilder sb, String s) { if (sb.length() > 0) sb.append(", "); sb.append(s); }

    private Map<String, Object> userPayload(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("name", user.getName());
        m.put("email", user.getEmail());
        m.put("mobile", user.getMobile());
        m.put("profile_pic", user.getProfilePic());
        return m;
    }
    private String nz(String... vals) { for (String v : vals) if (v != null) return v; return ""; }
    private String asString(Object o) { return o == null ? null : String.valueOf(o); }
}
