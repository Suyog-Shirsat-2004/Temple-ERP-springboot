package com.temple.erp.controller.api;

import com.temple.erp.model.User;
import com.temple.erp.repository.DarshanPassRepository;
import com.temple.erp.repository.DonationRepository;
import com.temple.erp.repository.RestaurantOrderRepository;
import com.temple.erp.repository.RoomBookingRepository;
import com.temple.erp.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final UserRepository userRepository;
    private final DarshanPassRepository passRepository;
    private final DonationRepository donationRepository;
    private final RoomBookingRepository bookingRepository;
    private final RestaurantOrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminApiController(UserRepository userRepository, DarshanPassRepository passRepository,
                              DonationRepository donationRepository, RoomBookingRepository bookingRepository,
                              RestaurantOrderRepository orderRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passRepository = passRepository;
        this.donationRepository = donationRepository;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        var passes = passRepository.findAll();
        var bookings = bookingRepository.findAll();
        var donations = donationRepository.findAll();
        var orders = orderRepository.findAll();
        long pendingPasses = passes.stream().filter(p -> "pending".equalsIgnoreCase(p.getStatus())).count();
        long pendingBookings = bookings.stream().filter(b -> "Pending".equalsIgnoreCase(b.getBookingStatus())).count();
        long pendingRestaurant = orders.stream().filter(o -> "pending".equalsIgnoreCase(o.getStatus())).count();
        long pendingDonations = donations.stream().filter(d -> d.getPaymentStatus() != null && d.getPaymentStatus().toLowerCase().contains("pending")).count();
        double restaurantRevenue = orders.stream().filter(o -> "paid".equalsIgnoreCase(o.getPaymentStatus())).mapToDouble(o -> o.getTotalAmount() == null ? 0 : o.getTotalAmount()).sum();
        double donationsAmount = donations.stream().filter(d -> isPaid(d.getPaymentStatus())).mapToDouble(d -> d.getAmount() == null ? 0 : d.getAmount()).sum();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_visitors", userRepository.count());
        stats.put("total_passes", passes.size());
        stats.put("total_bookings", bookings.size());
        stats.put("total_donations", donations.size());
        stats.put("total_donations_amount", donationsAmount);
        stats.put("total_restaurant_orders", orders.size());
        stats.put("pending_passes", pendingPasses);
        stats.put("pending_bookings", pendingBookings);
        stats.put("pending_donations", pendingDonations);
        stats.put("pending_restaurant_orders", pendingRestaurant);
        stats.put("total_pending", pendingPasses + pendingBookings + pendingRestaurant + pendingDonations);
        stats.put("restaurant_revenue", restaurantRevenue);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("stats", stats);
        res.put("recent_passes", passes.stream().sorted((a,b)->cmp(b.getId(),a.getId())).limit(5).collect(Collectors.toList()));
        res.put("recent_bookings", bookings.stream().sorted((a,b)->cmp(b.getId(),a.getId())).limit(5).collect(Collectors.toList()));
        res.put("recent_donations", donations.stream().sorted((a,b)->cmp(b.getId(),a.getId())).limit(5).collect(Collectors.toList()));
        res.put("recent_orders", orders.stream().sorted((a,b)->cmp(b.getId(),a.getId())).limit(5).collect(Collectors.toList()));
        res.put("monthly_donations", List.of());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> users() {
        var users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));
        Map<String, Object> res = ApiUtil.body(true);
        res.put("users", users);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, String> body) {
        User u = new User();
        u.setName(body.get("name")); u.setUsername(body.get("username")); u.setEmail(body.get("email"));
        u.setMobile(body.get("mobile"));
        if (body.get("password") != null) u.setPassword(passwordEncoder.encode(body.get("password")));
        userRepository.save(u);
        u.setPassword(null);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("message", "User added successfully");
        res.put("user", Map.of("id", u.getId(), "name", nz(u.getName()), "email", nz(u.getEmail())));
        return ResponseEntity.status(201).body(res);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String id, @RequestBody Map<String, String> body) {
        User u = userRepository.findById(id).orElse(null);
        if (u == null) return ApiUtil.fail(404, "User not found");
        if (body.get("name") != null) u.setName(body.get("name"));
        if (body.get("email") != null) u.setEmail(body.get("email"));
        if (body.containsKey("mobile")) u.setMobile(body.get("mobile"));
        if (body.get("status") != null) u.setStatus(body.get("status"));
        if (body.get("password") != null && !body.get("password").isBlank()) u.setPassword(passwordEncoder.encode(body.get("password")));
        userRepository.save(u);
        return ApiUtil.ok("User updated successfully");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id) {
        if (!userRepository.existsById(id)) return ApiUtil.fail(404, "User not found");
        userRepository.deleteById(id);
        return ApiUtil.ok("User deleted successfully");
    }

    @GetMapping("/devotees")
    public ResponseEntity<Map<String, Object>> devotees() {
        var users = userRepository.findAll();
        var list = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId()); m.put("_id", u.getId()); m.put("name", u.getName()); m.put("email", u.getEmail());
            m.put("mobile", u.getMobile()); m.put("status", u.getStatus()); m.put("role", u.getRole());
            var userPasses = passRepository.findByUserIdOrderByIdDesc(u.getId());
            m.put("total_passes", userPasses.size());
            double spent = userPasses.stream().filter(p -> "paid".equalsIgnoreCase(p.getPaymentStatus())).mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount()).sum();
            m.put("total_spent", spent);
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> res = ApiUtil.body(true);
        res.put("devotees", list);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/devotees/{id}")
    public ResponseEntity<Map<String, Object>> devoteeDetails(@PathVariable String id) {
        User u = userRepository.findById(id).orElse(null);
        if (u == null) return ApiUtil.fail(404, "User not found");
        u.setPassword(null);
        var passes = passRepository.findByUserIdOrderByIdDesc(id);
        var bookings = bookingRepository.findByUserIdOrderByIdDesc(id);
        var donations = donationRepository.findByUserIdOrderByIdDesc(id);
        var orders = orderRepository.findByUserIdOrderByIdDesc(id);
        double totalSpent = passes.stream().mapToDouble(p -> nz(p.getTotalAmount(), p.getAmount())).sum()
                + bookings.stream().mapToDouble(b -> nz(b.getTotalAmount())).sum()
                + donations.stream().mapToDouble(d -> nz(d.getAmount())).sum();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("user", u); res.put("passes", passes); res.put("bookings", bookings);
        res.put("donations", donations); res.put("orders", orders); res.put("total_spent", totalSpent);
        return ResponseEntity.ok(res);
    }

    private boolean isPaid(String s) { return s != null && java.util.Set.of("completed","success","paid").contains(s.toLowerCase()); }
    private int cmp(String a, String b) { if (a==null) return 1; if (b==null) return -1; return b.compareTo(a); }
    private String nz(String s) { return s == null ? "" : s; }
    private double nz(Double... v) { for (Double d : v) if (d != null) return d; return 0; }
}
