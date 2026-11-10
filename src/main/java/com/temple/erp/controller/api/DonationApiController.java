package com.temple.erp.controller.api;

import com.temple.erp.model.Donation;
import com.temple.erp.repository.DonationRepository;
import com.temple.erp.repository.UserRepository;
import com.temple.erp.security.AuthPrincipal;
import com.temple.erp.service.EmailService;
import com.temple.erp.service.PdfService;
import java.time.Instant;
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
@RequestMapping("/api/donations")
public class DonationApiController {
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PdfService pdfService;

    public DonationApiController(DonationRepository donationRepository, UserRepository userRepository,
                                 EmailService emailService, PdfService pdfService) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.pdfService = pdfService;
    }

    private String resolveEmail(String userId, String fallback) {
        if (userId != null && !userId.isBlank()) {
            try {
                String e = userRepository.findById(userId).map(u -> u.getEmail()).filter(x -> x != null && !x.isBlank()).map(String::trim).orElse(null);
                if (e != null) return e;
            } catch (Exception ignored) {}
        }
        return fallback != null && !fallback.isBlank() ? fallback.trim() : null;
    }
    private String resolveName(String userId, String fallback) {
        if (userId != null && !userId.isBlank()) {
            try {
                String n = userRepository.findById(userId).map(u -> u.getName()).filter(x -> x != null && !x.isBlank()).map(String::trim).orElse(null);
                if (n != null) return n;
            } catch (Exception ignored) {}
        }
        return fallback != null && !fallback.isBlank() ? fallback.trim() : null;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserDonations(Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        var donations = donationRepository.findByUserIdOrderByIdDesc(p.getId());
        double total = donations.stream().mapToDouble(d -> d.getAmount() == null ? 0 : d.getAmount()).sum();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("donations", donations);
        res.put("total_donated", total);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> res = ApiUtil.body(true);
        res.put("donations", donationRepository.findAllByOrderByIdDesc());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> getTotal() {
        double total = donationRepository.findAll().stream()
                .filter(d -> "completed".equalsIgnoreCase(d.getPaymentStatus()) || "paid".equalsIgnoreCase(d.getPaymentStatus()))
                .mapToDouble(d -> d.getAmount() == null ? 0 : d.getAmount()).sum();
        Map<String, Object> res = ApiUtil.body(true);
        res.put("total", total);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/receipt/{receiptNo}")
    public ResponseEntity<Map<String, Object>> getReceipt(@PathVariable String receiptNo) {
        Donation d = donationRepository.findByReceiptNo(receiptNo).orElse(null);
        if (d == null) return ApiUtil.fail(404, "Receipt not found");
        Map<String, Object> res = ApiUtil.body(true);
        res.put("donation", d);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body, Authentication auth) {
        AuthPrincipal p = (AuthPrincipal) auth.getPrincipal();
        Donation d = new Donation();
        d.setUserId(p.getId());
        d.setReceiptNo("DON" + System.currentTimeMillis() + (int) (Math.floor(Math.random() * 900) + 100));
        d.setName(str(body.get("name"), p.getName()));
        d.setEmail(str(body.get("email"), p.getEmail()));
        d.setPhone(str(body.get("phone"), null));
        Object amt = body.get("amount");
        double amount = amt instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(amt));
        d.setAmount(amount);
        d.setDonationType(str(body.get("donation_type"), "General Donation"));
        d.setPaymentMethod(str(body.get("payment_method"), "UPI"));
        d.setPaymentStatus("completed");
        d.setNotes(str(body.get("message"), str(body.get("notes"), null)));
        d.setDonationDate(Instant.now());
        donationRepository.save(d);
        final String emailTo = resolveEmail(p.getId(), d.getEmail());
        final String nameForMail = resolveName(p.getId(), d.getName());
        final Donation dForMail = d;
        final double amountForMail = amount;
        if (emailTo != null && !emailTo.isBlank()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    byte[] pdf = pdfService.generateDonationReceiptPdf(dForMail, nameForMail);
                    boolean sent = emailService.sendPdf(emailTo,
                            "Temple ERP - Donation Receipt " + dForMail.getReceiptNo(),
                            "Dear " + (nameForMail != null ? nameForMail : "Devotee") + ",\n\nThank you for your donation of Rs. " + amountForMail + " (" + dForMail.getDonationType() + "). Your donation receipt " + dForMail.getReceiptNo() + " is attached.\n\n- Temple ERP",
                            pdf, "donation-receipt-" + dForMail.getReceiptNo() + ".pdf");
                    if (!sent) {
                        org.slf4j.LoggerFactory.getLogger(DonationApiController.class).warn("Donation receipt email not sent for {} to {}: {}", dForMail.getReceiptNo(), emailTo, emailService.statusHint(emailTo));
                    }
                } catch (Exception ex) {
                    org.slf4j.LoggerFactory.getLogger(DonationApiController.class).warn("Background donation email failed for {}: {}", dForMail.getReceiptNo(), ex.getMessage());
                }
            });
        }
        Map<String, Object> res = ApiUtil.body(true);
        String msg = "Donation recorded. Receipt: " + d.getReceiptNo();
        if (emailTo != null && !emailTo.isBlank()) msg += " Receipt PDF will be emailed to " + emailTo + " shortly.";
        else msg += " Receipt PDF email not sent (" + emailService.statusHint(emailTo) + ").";
        res.put("message", msg);
        res.put("emailSent", false);
        res.put("donation", d);
        return ResponseEntity.status(201).body(res);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable String id) {
        Donation d = donationRepository.findById(id).orElse(null);
        if (d == null) return ApiUtil.fail(404, "Donation not found");
        d.setPaymentStatus("completed");
        donationRepository.save(d);
        return ApiUtil.ok("Donation marked complete");
    }

    private String str(Object v, String def) { return v == null ? def : String.valueOf(v); }
}
