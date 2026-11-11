package com.temple.erp.controller.web;

import com.temple.erp.model.DarshanPass;
import com.temple.erp.model.Donation;
import com.temple.erp.model.Notification;
import com.temple.erp.model.RestaurantOrder;
import com.temple.erp.model.Room;
import com.temple.erp.model.RoomBooking;
import com.temple.erp.model.User;
import com.temple.erp.repository.DarshanPassRepository;
import com.temple.erp.repository.DonationRepository;
import com.temple.erp.repository.EventRepository;
import com.temple.erp.repository.FestivalRepository;
import com.temple.erp.repository.NewsRepository;
import com.temple.erp.repository.NotificationRepository;
import com.temple.erp.repository.PassTypeRepository;
import com.temple.erp.repository.RestaurantMenuRepository;
import com.temple.erp.repository.RestaurantOrderRepository;
import com.temple.erp.repository.RoomBookingRepository;
import com.temple.erp.repository.RoomRepository;
import com.temple.erp.repository.TempleUpdateRepository;
import com.temple.erp.repository.UserRepository;
import com.temple.erp.security.AuthPrincipal;
import com.temple.erp.service.EmailService;
import com.temple.erp.service.FileStorageService;
import com.temple.erp.service.PdfService;
import com.temple.erp.service.QrCodeService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PassTypeRepository passTypeRepository;
    private final DarshanPassRepository passRepository;
    private final DonationRepository donationRepository;
    private final RoomRepository roomRepository;
    private final RoomBookingRepository bookingRepository;
    private final RestaurantMenuRepository menuRepository;
    private final RestaurantOrderRepository orderRepository;
    private final NotificationRepository notificationRepository;
    private final FestivalRepository festivalRepository;
    private final EventRepository eventRepository;
    private final NewsRepository newsRepository;
    private final TempleUpdateRepository templeUpdateRepository;
    private final FileStorageService fileStorageService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final QrCodeService qrCodeService;

    public WebController(UserRepository userRepository, PasswordEncoder passwordEncoder, PassTypeRepository passTypeRepository,
                         DarshanPassRepository passRepository, DonationRepository donationRepository, RoomRepository roomRepository,
                         RoomBookingRepository bookingRepository, RestaurantMenuRepository menuRepository,
                         RestaurantOrderRepository orderRepository, NotificationRepository notificationRepository,
                         FestivalRepository festivalRepository, EventRepository eventRepository, NewsRepository newsRepository,
                         TempleUpdateRepository templeUpdateRepository, FileStorageService fileStorageService,
                         PdfService pdfService, EmailService emailService, QrCodeService qrCodeService) {
        this.userRepository = userRepository; this.passwordEncoder = passwordEncoder; this.passTypeRepository = passTypeRepository;
        this.passRepository = passRepository; this.donationRepository = donationRepository; this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository; this.menuRepository = menuRepository; this.orderRepository = orderRepository;
        this.notificationRepository = notificationRepository; this.festivalRepository = festivalRepository;
        this.eventRepository = eventRepository; this.newsRepository = newsRepository; this.templeUpdateRepository = templeUpdateRepository;
        this.fileStorageService = fileStorageService;
        this.pdfService = pdfService; this.emailService = emailService; this.qrCodeService = qrCodeService;
    }

    private String resolveProfileEmail(String userId, String fallbackEmail) {
        if (userId != null && !userId.isBlank()) {
            try {
                String profile = userRepository.findById(userId).map(User::getEmail).filter(e -> e != null && !e.isBlank()).map(String::trim).orElse(null);
                if (profile != null) return profile;
            } catch (Exception ignored) {}
        }
        return (fallbackEmail != null && !fallbackEmail.isBlank()) ? fallbackEmail.trim() : null;
    }

    private String resolveProfileName(String userId, String fallbackName) {
        if (userId != null && !userId.isBlank()) {
            try {
                String profile = userRepository.findById(userId).map(User::getName).filter(n -> n != null && !n.isBlank()).map(String::trim).orElse(null);
                if (profile != null) return profile;
            } catch (Exception ignored) {}
        }
        return (fallbackName != null && !fallbackName.isBlank()) ? fallbackName.trim() : null;
    }

    @GetMapping({"/", "/home"})
    public String home(Authentication auth, Model model, HttpServletRequest request) {
        // Ensure session exists before rendering (large template + CSRF token processing)
        if (request != null) {
            request.getSession(true);
        }
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthPrincipal p) {
            model.addAttribute("principal", p);
            if (!p.isAdmin()) {
                userRepository.findById(p.getId()).ifPresent(u -> model.addAttribute("user", u));
                model.addAttribute("unreadCount", notificationRepository.countByUserIdAndIsReadFalse(p.getId()));
            }
        }
        // Render the public home page for everyone (temple_erp parity); dashboard remains at /dashboard
        return "home";
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthPrincipal p) {
            return p.isAdmin() ? "redirect:/admin/dashboard" : "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthPrincipal p) {
            return p.isAdmin() ? "redirect:/admin/dashboard" : "redirect:/dashboard";
        }
        return "admin-login";
    }

    @GetMapping("/register")
    public String register() { return "register"; }

    @GetMapping("/forgot-password")
    public String forgotPassword() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String doForgotPassword(@RequestParam String email, @RequestParam String new_password,
                                   @RequestParam String confirm_password, RedirectAttributes ra) {
        if (email == null || email.isBlank()) { ra.addFlashAttribute("error", "Email is required"); return "redirect:/forgot-password"; }
        if (!new_password.equals(confirm_password)) { ra.addFlashAttribute("error", "Wrong information. Passwords do not match. Please enter proper information and try again."); return "redirect:/forgot-password"; }
        if (new_password.length() < 6) { ra.addFlashAttribute("error", "Wrong information. Password must be at least 6 characters. Please enter proper information and try again."); return "redirect:/forgot-password"; }
        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        if (userOpt.isEmpty()) userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) { ra.addFlashAttribute("error", "Wrong information. The email address was not found. Please enter proper information and try again."); return "redirect:/forgot-password"; }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(new_password));
        userRepository.save(user);
        ra.addFlashAttribute("success", "Password reset successfully. Please log in.");
        return "redirect:/login";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String first_name, @RequestParam String last_name,
                             @RequestParam String username, @RequestParam String email,
                             @RequestParam String password, @RequestParam String password2,
                             RedirectAttributes ra) {
        String emailTrim = email == null ? "" : email.trim();
        if (!emailTrim.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            ra.addFlashAttribute("error", "Please enter a valid email address. Invalid email is not allowed.");
            return "redirect:/register";
        }
        if (!password.equals(password2)) { ra.addFlashAttribute("error", "Wrong information. Passwords do not match. Please enter proper information and try again."); return "redirect:/register"; }
        if (userRepository.existsByUsername(username)) { ra.addFlashAttribute("error", "Wrong information. Username already exists. Please choose a different username."); return "redirect:/register"; }
        // Multiple accounts can share the same email ID
        User u = new User(); u.setName((first_name == null ? "" : first_name.trim()) + " " + (last_name == null ? "" : last_name.trim()));
        u.setUsername(username == null ? null : username.trim()); u.setEmail(emailTrim);
        u.setPassword(passwordEncoder.encode(password)); userRepository.save(u);
        ra.addFlashAttribute("success", "Registration successful. Please log in.");
        return "redirect:/login";
    }

    // Aliases to match original MERN frontend routes
    @GetMapping("/passes") public String passesAlias() { return "redirect:/passes/history"; }
    @GetMapping("/bookings") public String bookingsAlias(Authentication auth, Model model) { return bookingForm(auth, model); }
    @GetMapping("/restaurant/orders") public String restaurantOrdersAlias(Authentication auth, Model model) { return orderHistory(auth, model); }
    @GetMapping("/admin") public String adminAlias() { return "redirect:/admin/dashboard"; }

    @GetMapping("/events")
    public String events(Model model) { model.addAttribute("events", eventRepository.findAllByOrderByEventDateDesc()); return "events"; }
    @GetMapping("/festivals")
    public String festivals(Model model) { model.addAttribute("festivals", festivalRepository.findAllByOrderByEventDateDesc()); return "festivals"; }
    @GetMapping("/news")
    public String news(Model model) { model.addAttribute("news", newsRepository.findByStatusOrderByPublishedAtDesc("active")); return "news"; }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("userName", "Devotee");
        model.addAttribute("passes", List.of());
        model.addAttribute("donations", List.of());
        model.addAttribute("bookings", List.of());
        model.addAttribute("orders", List.of());
        model.addAttribute("unreadCount", 0L);
        try {
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthPrincipal p)) {
                return "redirect:/login";
            }
            if (p.isAdmin()) return "redirect:/admin/dashboard";
            String uid = p.getId();
            model.addAttribute("userName", p.getName() != null && !p.getName().isBlank() ? p.getName() : "Devotee");
            if (uid == null || uid.isBlank()) {
                return "dashboard";
            }
            try { model.addAttribute("passes", passRepository.findByUserIdOrderByIdDesc(uid).stream().limit(5).toList()); } catch (Exception ignored) {}
            try { model.addAttribute("donations", donationRepository.findByUserIdOrderByIdDesc(uid).stream().limit(5).toList()); } catch (Exception ignored) {}
            try { model.addAttribute("bookings", bookingRepository.findByUserIdOrderByIdDesc(uid).stream().limit(5).toList()); } catch (Exception ignored) {}
            try { model.addAttribute("orders", orderRepository.findByUserIdOrderByIdDesc(uid).stream().limit(5).toList()); } catch (Exception ignored) {}
            try { model.addAttribute("unreadCount", notificationRepository.countByUserIdAndIsReadFalse(uid)); } catch (Exception ignored) {}
            return "dashboard";
        } catch (Exception e) {
            return "dashboard";
        }
    }

    @GetMapping("/passes/new")
    public String newPass(Authentication auth, Model model) {
        model.addAttribute("passTypes", passTypeRepository.findByIsActiveTrueOrderByPriceAsc());
        java.util.List<String> visitedTimes = new java.util.ArrayList<>();
        try {
            if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
                visitedTimes = passRepository.findByUserIdOrderByIdDesc(p.getId()).stream()
                        .map(com.temple.erp.model.DarshanPass::getVisitTime)
                        .filter(t -> t != null && !t.trim().isEmpty())
                        .map(String::trim)
                        .distinct()
                        .limit(12)
                        .toList();
            }
        } catch (Exception ignored) {}
        model.addAttribute("visitedTimes", visitedTimes);
        return "pass-new";
    }

    @PostMapping("/passes/new")
    public String createPass(Authentication auth, @RequestParam String pass_type, @RequestParam(required=false) String payment_method,
                             @RequestParam String devotee_name, @RequestParam(required=false) String phone,
                             @RequestParam(required=false) String email, @RequestParam(required=false) String gender,
                             @RequestParam(required=false) String address, @RequestParam(required=false) String visit_date,
                             @RequestParam(required=false) String visit_time, @RequestParam(defaultValue="1") Integer no_of_persons,
                             @RequestParam(required=false) String aadhar_number,
                             @RequestParam(required=false) String trust_id,
                             @RequestParam(required=false) String transaction_id,
                             @RequestParam(required=false, name="aadhar_card") MultipartFile aadharCard,
                             RedirectAttributes ra) throws Exception {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        String phoneNorm = phone == null ? "" : phone.trim();
        String emailNorm = email == null ? "" : email.trim();
        String aadharNorm = aadhar_number == null ? "" : aadhar_number.trim();
        if (!phoneNorm.matches("^[0-9]{10}$")) {
            ra.addFlashAttribute("error", "Phone number must be exactly 10 digits. Extra digits are not allowed and less than 10 digits is not accepted.");
            return "redirect:/passes/new";
        }
        if (!emailNorm.matches("^[A-Za-z][A-Za-z0-9._%+\\-]*@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            ra.addFlashAttribute("error", "Please enter a valid email address (e.g. abs1223@gmail.com). Capital first letter is allowed.");
            return "redirect:/passes/new";
        }
        if (!aadharNorm.matches("^[0-9]{12}$")) {
            ra.addFlashAttribute("error", "Aadhar number must be exactly 12 digits. Extra digits are not allowed and less than 12 digits is not accepted.");
            return "redirect:/passes/new";
        }
        phone = phoneNorm;
        email = emailNorm;
        aadhar_number = aadharNorm;
        var ptOpt = passTypeRepository.findActiveByPassTypeRegex("^" + java.util.regex.Pattern.quote(pass_type) + "$");
        double amount = ptOpt.map(t -> t.getPrice() == null ? 0.0 : t.getPrice()).orElseGet(() -> {
            String lt = pass_type == null ? "" : pass_type.toLowerCase();
            if (lt.contains("trust") || lt.contains("complimentary") || lt.contains("free") || lt.contains("guest entry")) return 0.0;
            if (lt.contains("vip")) return 200.0;
            if (lt.contains("special")) return 100.0;
            if (lt.contains("general")) return 50.0;
            return 50.0;
        });
        String typeName = ptOpt.map(t -> t.getPassType()).orElse(pass_type);
        String lowerName = typeName == null ? "" : typeName.toLowerCase();
        boolean freeTrustEntry = amount <= 0.0
                || lowerName.contains("trust")
                || lowerName.contains("complimentary")
                || lowerName.contains("free")
                || lowerName.contains("guest entry")
                || lowerName.contains("special guest")
                || lowerName.contains("special person");
        if (no_of_persons == null || no_of_persons < 1) no_of_persons = 1;

        DarshanPass pass = new DarshanPass();
        pass.setPassId("DSP" + System.currentTimeMillis() + (int)(Math.random()*900+100));
        pass.setUserId(p.getId()); pass.setDevoteeName(devotee_name); pass.setPhone(phone); pass.setEmail(email);
        pass.setGender(gender); pass.setAddress(address);
        if (visit_date != null && !visit_date.isBlank()) pass.setVisitDate(LocalDate.parse(visit_date));
        pass.setVisitTime(visit_time); pass.setNoOfPersons(no_of_persons); pass.setPassType(typeName);
        pass.setAadharNumber(aadhar_number == null ? "" : aadhar_number);
        String refId = (trust_id != null && !trust_id.isBlank()) ? trust_id.trim() : (transaction_id != null ? transaction_id.trim() : null);
        if (refId != null && !refId.isBlank()) pass.setTransactionId(refId);
        if (aadharCard != null && !aadharCard.isEmpty()) {
            try {
                pass.setAadharCard(fileStorageService.store(aadharCard, "passes", "aadhar_card", FileStorageService.ALLOWED_IMAGES_AND_PDF));
            } catch (IllegalArgumentException ex) {
                ra.addFlashAttribute("error", ex.getMessage());
                return "redirect:/passes/new";
            } catch (Exception ex) {
                ra.addFlashAttribute("error", "Could not upload Aadhar card/document. Please try again with a valid file.");
                return "redirect:/passes/new";
            }
        }

        if (freeTrustEntry) {
            pass.setAmount(0.0);
            pass.setTotalAmount(0.0);
            pass.setPaymentMethod((refId != null && !refId.isBlank()) ? "Trust ID / Free Entry (Ref: " + refId + ")" : "Trust ID / Free Entry");
            pass.setPaymentStatus("not_required"); // no payment; admin formality checkup + approval required
            pass.setStatus("pending");
        } else {
            pass.setAmount(amount);
            pass.setTotalAmount(amount * no_of_persons);
            String pm = (payment_method == null || payment_method.isBlank()) ? "UPI" : payment_method;
            pass.setPaymentMethod(pm);
            boolean upiLike = pm.equalsIgnoreCase("upi") || pm.equalsIgnoreCase("qr") || pm.toLowerCase().contains("upi");
            pass.setPaymentStatus(upiLike ? "paid" : "pending");
            // status remains pending for admin approval workflow
        }

        passRepository.save(pass);
        // Send submitted pass PDF to user's profile email in background (avoids slow submit if SMTP/PDF is slow)
        final String emailTo = resolveProfileEmail(p.getId(), pass.getEmail());
        final String nameForMail = resolveProfileName(p.getId(), pass.getDevoteeName());
        final String profilePicForMail;
        String tmpProfilePic = null;
        try { tmpProfilePic = userRepository.findById(p.getId()).map(User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        profilePicForMail = tmpProfilePic;
        final DarshanPass passForMail = pass;
        if (emailTo != null && !emailTo.isBlank()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    byte[] pdf = pdfService.generatePassPdf(passForMail, nameForMail, profilePicForMail);
                    boolean sent = emailService.sendPdf(emailTo,
                            "Temple ERP - Darshan Pass " + passForMail.getPassId() + " (Submitted)",
                            "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour Darshan Pass request " + passForMail.getPassId() + " has been submitted (status: " + (passForMail.getStatus() != null ? passForMail.getStatus() : "pending") + "). The pass PDF is attached. It becomes valid after admin approval.\n\n- Temple ERP",
                            pdf, "Temple Darshan Pass - original pass.pdf");
                    if (!sent) {
                        org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Pass submission PDF email not sent for {} to {}: {}", passForMail.getPassId(), emailTo, emailService.statusHint(emailTo));
                    }
                } catch (Exception ex) {
                    org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Background pass submission email failed for {}: {}", passForMail.getPassId(), ex.getMessage());
                }
            });
        }
        // Notification/count is added after admin approval/rejection (not on submission)
        String msg = freeTrustEntry
                ? "Free/Trust ID pass request submitted (formality checkup & admin approval required). Pass ID: " + pass.getPassId()
                : "Darshan Pass submitted! Pass ID: " + pass.getPassId();
        if (emailTo != null && !emailTo.isBlank()) msg += " Pass PDF will be emailed to " + emailTo + " shortly.";
        else msg += " PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        ra.addFlashAttribute("success", msg);
        return "redirect:/passes/history";
    }

    @GetMapping("/passes/history")
    public String passHistory(Authentication auth, Model model) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        model.addAttribute("passes", passRepository.findByUserIdOrderByIdDesc(p.getId()));
        return "pass-history";
    }

    @GetMapping("/passes/{id}")
    public String passDetail(@PathVariable String id, Model model) {
        var pass = passRepository.findByPassId(id).or(() -> passRepository.findById(id)).orElse(null);
        model.addAttribute("pass", pass);
        if (pass != null) {
            model.addAttribute("pdfMode", false);
            model.addAttribute("displayName", pass.getDevoteeName());
            if (pass.getUserId() != null) {
                userRepository.findById(pass.getUserId()).ifPresent(u -> {
                    model.addAttribute("passOwner", u);
                    model.addAttribute("passOwnerProfilePic", u.getProfilePic());
                    if (u.getName() != null && !u.getName().isBlank()) model.addAttribute("displayName", u.getName());
                });
            }
            if (pass.getQrCode() != null && !pass.getQrCode().isBlank()) {
                model.addAttribute("passQrImageUrl", "/uploads/passes/qr/" + pass.getQrCode());
            }
            model.addAttribute("passQrToken", pass.getQrToken());
        }
        return "pass-detail";
    }

    @PostMapping("/passes/{id}/send-pdf")
    public String sendPassPdf(@PathVariable String id, Authentication auth, RedirectAttributes ra) {
        var passOpt = passRepository.findByPassId(id).or(() -> passRepository.findById(id));
        if (passOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Pass not found.");
            return "redirect:/passes/history";
        }
        var pass = passOpt.get();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p)) {
            ra.addFlashAttribute("error", "Please log in to send the pass PDF.");
            return "redirect:/passes/" + id;
        }
        if (!p.isAdmin() && (pass.getUserId() == null || !pass.getUserId().equals(p.getId()))) {
            ra.addFlashAttribute("error", "You can send PDF only for your own pass.");
            return "redirect:/passes/" + id;
        }
        boolean emailSent = false;
        String emailTo = resolveProfileEmail(pass.getUserId(), pass.getEmail());
        try {
            if ("approved".equalsIgnoreCase(pass.getStatus())) {
                try { String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(); qrCodeService.ensurePassQr(pass, baseUrl); passRepository.save(pass); } catch (Exception ignored) {}
            }
            if (emailTo != null && !emailTo.isBlank()) {
                String name = resolveProfileName(pass.getUserId(), pass.getDevoteeName());
                String profilePic = null;
                try { if (pass.getUserId() != null) profilePic = userRepository.findById(pass.getUserId()).map(User::getProfilePic).orElse(null); } catch (Exception ignored) {}
                byte[] pdf = pdfService.generatePassPdf(pass, name, profilePic);
                String passId = pass.getPassId() != null ? pass.getPassId() : pass.getId();
                String status = pass.getStatus() != null ? pass.getStatus() : "pending";
                emailSent = emailService.sendPdf(emailTo,
                        "Temple ERP - Darshan Pass " + passId + " (" + status + ")",
                        "Dear " + (name != null ? name : "Devotee") + ",\n\nYour Darshan Pass " + passId + " PDF is attached (status: " + status + ").\n\n- Temple ERP",
                        pdf, "Temple Darshan Pass - original pass.pdf");
            }
        } catch (Exception ignored) {}
        if (emailSent) ra.addFlashAttribute("success", "Pass PDF sent to " + emailTo + ".");
        else ra.addFlashAttribute("error", "Pass PDF not sent (" + emailService.statusHint(emailTo) + "). Configure Admin > Email Settings and ensure your profile email is set.");
        return "redirect:/passes/" + id;
    }

    @GetMapping("/passes/verify/{token}")
    public String verifyPass(@PathVariable String token, Model model) {
        var pass = passRepository.findByQrToken(token).orElse(null);
        model.addAttribute("pass", pass);
        model.addAttribute("token", token);
        if (pass != null && pass.getUserId() != null) {
            userRepository.findById(pass.getUserId()).ifPresent(u -> model.addAttribute("passOwner", u));
        }
        if (pass != null && pass.getQrCode() != null && !pass.getQrCode().isBlank()) {
            model.addAttribute("passQrImageUrl", "/uploads/passes/qr/" + pass.getQrCode());
        }
        return "pass-verify";
    }

    @GetMapping("/passes/{id}/pdf")
    public ResponseEntity<byte[]> downloadPassPdf(@PathVariable String id, Authentication auth) {
        var passOpt = passRepository.findByPassId(id).or(() -> passRepository.findById(id));
        if (passOpt.isEmpty()) return ResponseEntity.notFound().build();
        var pass = passOpt.get();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
            if (pass.getUserId() == null || !pass.getUserId().equals(p.getId())) return ResponseEntity.status(403).build();
        }
        if ("approved".equalsIgnoreCase(pass.getStatus())) {
            try { String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(); qrCodeService.ensurePassQr(pass, baseUrl); passRepository.save(pass); } catch (Exception ignored) {}
        }
        String name = resolveProfileName(pass.getUserId(), pass.getDevoteeName());
        String profilePic = null;
        try { if (pass.getUserId() != null) profilePic = userRepository.findById(pass.getUserId()).map(User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        byte[] pdf = pdfService.generatePassPdf(pass, name, profilePic);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Temple Darshan Pass - original pass.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/donations")
    public String donations(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthPrincipal p)) {
            return "redirect:/login";
        }
        if (p.isAdmin()) {
            return "redirect:/admin/donations";
        }
        var list = donationRepository.findByUserIdOrderByIdDesc(p.getId());
        model.addAttribute("donations", list);
        model.addAttribute("total", list.stream().mapToDouble(d -> d.getAmount()==null?0:d.getAmount()).sum());
        return "donations";
    }

    @GetMapping("/donations/receipt/{id}")
    public String donationReceipt(@PathVariable String id, Authentication auth, Model model, RedirectAttributes ra) {
        var donation = donationRepository.findByReceiptNo(id)
                .or(() -> donationRepository.findById(id))
                .orElse(null);
        if (donation == null) {
            ra.addFlashAttribute("error", "Donation receipt not found.");
            return "redirect:/donations";
        }
        // allow owner or admin
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
            if (donation.getUserId() == null || !donation.getUserId().equals(p.getId())) {
                ra.addFlashAttribute("error", "You can view only your own donation receipt.");
                return "redirect:/donations";
            }
        }
        model.addAttribute("donation", donation);
        if (donation.getUserId() != null) {
            userRepository.findById(donation.getUserId()).ifPresent(u -> {
                model.addAttribute("donorUser", u);
                model.addAttribute("donorProfilePic", u.getProfilePic());
            });
        }
        return "donation-receipt";
    }

    @GetMapping({"/donations/receipt/{id}/pdf", "/donations/{id}/pdf"})
    public ResponseEntity<byte[]> downloadDonationReceiptPdf(@PathVariable String id, Authentication auth) {
        var donation = donationRepository.findByReceiptNo(id).or(() -> donationRepository.findById(id)).orElse(null);
        if (donation == null) return ResponseEntity.notFound().build();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
            if (donation.getUserId() == null || !donation.getUserId().equals(p.getId())) return ResponseEntity.status(403).build();
        }
        String name = resolveProfileName(donation.getUserId(), donation.getName());
        String profilePic = null;
        try { if (donation.getUserId() != null) profilePic = userRepository.findById(donation.getUserId()).map(User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        byte[] pdf = pdfService.generateDonationReceiptPdf(donation, name, profilePic);
        String ref = donation.getReceiptNo() != null ? donation.getReceiptNo() : donation.getId();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"donation-receipt-" + ref + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/donations")
    public String createDonation(Authentication auth, @RequestParam Double amount, @RequestParam(required=false) String donation_type,
                                 @RequestParam(defaultValue="UPI") String payment_method, @RequestParam(required=false) String message,
                                 RedirectAttributes ra) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Donation d = new Donation(); d.setUserId(p.getId()); d.setReceiptNo("DON"+System.currentTimeMillis()+ (int)(Math.random()*900+100));
        d.setName(p.getName()); d.setEmail(p.getEmail()); d.setAmount(amount);
        d.setDonationType(donation_type == null ? "General Donation" : donation_type);
        d.setPaymentMethod(payment_method); d.setPaymentStatus("completed"); d.setNotes(message); d.setDonationDate(Instant.now());
        donationRepository.save(d);
        final String emailTo = resolveProfileEmail(p.getId(), d.getEmail());
        final String nameForMail = resolveProfileName(p.getId(), d.getName());
        final Donation dForMail = d;
        final Double amountForMail = amount;
        if (emailTo != null && !emailTo.isBlank()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    byte[] pdf = pdfService.generateDonationReceiptPdf(dForMail, nameForMail);
                    boolean sent = emailService.sendPdf(emailTo,
                            "Temple ERP - Donation Receipt " + dForMail.getReceiptNo(),
                            "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nThank you for your donation of Rs. " + amountForMail + " (" + dForMail.getDonationType() + "). Your donation receipt " + dForMail.getReceiptNo() + " is attached.\n\n- Temple ERP",
                            pdf, "donation-receipt-" + dForMail.getReceiptNo() + ".pdf");
                    if (!sent) {
                        org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Donation receipt email not sent for {} to {}: {}", dForMail.getReceiptNo(), emailTo, emailService.statusHint(emailTo));
                    }
                } catch (Exception ex) {
                    org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Background donation email failed for {}: {}", dForMail.getReceiptNo(), ex.getMessage());
                }
            });
        }
        notify(p.getId(), "Donation Received", "Thank you! Your donation of Rs." + amount + " (" + d.getDonationType() + ") was recorded. Receipt: " + d.getReceiptNo() + (emailTo != null && !emailTo.isBlank() ? ". Receipt PDF will be emailed to " + emailTo + " shortly." : ""), "donation");
        String msg = "Donation recorded. Receipt: " + d.getReceiptNo();
        if (emailTo != null && !emailTo.isBlank()) msg += " Receipt PDF will be emailed to " + emailTo + " shortly.";
        else msg += " Receipt PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        ra.addFlashAttribute("success", msg);
        return "redirect:/donations";
    }

    @GetMapping("/bookings/new")
    public String bookingForm(Authentication auth, Model model) {
        var rooms = roomRepository.findByStatus("available");
        if (rooms == null || rooms.isEmpty()) {
            try {
                rooms = roomRepository.findAll().stream()
                        .filter(r -> r != null && r.getStatus() != null && "available".equalsIgnoreCase(r.getStatus().trim()))
                        .toList();
            } catch (Exception ignored) { rooms = java.util.List.of(); }
        }
        model.addAttribute("rooms", rooms);
        try {
            if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
                model.addAttribute("myBookings", bookingRepository.findByUserIdOrderByIdDesc(p.getId()).stream().limit(10).toList());
            }
        } catch (Exception ignored) {}
        return "room-booking";
    }

    @PostMapping("/bookings/new")
    public String createBooking(Authentication auth, @RequestParam String room_id, @RequestParam String check_in,
                                @RequestParam String check_out, @RequestParam String guest_name,
                                @RequestParam(required=false) String phone, @RequestParam(required=false) String email,
                                @RequestParam(required=false) Integer no_of_guests,
                                @RequestParam(required=false, defaultValue="UPI") String payment_method,
                                RedirectAttributes ra) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Room room = roomRepository.findById(room_id).orElse(null);
        if (room == null) { ra.addFlashAttribute("error", "Room not found"); return "redirect:/bookings/new"; }
        if (room.getStatus() == null || !"available".equalsIgnoreCase(room.getStatus().trim())) {
            ra.addFlashAttribute("error", "Selected room is not available. Please choose another available room.");
            return "redirect:/bookings/new";
        }

        String phoneDigits = phone == null ? "" : phone.replaceAll("\\D", "");
        String emailTrim = email == null ? "" : email.trim();
        if (!phoneDigits.matches("^\\d{10}$")) {
            ra.addFlashAttribute("error", "Phone number must be exactly 10 digits (numbers only). Invalid phone is not allowed.");
            return "redirect:/bookings/new?room_id=" + room_id;
        }
        if (emailTrim.isEmpty() || !emailTrim.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            ra.addFlashAttribute("error", "Please enter a valid email address. Invalid email is not allowed.");
            return "redirect:/bookings/new?room_id=" + room_id;
        }
        String guestName = guest_name == null ? "" : guest_name.trim();
        if (guestName.length() < 2) {
            ra.addFlashAttribute("error", "Guest name is required.");
            return "redirect:/bookings/new?room_id=" + room_id;
        }

        LocalDate in = LocalDate.parse(check_in); LocalDate out = LocalDate.parse(check_out);
        long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));
        double total = (room.getPricePerDay()==null?0:room.getPricePerDay()) * nights;
        if (no_of_guests == null || no_of_guests < 1) no_of_guests = 1;
        Integer capacity = room.getCapacity();
        if (capacity != null && capacity > 0 && no_of_guests > capacity) {
            ra.addFlashAttribute("error", "Room capacity is " + capacity + " person(s). Please enter guests within the room capacity.");
            return "redirect:/bookings/new?room_id=" + room_id;
        }

        RoomBooking b = new RoomBooking(); b.setBookingId("RMB"+System.currentTimeMillis()+ (int)(Math.random()*900+100));
        b.setUserId(p.getId()); b.setRoomId(room_id); b.setGuestName(guestName);
        b.setPhone(phoneDigits); b.setEmail(emailTrim);
        b.setCheckIn(in); b.setCheckOut(out); b.setRoomType(room.getRoomType()); b.setNoOfGuests(no_of_guests);
        b.setTotalAmount(total); b.setPaymentMethod(payment_method);
        // User pays during booking; admin approval is required before token/status becomes approved.
        if (payment_method != null && !payment_method.isBlank()) {
            b.setPaymentStatus("Paid");
            b.setPaymentDate(Instant.now());
        }
        b.setBookingStatus("Pending");
        bookingRepository.save(b);
        final String emailTo = resolveProfileEmail(p.getId(), b.getEmail());
        final String nameForMail = resolveProfileName(p.getId(), b.getGuestName());
        final String profilePicForMail;
        String tmpProfilePic = null;
        try { tmpProfilePic = userRepository.findById(p.getId()).map(User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        profilePicForMail = tmpProfilePic;
        final RoomBooking bForMail = b;
        if (emailTo != null && !emailTo.isBlank()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    byte[] pdf = pdfService.generateBookingTokenPdf(bForMail, nameForMail, profilePicForMail);
                    boolean sent = emailService.sendPdf(emailTo,
                            "Temple ERP - Room Booking Token " + bForMail.getBookingId() + " (Submitted)",
                            "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour room booking " + bForMail.getBookingId() + " has been submitted (status: " + (bForMail.getBookingStatus() != null ? bForMail.getBookingStatus() : "Pending") + "). The booking token PDF is attached. It becomes valid after admin approval.\n\n- Temple ERP",
                            pdf, "room-booking-token-" + bForMail.getBookingId() + "-submitted.pdf");
                    if (!sent) {
                        org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Booking token email not sent for {} to {}: {}", bForMail.getBookingId(), emailTo, emailService.statusHint(emailTo));
                    }
                } catch (Exception ex) {
                    org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Background booking email failed for {}: {}", bForMail.getBookingId(), ex.getMessage());
                }
            });
        }
        // Notification/count is added after admin approval/rejection (not on submission)
        String msg = "Booking submitted: " + b.getBookingId() + ". Payment recorded (Rs." + total + "). Waiting for admin approval - token will be available after approval.";
        if (emailTo != null && !emailTo.isBlank()) msg += " Token PDF will be emailed to " + emailTo + " shortly.";
        else msg += " Token PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        ra.addFlashAttribute("success", msg);
        return "redirect:/bookings/history";
    }

    @GetMapping("/bookings/history")
    public String bookingHistory(Authentication auth, Model model) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        model.addAttribute("bookings", bookingRepository.findByUserIdOrderByIdDesc(p.getId()));
        return "booking-history";
    }

    @GetMapping({"/bookings/token/{id}", "/bookings/{id}/token"})
    public String bookingToken(@PathVariable String id, Authentication auth, Model model, RedirectAttributes ra) {
        var booking = bookingRepository.findByBookingId(id).or(() -> bookingRepository.findById(id)).orElse(null);
        if (booking == null) {
            ra.addFlashAttribute("error", "Booking token not found.");
            return "redirect:/bookings/history";
        }
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
            if (booking.getUserId() == null || !booking.getUserId().equals(p.getId())) {
                ra.addFlashAttribute("error", "You can view only your own booking token.");
                return "redirect:/bookings/history";
            }
        }
        String st = booking.getBookingStatus() == null ? "" : booking.getBookingStatus().toLowerCase();
        boolean approved = st.equals("confirmed") || st.equals("approved");
        model.addAttribute("tokenApproved", approved);
        model.addAttribute("booking", booking);
        if (booking.getUserId() != null) {
            userRepository.findById(booking.getUserId()).ifPresent(u -> {
                model.addAttribute("bookingUser", u);
                model.addAttribute("bookingUserProfilePic", u.getProfilePic());
            });
        }
        return "booking-token";
    }

    @GetMapping({"/bookings/token/{id}/pdf", "/bookings/{id}/pdf"})
    public ResponseEntity<byte[]> downloadBookingTokenPdf(@PathVariable String id, Authentication auth) {
        var booking = bookingRepository.findByBookingId(id).or(() -> bookingRepository.findById(id)).orElse(null);
        if (booking == null) return ResponseEntity.notFound().build();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
            if (booking.getUserId() == null || !booking.getUserId().equals(p.getId())) return ResponseEntity.status(403).build();
        }
        String name = resolveProfileName(booking.getUserId(), booking.getGuestName());
        String profilePic = null;
        try { if (booking.getUserId() != null) profilePic = userRepository.findById(booking.getUserId()).map(User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        byte[] pdf = pdfService.generateBookingTokenPdf(booking, name, profilePic);
        String ref = booking.getBookingId() != null ? booking.getBookingId() : booking.getId();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"room-booking-token-" + ref + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/restaurant")
    public String restaurant(Authentication auth, Model model) {
        java.util.List<com.temple.erp.model.RestaurantMenuItem> menuItems;
        try {
            menuItems = menuRepository.findByAvailableTrue();
            if (menuItems == null) menuItems = java.util.List.of();
        } catch (Exception e) {
            menuItems = java.util.List.of();
        }
        // Safety: if query misses items with null 'available' (legacy), show items not explicitly unavailable
        if (menuItems.isEmpty()) {
            try {
                menuItems = menuRepository.findAll().stream()
                        .filter(m -> m.getAvailable() == null || Boolean.TRUE.equals(m.getAvailable()))
                        .toList();
            } catch (Exception ignored) {}
        }
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("restroImageOptions", java.util.List.of("re1.jpg", "re2.jpg", "rre3.jpg", "re4.jpg", "re5.jpg", "re7.webp"));
        try {
            if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
                model.addAttribute("myOrders", orderRepository.findByUserIdOrderByIdDesc(p.getId()).stream().limit(8).toList());
            }
        } catch (Exception ignored) {}
        return "restaurant";
    }

    @PostMapping("/restaurant/order")
    public String placeOrder(Authentication auth,
                             @RequestParam(required=false, name="item_name") java.util.List<String> itemNames,
                             @RequestParam(required=false, name="item_price") java.util.List<Double> itemPrices,
                             @RequestParam(required=false, name="qty") java.util.List<Integer> qtys,
                             @RequestParam(required=false, defaultValue="UPI") String payment_method,
                             RedirectAttributes ra) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        java.util.List<RestaurantOrder.OrderItem> items = new java.util.ArrayList<>();
        double total = 0.0;
        int n = itemNames == null ? 0 : itemNames.size();
        for (int i = 0; i < n; i++) {
            String name = itemNames.get(i) == null ? "" : itemNames.get(i).trim();
            Double price = (itemPrices != null && i < itemPrices.size()) ? itemPrices.get(i) : null;
            Integer q = (qtys != null && i < qtys.size()) ? qtys.get(i) : 1;
            if (name.isEmpty() || price == null) continue;
            if (q == null || q < 1) q = 1;
            if (q > 50) q = 50;
            if (price < 0) price = 0.0;
            RestaurantOrder.OrderItem oi = new RestaurantOrder.OrderItem();
            oi.setName(name);
            oi.setPrice(price);
            oi.setQty(q);
            items.add(oi);
            total += price * q;
        }
        // Backward-compatible fallback (single-item legacy fields)
        if (items.isEmpty()) { ra.addFlashAttribute("error","Select at least one item"); return "redirect:/restaurant"; }

        RestaurantOrder o = new RestaurantOrder();
        o.setOrderId("ORD"+System.currentTimeMillis()+ (int)(Math.random()*900+100));
        o.setUserId(p.getId());
        o.setItems(items);
        o.setTotalAmount(Math.round(total * 100.0) / 100.0); // server-side source of truth
        o.setName(p.getName()); o.setOrderDate(LocalDate.now()); o.setOrderTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        o.setPaymentMethod(payment_method);
        o.setStatus("pending"); // requires admin approval before token is shown
        if (payment_method != null && !payment_method.isBlank() && !"cash".equalsIgnoreCase(payment_method)) {
            o.setPaymentStatus("paid");
            o.setPaymentDate(Instant.now());
        } else if ("cash".equalsIgnoreCase(payment_method)) {
            o.setPaymentStatus("pending");
        }
        orderRepository.save(o);
        final String emailTo = resolveProfileEmail(p.getId(), null);
        final String nameForMail = resolveProfileName(p.getId(), o.getName());
        final RestaurantOrder oForMail = o;
        if (emailTo != null && !emailTo.isBlank()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    byte[] pdf = pdfService.generateRestaurantTokenPdf(oForMail, nameForMail);
                    boolean sent = emailService.sendPdf(emailTo,
                            "Temple ERP - Restaurant Order Token " + oForMail.getOrderId() + " (Submitted)",
                            "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nYour restaurant order " + oForMail.getOrderId() + " has been submitted (status: " + (oForMail.getStatus() != null ? oForMail.getStatus() : "pending") + "). The order token PDF is attached. It becomes valid after admin approval.\n\n- Temple ERP",
                            pdf, "restaurant-token-" + oForMail.getOrderId() + "-submitted.pdf");
                    if (!sent) {
                        org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Restaurant token email not sent for {} to {}: {}", oForMail.getOrderId(), emailTo, emailService.statusHint(emailTo));
                    }
                } catch (Exception ex) {
                    org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Background restaurant email failed for {}: {}", oForMail.getOrderId(), ex.getMessage());
                }
            });
        }
        // Notification/count is added after admin approval/status update (not on submission)
        String msg = "Order placed: " + o.getOrderId() + " (" + items.size() + " item" + (items.size()>1?"s":"") + ", " + payment_method + "). Waiting for admin approval - token will be available after approval.";
        if (emailTo != null && !emailTo.isBlank()) msg += " Token PDF will be emailed to " + emailTo + " shortly.";
        else msg += " Token PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        ra.addFlashAttribute("success", msg);
        return "redirect:/restaurant/history";
    }

    @GetMapping("/restaurant/history")
    public String orderHistory(Authentication auth, Model model) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        model.addAttribute("orders", orderRepository.findByUserIdOrderByIdDesc(p.getId()));
        return "order-history";
    }

    @GetMapping({"/restaurant/token/{id}", "/restaurant/orders/{id}"})
    public String orderToken(@PathVariable String id, Authentication auth, Model model, RedirectAttributes ra) {
        var order = orderRepository.findByOrderId(id).or(() -> orderRepository.findById(id)).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Order token not found.");
            return "redirect:/restaurant/history";
        }
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
            if (order.getUserId() == null || !order.getUserId().equals(p.getId())) {
                ra.addFlashAttribute("error", "You can view only your own order token.");
                return "redirect:/restaurant/history";
            }
        }
        String st = order.getStatus() == null ? "" : order.getStatus().toLowerCase();
        boolean approved = !(st.equals("pending") || st.equals("cancelled") || st.equals("rejected") || st.isEmpty());
        model.addAttribute("tokenApproved", approved);
        model.addAttribute("order", order);
        if (order.getUserId() != null) {
            userRepository.findById(order.getUserId()).ifPresent(u -> {
                model.addAttribute("orderUser", u);
                model.addAttribute("orderUserProfilePic", u.getProfilePic());
            });
        }
        return "order-token";
    }

    @GetMapping({"/restaurant/token/{id}/pdf", "/restaurant/orders/{id}/pdf"})
    public ResponseEntity<byte[]> downloadRestaurantTokenPdf(@PathVariable String id, Authentication auth) {
        var order = orderRepository.findByOrderId(id).or(() -> orderRepository.findById(id)).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
            if (order.getUserId() == null || !order.getUserId().equals(p.getId())) return ResponseEntity.status(403).build();
        }
        String name = resolveProfileName(order.getUserId(), order.getName());
        String profilePic = null;
        try { if (order.getUserId() != null) profilePic = userRepository.findById(order.getUserId()).map(User::getProfilePic).orElse(null); } catch (Exception ignored) {}
        byte[] pdf = pdfService.generateRestaurantTokenPdf(order, name, profilePic);
        String ref = order.getOrderId() != null ? order.getOrderId() : order.getId();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"restaurant-token-" + ref + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/notifications")
    public String notifications(Authentication auth, Model model) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        model.addAttribute("notifications", notificationRepository.findByUserIdOrderByCreatedAtDesc(p.getId()));
        return "notifications";
    }

    @PostMapping("/notifications/read")
    public String markRead(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        var list = notificationRepository.findByUserIdAndIsReadFalse(p.getId());
        list.forEach(n -> n.setIsRead(true)); notificationRepository.saveAll(list);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/delete")
    public String deleteNotifications(Authentication auth,
                                      @RequestParam(value = "ids", required = false) java.util.List<String> ids,
                                      @RequestParam(value = "id", required = false) String id,
                                      RedirectAttributes ra) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        try {
            java.util.LinkedHashSet<String> toDelete = new java.util.LinkedHashSet<>();
            if (ids != null) {
                for (String v : ids) {
                    if (v != null && !v.isBlank()) toDelete.add(v.trim());
                }
            }
            if (id != null && !id.isBlank()) toDelete.add(id.trim());

            if (toDelete.isEmpty()) {
                ra.addFlashAttribute("error", "Select at least one notification to delete.");
                return "redirect:/notifications";
            }

            var owned = notificationRepository.findAllById(toDelete).stream()
                    .filter(n -> n != null && n.getUserId() != null && n.getUserId().equals(p.getId()))
                    .toList();
            if (owned.isEmpty()) {
                ra.addFlashAttribute("error", "No matching notifications found to delete.");
                return "redirect:/notifications";
            }
            notificationRepository.deleteAll(owned);
            ra.addFlashAttribute("success", owned.size() + " notification(s) deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not delete notifications. Please try again.");
        }
        return "redirect:/notifications";
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        if (!p.isAdmin()) {
            userRepository.findById(p.getId()).ifPresent(u -> model.addAttribute("user", u));
            try {
                var myPasses = passRepository.findByUserIdOrderByIdDesc(p.getId());
                model.addAttribute("passesCount", myPasses.size());
                model.addAttribute("recentPasses", myPasses.stream().limit(3).toList());
            } catch (Exception e) {
                model.addAttribute("passesCount", 0);
                model.addAttribute("recentPasses", List.of());
            }
        } else {
            model.addAttribute("passesCount", 0);
            model.addAttribute("recentPasses", List.of());
        }
        model.addAttribute("principal", p);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication auth,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String mobile,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String gender,
                                @RequestParam(required = false, name = "profile_pic") MultipartFile profilePic,
                                RedirectAttributes ra) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        if (p.isAdmin()) {
            ra.addFlashAttribute("error", "Admin profile is managed via configuration.");
            return "redirect:/profile";
        }
        Optional<User> userOpt = userRepository.findById(p.getId());
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User account not found.");
            return "redirect:/profile";
        }
        try {
            User user = userOpt.get();
            String emailTrim = email == null ? "" : email.trim();
            String mobileDigits = mobile == null ? "" : mobile.replaceAll("\\D", "");
            if (!emailTrim.isEmpty() && !emailTrim.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
                ra.addFlashAttribute("error", "Please enter a valid email address. Invalid email is not allowed.");
                return "redirect:/profile";
            }
            if (!mobileDigits.isEmpty() && !mobileDigits.matches("^\\d{10}$")) {
                ra.addFlashAttribute("error", "Mobile number must be exactly 10 digits (numbers only). Invalid mobile is not allowed.");
                return "redirect:/profile";
            }
            // Multiple accounts can share the same email ID (no uniqueness check)
            if (name != null && !name.isBlank()) user.setName(name.trim());
            if (email != null) user.setEmail(emailTrim.isEmpty() ? null : emailTrim);
            if (mobile != null) user.setMobile(mobileDigits.isEmpty() ? null : mobileDigits);
            if (address != null) user.setAddress(address.trim());
            if (gender != null) user.setGender(gender.trim());
            if (profilePic != null && !profilePic.isEmpty() && profilePic.getOriginalFilename() != null && !profilePic.getOriginalFilename().isBlank()) {
                String stored = fileStorageService.store(profilePic, "profile", "profile_pic");
                user.setProfilePic(stored);
            }
            userRepository.save(user);
            ra.addFlashAttribute("success", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Invalid file format. Allowed: jpg, jpeg, png, gif, webp.");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(WebController.class).warn("Profile update failed for user {}: {}", p.getId(), e.getMessage(), e);
            String msg = e.getMessage();
            if (msg != null && (msg.toLowerCase().contains("duplicate") || msg.toLowerCase().contains("e11000"))) {
                ra.addFlashAttribute("error", "This email is already in use by another account. Multiple accounts can share the same email only after restarting the app to remove the old unique email index.");
            } else {
                ra.addFlashAttribute("error", "Could not update profile. " + (msg != null && !msg.isBlank() ? msg : "Please try again."));
            }
        }
        return "redirect:/profile";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/services")
    public String services(Authentication auth, Model model) {
        model.addAttribute("passTypes", passTypeRepository.findByIsActiveTrueOrderByPriceAsc());
        model.addAttribute("rooms", roomRepository.findByStatus("available"));
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthPrincipal p && !p.isAdmin()) {
            String uid = p.getId();
            try {
                var passes = passRepository.findByUserIdOrderByIdDesc(uid);
                model.addAttribute("totalPasses", passes.size());
                model.addAttribute("previousPasses", passes.stream().limit(8).toList());
            } catch (Exception e) { model.addAttribute("totalPasses", 0); model.addAttribute("previousPasses", java.util.List.of()); }
            try {
                var bookings = bookingRepository.findByUserIdOrderByIdDesc(uid);
                model.addAttribute("totalBookings", bookings.size());
                model.addAttribute("myBookings", bookings.stream().limit(10).toList());
            } catch (Exception e) { model.addAttribute("totalBookings", 0); model.addAttribute("myBookings", java.util.List.of()); }
            try {
                var dons = donationRepository.findByUserIdOrderByIdDesc(uid);
                model.addAttribute("totalDonations", dons.size());
                model.addAttribute("totalDonationAmount", dons.stream().filter(d -> d.getAmount() != null && d.getPaymentStatus() != null && (d.getPaymentStatus().equalsIgnoreCase("completed") || d.getPaymentStatus().equalsIgnoreCase("paid") || d.getPaymentStatus().equalsIgnoreCase("Success"))).mapToDouble(com.temple.erp.model.Donation::getAmount).sum());
            } catch (Exception e) { model.addAttribute("totalDonations", 0); model.addAttribute("totalDonationAmount", 0.0); }
            try { model.addAttribute("totalOrders", orderRepository.findByUserIdOrderByIdDesc(uid).size()); } catch (Exception e) { model.addAttribute("totalOrders", 0); }
            try {
                var notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(uid);
                model.addAttribute("serviceNotifications", notifs.stream().limit(12).toList());
                model.addAttribute("bookingNotifications", notifs.stream().filter(n -> {
                    String t = ((n.getType()==null?"":n.getType()) + " " + (n.getTitle()==null?"":n.getTitle()) + " " + (n.getMessage()==null?"":n.getMessage())).toLowerCase();
                    return t.contains("book") || t.contains("room");
                }).limit(8).toList());
            } catch (Exception e) { model.addAttribute("serviceNotifications", java.util.List.of()); model.addAttribute("bookingNotifications", java.util.List.of()); }
        }
        return "services";
    }

    @GetMapping("/temple-updates")
    public String templeUpdates(@RequestParam(required = false) String type, Model model) {
        var updates = (type == null || type.isBlank() || "all".equalsIgnoreCase(type))
                ? templeUpdateRepository.findByStatusOrderByCreatedAtDesc("active")
                : templeUpdateRepository.findByStatusAndUpdateTypeOrderByCreatedAtDesc("active", type.toLowerCase());
        if (updates.isEmpty() && (type == null || type.isBlank() || "all".equalsIgnoreCase(type))) {
            updates = templeUpdateRepository.findAllByOrderByCreatedAtDesc();
        }
        model.addAttribute("updates", updates);
        model.addAttribute("selectedType", type == null || type.isBlank() ? "all" : type.toLowerCase());
        return "temple-updates";
    }

    @GetMapping("/change-password")
    public String changePassword(Authentication auth, Model model) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        model.addAttribute("principal", p);
        return "change-password";
    }

    @PostMapping("/change-password")
    public String doChangePassword(Authentication auth,
                                   @RequestParam(required = false) String old_password,
                                   @RequestParam String new_password,
                                   @RequestParam String confirm_password,
                                   RedirectAttributes ra) {
        if (new_password == null || !new_password.equals(confirm_password)) {
            ra.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/change-password";
        }
        if (new_password.length() < 6) {
            ra.addFlashAttribute("error", "Password must be at least 6 characters");
            return "redirect:/change-password";
        }
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Optional<User> userOpt = userRepository.findById(p.getId());
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Password changes for this account are managed via configuration.");
            return "redirect:/change-password";
        }
        User user = userOpt.get();
        if (old_password == null || old_password.isBlank() || !passwordEncoder.matches(old_password, user.getPassword())) {
            ra.addFlashAttribute("error", "Old password is incorrect");
            return "redirect:/change-password";
        }
        user.setPassword(passwordEncoder.encode(new_password));
        userRepository.save(user);
        ra.addFlashAttribute("success", "Password changed successfully.");
        return "redirect:/profile";
    }

    // PHP temple_erp route parity aliases
    @GetMapping({"/darshan_pass", "/darshan_pass/new"}) public String darshanPassAlias() { return "redirect:/passes/new"; }
    @GetMapping({"/darshan_pass/history", "/pass_history", "/my_passes"}) public String darshanHistoryAlias() { return "redirect:/passes/history"; }
    @GetMapping({"/darshan_pass/view_pass/{id}", "/darshan_pass/{id}"}) public String darshanViewAlias(@PathVariable String id) { return "redirect:/passes/" + id; }
    @GetMapping({"/room_booking", "/room_booking/new"}) public String roomBookingAlias() { return "redirect:/bookings/new"; }
    @GetMapping({"/room_bookings_history", "/room_booking/history", "/my_bookings"}) public String roomHistoryAlias() { return "redirect:/bookings/history"; }
    @GetMapping({"/donations/donate", "/my_donations"}) public String donationsAlias() { return "redirect:/donations"; }
    @GetMapping({"/restaurant/order"}) public String restaurantOrderAlias() { return "redirect:/restaurant"; }

    private void notify(String userId, String title, String message, String type) {
        try { if (userId == null) return; Notification n = new Notification(); n.setUserId(userId); n.setTitle(title); n.setMessage(message); n.setType(type); notificationRepository.save(n);} catch (Exception ignored) {}
    }
}
