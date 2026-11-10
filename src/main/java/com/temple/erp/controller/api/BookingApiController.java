package com.temple.erp.controller.api;

import com.temple.erp.model.Notification;
import com.temple.erp.model.Room;
import com.temple.erp.model.RoomBooking;
import com.temple.erp.repository.NotificationRepository;
import com.temple.erp.repository.RoomBookingRepository;
import com.temple.erp.repository.RoomRepository;
import com.temple.erp.repository.UserRepository;
import com.temple.erp.security.AuthPrincipal;
import com.temple.erp.service.EmailService;
import com.temple.erp.service.PdfService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {
    private final RoomRepository roomRepository;
    private final RoomBookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PdfService pdfService;

    public BookingApiController(RoomRepository roomRepository, RoomBookingRepository bookingRepository,
                                NotificationRepository notificationRepository, UserRepository userRepository,
                                EmailService emailService, PdfService pdfService) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.pdfService = pdfService;
    }

    private String resolveEmail(String userId, String fallback) {
        if (userId != null && !userId.isBlank()) {
            try { String e = userRepository.findById(userId).map(u -> u.getEmail()).filter(x -> x != null && !x.isBlank()).map(String::trim).orElse(null); if (e != null) return e; } catch (Exception ignored) {}
        }
        return fallback != null && !fallback.isBlank() ? fallback.trim() : null;
    }
    private String resolveName(String userId, String fallback) {
        if (userId != null && !userId.isBlank()) {
            try { String n = userRepository.findById(userId).map(u -> u.getName()).filter(x -> x != null && !x.isBlank()).map(String::trim).orElse(null); if (n != null) return n; } catch (Exception ignored) {}
        }
        return fallback != null && !fallback.isBlank() ? fallback.trim() : null;
    }
    private boolean sendToken(RoomBooking b, String statusLabel) {
        try {
            String to = resolveEmail(b.getUserId(), b.getEmail());
            if (to == null || to.isBlank()) return false;
            String name = resolveName(b.getUserId(), b.getGuestName());
            byte[] pdf = pdfService.generateBookingTokenPdf(b, name);
            String tokenId = b.getBookingId() != null ? b.getBookingId() : b.getId();
            return emailService.sendPdf(to,
                    "Temple ERP - Room Booking Token " + tokenId + " (" + statusLabel + ")",
                    "Dear " + (name != null ? name : "Devotee") + ",\n\nYour room booking " + tokenId + " status: " + statusLabel + ". The booking token PDF is attached.\n\n- Temple ERP",
                    pdf, "room-booking-token-" + tokenId + ".pdf");
        } catch (Exception e) { return false; }
    }

    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> rooms() {
        var rooms = roomRepository.findByStatus("available");
        if (rooms == null || rooms.isEmpty()) {
            try {
                rooms = roomRepository.findAll().stream()
                        .filter(r -> r != null && r.getStatus() != null && "available".equalsIgnoreCase(r.getStatus().trim()))
                        .toList();
            } catch (Exception ignored) { rooms = java.util.List.of(); }
        }
        Map<String, Object> res = ApiUtil.body(true);
        res.put("rooms", rooms);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/rooms/all")
    public ResponseEntity<Map<String, Object>> allRooms() {
        Map<String, Object> res = ApiUtil.body(true);
        res.put("rooms", roomRepository.findAll());
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> userBookings(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("bookings", bookingRepository.findByUserIdOrderByIdDesc(p.getId()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> allBookings() {
        Map<String, Object> res = ApiUtil.body(true);
        res.put("bookings", bookingRepository.findAllByOrderByIdDesc());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        RoomBooking b = bookingRepository.findById(id).orElse(null);
        if (b == null) b = bookingRepository.findByBookingId(id).orElse(null);
        if (b == null) return ApiUtil.fail(404, "Booking not found");
        Map<String, Object> res = ApiUtil.body(true);
        res.put("booking", b);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body, Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        String roomId = s(body.get("room_id"));
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return ApiUtil.fail(404, "Room not found");
        LocalDate in = LocalDate.parse(s(body.get("check_in")).substring(0, 10));
        LocalDate out = LocalDate.parse(s(body.get("check_out")).substring(0, 10));
        long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));
        double total = (room.getPricePerDay() == null ? 0 : room.getPricePerDay()) * nights;
        RoomBooking b = new RoomBooking();
        b.setBookingId("RMB" + System.currentTimeMillis() + (int) (Math.floor(Math.random() * 900) + 100));
        b.setUserId(p.getId());
        b.setRoomId(roomId);
        b.setGuestName(s(body.get("guest_name")));
        b.setPhone(s(body.get("phone")));
        b.setEmail(s(body.get("email")));
        b.setCheckIn(in);
        b.setCheckOut(out);
        b.setRoomType(room.getRoomType());
        b.setNoOfGuests(body.get("no_of_guests") instanceof Number n ? n.intValue() : parseInt(body.get("no_of_guests")));
        b.setTotalAmount(total);
        b.setPaymentStatus("Pending");
        b.setBookingStatus("Pending");
        bookingRepository.save(b);
        final String emailTo = resolveEmail(b.getUserId(), b.getEmail());
        final RoomBooking bForMail = b;
        if (emailTo != null && !emailTo.isBlank()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { sendToken(bForMail, "Submitted"); } catch (Exception ignored) {}
            });
        }
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Booking created. Booking ID: " + b.getBookingId();
        if (emailTo != null && !emailTo.isBlank()) msg += " Token PDF will be emailed to " + emailTo + " shortly.";
        else msg += " Token PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        res.put("message", msg);
        res.put("emailSent", false);
        res.put("booking", b);
        res.put("amount", total);
        return ResponseEntity.status(201).body(res);
    }

    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> payment(@RequestBody Map<String, String> body) {
        RoomBooking b = bookingRepository.findByBookingId(body.get("booking_id")).orElse(null);
        if (b == null) b = bookingRepository.findById(body.get("booking_id")).orElse(null);
        if (b == null) return ApiUtil.fail(404, "Booking not found");
        b.setPaymentMethod(body.get("payment_method"));
        b.setPaymentStatus("Paid");
        b.setBookingStatus("Confirmed");
        b.setPaymentDate(Instant.now());
        bookingRepository.save(b);
        notify(b.getUserId(), "Booking Confirmed", "Your room booking (" + b.getBookingId() + ") is confirmed.", "booking");
        boolean emailSent = sendToken(b, "Confirmed");
        String emailTo = resolveEmail(b.getUserId(), b.getEmail());
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Payment successful, booking confirmed";
        if (emailSent) msg += ". Token PDF sent to " + emailTo + ".";
        else msg += ". Token PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        res.put("success", true);
        res.put("message", msg);
        res.put("emailSent", emailSent);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable String id, Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        RoomBooking b = bookingRepository.findByIdAndUserId(id, p.getId()).orElse(null);
        if (b == null) return ApiUtil.fail(404, "Booking not found");
        b.setBookingStatus("Cancelled");
        bookingRepository.save(b);
        return ApiUtil.ok("Booking cancelled");
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable String id) {
        RoomBooking b = bookingRepository.findById(id).orElse(null);
        if (b == null) return ApiUtil.fail(404, "Booking not found");
        b.setBookingStatus("Confirmed");
        if (b.getPaymentStatus() == null || b.getPaymentStatus().isBlank() || "pending".equalsIgnoreCase(b.getPaymentStatus())) {
            b.setPaymentStatus("Paid");
            b.setPaymentDate(Instant.now());
        }
        bookingRepository.save(b);
        notify(b.getUserId(), "Booking Confirmed", "Your room booking (" + b.getBookingId() + ") is confirmed.", "booking");
        boolean emailSent = sendToken(b, "Confirmed");
        String emailTo = resolveEmail(b.getUserId(), b.getEmail());
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Booking confirmed";
        if (emailSent) msg += ". Token PDF sent to " + emailTo + ".";
        else msg += ". Token PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        res.put("success", true);
        res.put("message", msg);
        res.put("emailSent", emailSent);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/cancel-admin")
    public ResponseEntity<Map<String, Object>> cancelAdmin(@PathVariable String id) {
        RoomBooking b = bookingRepository.findById(id).orElse(null);
        if (b == null) return ApiUtil.fail(404, "Booking not found");
        b.setBookingStatus("Cancelled");
        bookingRepository.save(b);
        return ApiUtil.ok("Booking cancelled");
    }

    private void notify(String userId, String title, String message, String type) {
        try { if (userId == null) return; Notification n = new Notification(); n.setUserId(userId); n.setTitle(title); n.setMessage(message); n.setType(type); notificationRepository.save(n);} catch (Exception ignored) {}
    }
    private String s(Object o) { return o == null ? "" : String.valueOf(o); }
    private Integer parseInt(Object o) { try { return o == null ? null : Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; } }
}
