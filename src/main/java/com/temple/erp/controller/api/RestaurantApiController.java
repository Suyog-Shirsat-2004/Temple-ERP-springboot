package com.temple.erp.controller.api;

import com.temple.erp.model.RestaurantMenuItem;
import com.temple.erp.model.RestaurantOrder;
import com.temple.erp.repository.RestaurantMenuRepository;
import com.temple.erp.repository.RestaurantOrderRepository;
import com.temple.erp.repository.UserRepository;
import com.temple.erp.security.AuthPrincipal;
import com.temple.erp.service.EmailService;
import com.temple.erp.service.PdfService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurant")
public class RestaurantApiController {
    private final RestaurantMenuRepository menuRepository;
    private final RestaurantOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PdfService pdfService;

    public RestaurantApiController(RestaurantMenuRepository menuRepository, RestaurantOrderRepository orderRepository,
                                   UserRepository userRepository, EmailService emailService, PdfService pdfService) {
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.pdfService = pdfService;
    }

    private String resolveEmail(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try { return userRepository.findById(userId).map(u -> u.getEmail()).filter(x -> x != null && !x.isBlank()).map(String::trim).orElse(null); } catch (Exception e) { return null; }
    }
    private String resolveName(String userId, String fallback) {
        if (userId != null && !userId.isBlank()) {
            try { String n = userRepository.findById(userId).map(u -> u.getName()).filter(x -> x != null && !x.isBlank()).map(String::trim).orElse(null); if (n != null) return n; } catch (Exception ignored) {}
        }
        return fallback != null && !fallback.isBlank() ? fallback.trim() : null;
    }
    private boolean sendToken(RestaurantOrder o, String statusLabel) {
        try {
            String to = resolveEmail(o.getUserId());
            if (to == null || to.isBlank()) return false;
            String name = resolveName(o.getUserId(), o.getName());
            byte[] pdf = pdfService.generateRestaurantTokenPdf(o, name);
            String tokenId = o.getOrderId() != null ? o.getOrderId() : o.getId();
            return emailService.sendPdf(to,
                    "Temple ERP - Restaurant Order Token " + tokenId + " (" + statusLabel + ")",
                    "Dear " + (name != null ? name : "Devotee") + ",\n\nYour restaurant order " + tokenId + " status: " + statusLabel + ". The order token PDF is attached.\n\n- Temple ERP",
                    pdf, "restaurant-token-" + tokenId + ".pdf");
        } catch (Exception e) { return false; }
    }

    @GetMapping("/menu")
    public ResponseEntity<Map<String, Object>> menu() {
        List<RestaurantMenuItem> items = menuRepository.findByAvailableTrue();
        List<String> categories = items.stream().map(RestaurantMenuItem::getCategory).filter(c -> c != null).distinct().collect(Collectors.toList());
        Map<String, Object> res = ApiUtil.body(true);
        res.put("menu_items", items);
        res.put("categories", categories);
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> userOrders(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("orders", orderRepository.findByUserIdOrderByIdDesc(p.getId()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> allOrders() {
        Map<String, Object> res = ApiUtil.body(true);
        res.put("orders", orderRepository.findAllByOrderByIdDesc());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        RestaurantOrder o = orderRepository.findByOrderId(orderId).orElse(null);
        if (o == null) o = orderRepository.findById(orderId).orElse(null);
        if (o == null) return ApiUtil.fail(404, "Order not found");
        Map<String, Object> res = ApiUtil.body(true);
        res.put("order", o);
        res.put("items", o.getItems());
        return ResponseEntity.ok(res);
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body, Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) body.get("items");
        List<RestaurantOrder.OrderItem> items = new ArrayList<>();
        double total = 0;
        if (itemsRaw != null) {
            for (Map<String, Object> it : itemsRaw) {
                RestaurantOrder.OrderItem oi = new RestaurantOrder.OrderItem();
                oi.setName(String.valueOf(it.get("name")));
                double price = it.get("price") instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(it.get("price")));
                int qty = it.get("qty") instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(it.get("qty")));
                oi.setPrice(price); oi.setQty(qty);
                items.add(oi);
                total += price * qty;
            }
        }
        RestaurantOrder o = new RestaurantOrder();
        o.setOrderId("ORD" + System.currentTimeMillis() + (int) (Math.floor(Math.random() * 900) + 100));
        o.setUserId(p.getId());
        o.setItems(items);
        o.setTotalAmount(total);
        o.setName(body.get("name") != null ? String.valueOf(body.get("name")) : p.getName());
        o.setPhone(body.get("phone") != null ? String.valueOf(body.get("phone")) : null);
        o.setOrderDate(LocalDate.now());
        o.setOrderTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        o.setPaymentMethod(body.get("payment_method") != null ? String.valueOf(body.get("payment_method")) : "UPI");
        o.setPaymentStatus("paid");
        o.setStatus("confirmed");
        orderRepository.save(o);
        final String emailTo = resolveEmail(o.getUserId());
        final RestaurantOrder oForMail = o;
        if (emailTo != null && !emailTo.isBlank()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { sendToken(oForMail, "confirmed"); } catch (Exception ignored) {}
            });
        }
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Order placed. Order ID: " + o.getOrderId();
        if (emailTo != null && !emailTo.isBlank()) msg += " Token PDF will be emailed to " + emailTo + " shortly.";
        else msg += " Token PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        res.put("message", msg);
        res.put("emailSent", false);
        res.put("order", o);
        return ResponseEntity.status(201).body(res);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        RestaurantOrder o = orderRepository.findById(id).orElse(null);
        if (o == null) return ApiUtil.fail(404, "Order not found");
        String status = body.get("status");
        o.setStatus(status);
        boolean approving = status != null && ("approved".equalsIgnoreCase(status) || "confirmed".equalsIgnoreCase(status));
        if (approving && (o.getPaymentStatus() == null || o.getPaymentStatus().isBlank() || "pending".equalsIgnoreCase(o.getPaymentStatus()))) {
            o.setPaymentStatus("paid");
            o.setPaymentDate(java.time.Instant.now());
        }
        orderRepository.save(o);
        boolean emailSent = false;
        if (approving) emailSent = sendToken(o, status);
        String emailTo = resolveEmail(o.getUserId());
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Order status updated";
        if (approving) {
            if (emailSent) msg += ". Token PDF sent to " + emailTo + ".";
            else msg += ". Token PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        }
        res.put("success", true);
        res.put("message", msg);
        res.put("emailSent", emailSent);
        return ResponseEntity.ok(res);
    }
}
