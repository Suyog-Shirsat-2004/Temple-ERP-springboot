package com.temple.erp.controller.api;

import com.temple.erp.model.User;
import com.temple.erp.repository.DarshanPassRepository;
import com.temple.erp.repository.DonationRepository;
import com.temple.erp.repository.NotificationRepository;
import com.temple.erp.repository.RestaurantOrderRepository;
import com.temple.erp.repository.RoomBookingRepository;
import com.temple.erp.repository.UserRepository;
import com.temple.erp.security.AuthPrincipal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {
    private final UserRepository userRepository;
    private final DarshanPassRepository passRepository;
    private final DonationRepository donationRepository;
    private final RoomBookingRepository bookingRepository;
    private final RestaurantOrderRepository orderRepository;
    private final NotificationRepository notificationRepository;

    public DashboardApiController(UserRepository userRepository, DarshanPassRepository passRepository,
                                  DonationRepository donationRepository, RoomBookingRepository bookingRepository,
                                  RestaurantOrderRepository orderRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.passRepository = passRepository;
        this.donationRepository = donationRepository;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> dashboard(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        User user = userRepository.findById(p.getId()).orElse(null);
        var passes = passRepository.findByUserIdOrderByIdDesc(p.getId());
        var donations = donationRepository.findByUserIdOrderByIdDesc(p.getId());
        var bookings = bookingRepository.findByUserIdOrderByIdDesc(p.getId());
        var orders = orderRepository.findByUserIdOrderByIdDesc(p.getId());
        double totalDonationAmount = donations.stream()
                .filter(d -> "completed".equalsIgnoreCase(d.getPaymentStatus()))
                .mapToDouble(d -> d.getAmount() == null ? 0 : d.getAmount()).sum();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("user_data", user);
        res.put("passes", passes.stream().limit(5).toList());
        res.put("donations", donations.stream().limit(5).toList());
        res.put("bookings", bookings.stream().limit(3).toList());
        res.put("orders", orders.stream().limit(5).toList());
        res.put("total_passes", passes.size());
        res.put("total_donations", donations.size());
        res.put("total_bookings", bookings.size());
        res.put("total_orders", orders.size());
        res.put("total_donation_amount", totalDonationAmount);
        res.put("all_messages", java.util.List.of());
        res.put("unread_count", notificationRepository.countByUserIdAndIsReadFalse(p.getId()));
        res.put("last_logout", "First login");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notifications(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("notifications", notificationRepository.findByUserIdOrderByCreatedAtDesc(p.getId()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/notifications/unread")
    public ResponseEntity<Map<String, Object>> unread(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("count", notificationRepository.countByUserIdAndIsReadFalse(p.getId()));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/notifications/read")
    public ResponseEntity<Map<String, Object>> markRead(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        var list = notificationRepository.findByUserIdAndIsReadFalse(p.getId());
        list.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(list);
        return ApiUtil.ok("Notifications marked as read");
    }
}
