package com.temple.erp.controller.api;

import com.temple.erp.model.DarshanPass;
import com.temple.erp.model.Notification;
import com.temple.erp.model.PassType;
import com.temple.erp.repository.DarshanPassRepository;
import com.temple.erp.repository.NotificationRepository;
import com.temple.erp.repository.PassTypeRepository;
import com.temple.erp.repository.UserRepository;
import com.temple.erp.security.AuthPrincipal;
import com.temple.erp.service.EmailService;
import com.temple.erp.service.FileStorageService;
import com.temple.erp.service.PdfService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/passes")
public class PassApiController {
    private final DarshanPassRepository passRepository;
    private final PassTypeRepository passTypeRepository;
    private final NotificationRepository notificationRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PdfService pdfService;

    public PassApiController(DarshanPassRepository passRepository, PassTypeRepository passTypeRepository,
                             NotificationRepository notificationRepository, FileStorageService fileStorageService,
                             UserRepository userRepository, EmailService emailService, PdfService pdfService) {
        this.passRepository = passRepository;
        this.passTypeRepository = passTypeRepository;
        this.notificationRepository = notificationRepository;
        this.fileStorageService = fileStorageService;
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
    private boolean sendPassPdf(DarshanPass pass, String statusLabel) {
        try {
            String to = resolveEmail(pass.getUserId(), pass.getEmail());
            if (to == null || to.isBlank()) return false;
            String name = resolveName(pass.getUserId(), pass.getDevoteeName());
            String profilePic = null;
            try { if (pass.getUserId() != null) profilePic = userRepository.findById(pass.getUserId()).map(u -> u.getProfilePic()).orElse(null); } catch (Exception ignored) {}
            byte[] pdf = pdfService.generatePassPdf(pass, name, profilePic);
            String passId = pass.getPassId() != null ? pass.getPassId() : pass.getId();
            return emailService.sendPdf(to,
                    "Temple ERP - Darshan Pass " + passId + " (" + statusLabel + ")",
                    "Dear " + (name != null ? name : "Devotee") + ",\n\nYour Darshan Pass " + passId + " status: " + statusLabel + ". The pass PDF is attached.\n\n- Temple ERP",
                    pdf, "Temple Darshan Pass - original pass.pdf");
        } catch (Exception e) { return false; }
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getPassTypes() {
        Map<String, Object> res = ApiUtil.body(true);
        res.put("passTypes", passTypeRepository.findByIsActiveTrueOrderByPriceAsc());
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserPasses(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("passes", passRepository.findByUserIdOrderByIdDesc(p.getId()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllPasses() {
        Map<String, Object> res = ApiUtil.body(true);
        res.put("passes", passRepository.findAllByOrderByIdDesc());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPassById(@PathVariable String id) {
        Optional<DarshanPass> passOpt = passRepository.findByPassId(id);
        if (passOpt.isEmpty()) passOpt = passRepository.findById(id);
        if (passOpt.isEmpty()) return ApiUtil.fail(404, "Pass not found");
        Map<String, Object> res = ApiUtil.body(true);
        res.put("pass", passOpt.get());
        return ResponseEntity.ok(res);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createPass(Authentication auth,
            @RequestParam(required = false) String pass_type,
            @RequestParam(required = false) String payment_method,
            @RequestParam(required = false) String devotee_name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String visit_date,
            @RequestParam(required = false) String visit_time,
            @RequestParam(required = false) String no_of_persons,
            @RequestParam(required = false) String transaction_id,
            @RequestParam(required = false) String aadhar_number,
            @RequestParam(required = false, name = "aadhar_card") MultipartFile aadharCard) throws Exception {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        String typeKey = pass_type == null ? "" : pass_type;
        Optional<PassType> ptOpt = passTypeRepository.findActiveByPassTypeRegex("^" + java.util.regex.Pattern.quote(typeKey) + "$");
        if (ptOpt.isEmpty()) ptOpt = passTypeRepository.findActiveByPassTypeRegex(java.util.regex.Pattern.quote(typeKey.replace("_", " ")));
        double amount = ptOpt.map(t -> t.getPrice() == null ? 0.0 : t.getPrice()).orElseGet(() -> fallbackPrice(typeKey));
        String passTypeName = ptOpt.map(PassType::getPassType).orElse(titleize(typeKey));
        int persons = 1;
        try { if (no_of_persons != null) persons = Integer.parseInt(no_of_persons); } catch (Exception ignored) {}
        double total = amount * persons;
        String passId = "DSP" + System.currentTimeMillis() + (int) (Math.floor(Math.random() * 900) + 100);
        String paymentStatus = "upi".equalsIgnoreCase(payment_method) ? "paid" : "pending";

        DarshanPass pass = new DarshanPass();
        pass.setPassId(passId);
        pass.setUserId(p.getId());
        pass.setDevoteeName(devotee_name);
        pass.setPhone(phone);
        pass.setEmail(email);
        pass.setGender(gender);
        pass.setAddress(address);
        if (visit_date != null && !visit_date.isBlank()) {
            try { pass.setVisitDate(LocalDate.parse(visit_date.substring(0, Math.min(10, visit_date.length())))); } catch (Exception ignored) {}
        }
        pass.setVisitTime(visit_time);
        pass.setNoOfPersons(persons);
        pass.setPassType(passTypeName);
        pass.setAmount(amount);
        pass.setTotalAmount(total);
        pass.setPaymentMethod(payment_method);
        pass.setTransactionId(transaction_id == null ? "" : transaction_id);
        pass.setAadharNumber(aadhar_number == null ? "" : aadhar_number);
        if (aadharCard != null && !aadharCard.isEmpty()) pass.setAadharCard(fileStorageService.store(aadharCard, "passes", "aadhar_card"));
        pass.setStatus("pending");
        pass.setPaymentStatus(paymentStatus);
        passRepository.save(pass);
        final String emailToApi = resolveEmail(pass.getUserId(), pass.getEmail());
        final DarshanPass passForMailApi = pass;
        if (emailToApi != null && !emailToApi.isBlank()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    sendPassPdf(passForMailApi, "Submitted");
                } catch (Exception ignored) {}
            });
        }
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Darshan Pass request submitted! Pass ID: " + passId;
        if (emailToApi != null && !emailToApi.isBlank()) msg += " Pass PDF will be emailed to " + emailToApi + " shortly.";
        else msg += " Pass PDF email not sent (" + emailService.statusHint(emailToApi) + ").";
        res.put("message", msg);
        res.put("emailSent", false);
        res.put("pass", pass);
        return ResponseEntity.status(201).body(res);
    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<Map<String, Object>> confirmPayment(@RequestBody Map<String, String> body) {
        DarshanPass pass = passRepository.findByPassId(body.get("pass_id")).orElse(null);
        if (pass == null) return ApiUtil.fail(404, "Pass not found");
        if ("paid".equalsIgnoreCase(pass.getPaymentStatus())) {
            Map<String, Object> res = ApiUtil.body(true);
            res.put("already_paid", true);
            res.put("message", "Payment already confirmed");
            return ResponseEntity.ok(res);
        }
        pass.setPaymentStatus("paid");
        pass.setPaymentDate(Instant.now());
        passRepository.save(pass);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("message", "Payment Successful");
        res.put("transaction_id", "TXN" + System.currentTimeMillis());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPass(@PathVariable String id, Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        DarshanPass pass = passRepository.findByIdAndUserId(id, p.getId()).orElse(null);
        if (pass == null) return ApiUtil.fail(404, "Pass not found");
        pass.setStatus("cancelled");
        passRepository.save(pass);
        return ApiUtil.ok("Pass cancelled successfully");
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String id) {
        DarshanPass pass = passRepository.findById(id).orElse(null);
        if (pass == null) return ApiUtil.fail(404, "Pass not found");
        pass.setStatus("approved");
        boolean freeByTypeOrMethod = (pass.getPaymentStatus() != null && java.util.Set.of("not_required", "free", "waived", "na", "n/a").contains(pass.getPaymentStatus().toLowerCase()))
                || (pass.getPaymentMethod() != null && (pass.getPaymentMethod().toLowerCase().contains("trust") || pass.getPaymentMethod().toLowerCase().contains("free")));
        if (!freeByTypeOrMethod && pass.getAmount() != null && pass.getNoOfPersons() != null && pass.getNoOfPersons() > 0) {
            double calc = pass.getAmount() * pass.getNoOfPersons();
            if (pass.getTotalAmount() == null || Math.abs(pass.getTotalAmount() - calc) > 0.001) pass.setTotalAmount(calc);
        }
        boolean freeOrNoPayment = freeByTypeOrMethod || (pass.getTotalAmount() != null && pass.getTotalAmount() <= 0.0);
        if (freeOrNoPayment) {
            if (pass.getPaymentStatus() == null || pass.getPaymentStatus().isBlank() || "pending".equalsIgnoreCase(pass.getPaymentStatus())) pass.setPaymentStatus("not_required");
        } else {
            pass.setPaymentStatus("paid");
            pass.setPaymentDate(Instant.now());
        }
        passRepository.save(pass);
        notify(pass.getUserId(), "Pass Approved", "Your Darshan Pass (" + pass.getPassId() + ") has been APPROVED!", "pass");
        boolean emailSent = sendPassPdf(pass, "Approved");
        String emailTo = resolveEmail(pass.getUserId(), pass.getEmail());
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Pass approved successfully";
        if (emailSent) msg += ". Pass PDF sent to " + emailTo + ".";
        else msg += ". Pass PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        res.put("message", msg);
        res.put("emailSent", emailSent);
        res.put("pass", pass);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable String id) {
        DarshanPass pass = passRepository.findById(id).orElse(null);
        if (pass == null) return ApiUtil.fail(404, "Pass not found");
        pass.setStatus("rejected");
        passRepository.save(pass);
        notify(pass.getUserId(), "Pass Rejected", "Your Darshan Pass request has been rejected.", "pass");
        return ApiUtil.ok("Pass rejected");
    }

    @PostMapping("/{id}/payment-paid")
    public ResponseEntity<Map<String, Object>> paymentPaid(@PathVariable String id) {
        DarshanPass pass = passRepository.findById(id).orElse(null);
        if (pass == null) return ApiUtil.fail(404, "Pass not found");
        pass.setPaymentStatus("paid");
        pass.setPaymentDate(Instant.now());
        passRepository.save(pass);
        return ApiUtil.ok("Payment marked as received for pass: " + pass.getPassId());
    }

    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable String id, @PathVariable String status) {
        DarshanPass pass = passRepository.findById(id).orElse(null);
        if (pass == null) return ApiUtil.fail(404, "Pass not found");
        boolean activating = "active".equals(status) || "confirmed".equals(status) || "approved".equalsIgnoreCase(status);
        if (activating) {
            pass.setStatus("approved".equalsIgnoreCase(status) ? "approved" : "active");
            boolean freeByTypeOrMethod = (pass.getPaymentStatus() != null && java.util.Set.of("not_required", "free", "waived", "na", "n/a").contains(pass.getPaymentStatus().toLowerCase()))
                    || (pass.getPaymentMethod() != null && (pass.getPaymentMethod().toLowerCase().contains("trust") || pass.getPaymentMethod().toLowerCase().contains("free")));
            boolean freeOrNoPayment = freeByTypeOrMethod || (pass.getTotalAmount() != null && pass.getTotalAmount() <= 0.0);
            if (!freeOrNoPayment) {
                pass.setPaymentStatus("paid");
                pass.setPaymentDate(Instant.now());
            } else if (pass.getPaymentStatus() == null || pass.getPaymentStatus().isBlank() || "pending".equalsIgnoreCase(pass.getPaymentStatus())) {
                pass.setPaymentStatus("not_required");
            }
        } else {
            pass.setStatus(status);
        }
        passRepository.save(pass);
        boolean emailSent = false;
        if (activating) emailSent = sendPassPdf(pass, pass.getStatus());
        String emailTo = resolveEmail(pass.getUserId(), pass.getEmail());
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Pass status updated";
        if (activating) {
            if (emailSent) msg += ". Pass PDF sent to " + emailTo + ".";
            else msg += ". Pass PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        }
        res.put("success", true);
        res.put("message", msg);
        res.put("emailSent", emailSent);
        res.put("pass", pass);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        if (!passRepository.existsById(id)) return ApiUtil.fail(404, "Pass not found");
        passRepository.deleteById(id);
        return ApiUtil.ok("Pass deleted successfully");
    }

    @DeleteMapping("/multiple")
    public ResponseEntity<Map<String, Object>> deleteMultiple(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) return ApiUtil.fail(400, "No passes selected");
        passRepository.deleteAllById(ids);
        Map<String, Object> res = ApiUtil.body(true);
        res.put("message", ids.size() + " passes deleted");
        return ResponseEntity.ok(res);
    }

    private void notify(String userId, String title, String message, String type) {
        try {
            if (userId == null) return;
            Notification n = new Notification();
            n.setUserId(userId); n.setTitle(title); n.setMessage(message); n.setType(type);
            notificationRepository.save(n);
        } catch (Exception ignored) {}
    }
    private double fallbackPrice(String type) {
        if (type == null) return 0;
        return switch (type.toLowerCase()) { case "general" -> 50; case "special" -> 100; case "vip" -> 500; default -> 0; };
    }
    private String titleize(String s) {
        if (s == null) return "";
        String t = s.replace('_', ' ');
        String[] parts = t.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase()).append(' ');
        return sb.toString().trim();
    }
}
