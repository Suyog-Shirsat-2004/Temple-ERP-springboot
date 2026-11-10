package com.temple.erp.service;

import com.temple.erp.model.MailSetting;
import com.temple.erp.repository.MailSettingRepository;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final MailSettingRepository mailSettingRepository;

    @Value("${app.mail.enabled:false}")
    private boolean propEnabled;

    @Value("${app.mail.from:}")
    private String propFrom;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String propHost;

    @Value("${spring.mail.port:587}")
    private int propPort;

    @Value("${spring.mail.username:}")
    private String propUsername;

    @Value("${spring.mail.password:}")
    private String propPassword;

    public EmailService(MailSettingRepository mailSettingRepository) {
        this.mailSettingRepository = mailSettingRepository;
    }

    private static class Resolved {
        boolean enabled;
        String from;
        String host;
        int port;
        String username;
        String password;
        String source;
    }

    private Resolved resolve() {
        Resolved r = new Resolved();
        r.source = "properties";
        try {
            MailSetting db = mailSettingRepository.findById("mail").orElse(null);
            if (db != null && (db.isEnabled() || hasText(db.getUsername()) || hasText(db.getFromEmail()) || hasText(db.getPassword()))) {
                r.source = "admin-settings";
                r.enabled = db.isEnabled();
                r.host = hasText(db.getHost()) ? db.getHost().trim() : propHost;
                r.port = (db.getPort() != null && db.getPort() > 0) ? db.getPort() : propPort;
                r.username = trim(db.getUsername());
                r.password = db.getPassword() == null ? "" : db.getPassword(); // may include spaces? Gmail app passwords are usually shown spaced; we'll trim
                if (r.password != null) r.password = r.password.replace(" ", "").trim();
                r.from = hasText(db.getFromEmail()) ? db.getFromEmail().trim() : (hasText(r.username) ? r.username : trim(propFrom));
                // if DB record exists but fields empty, fall back selectively
                if (!hasText(r.username)) r.username = trim(propUsername);
                if (!hasText(r.password)) r.password = propPassword == null ? "" : propPassword.replace(" ", "").trim();
                if (!hasText(r.from)) r.from = hasText(r.username) ? r.username : trim(propFrom);
                return r;
            }
        } catch (Exception e) {
            log.debug("Could not load mail settings from DB, using properties: {}", e.getMessage());
        }
        r.enabled = propEnabled;
        r.host = hasText(propHost) ? propHost.trim() : "smtp.gmail.com";
        r.port = propPort > 0 ? propPort : 587;
        r.username = trim(propUsername);
        r.password = propPassword == null ? "" : propPassword.replace(" ", "").trim();
        r.from = hasText(propFrom) ? propFrom.trim() : r.username;
        return r;
    }

    public boolean isEnabled() { return resolve().enabled; }

    public boolean isConfigured() {
        Resolved r = resolve();
        return r.enabled && hasText(r.from) && hasText(r.username) && hasText(r.password) && hasText(r.host);
    }

    public String statusHint(String toEmail) {
        if (!hasText(toEmail)) return "no user/profile email found";
        Resolved r = resolve();
        if (!r.enabled) return "email disabled (set in Admin > Email Settings: enable and save Gmail/App Password)";
        if (!hasText(r.username) || !hasText(r.password)) return "Gmail SMTP not configured (Admin > Email Settings: set Gmail and App Password)";
        if (!hasText(r.from)) return "sender email not configured (Admin > Email Settings)";
        return "email send failed (check Gmail/App Password, 2-Step Verification, and server logs)";
    }

    public MailSetting getSettingsForAdmin() {
        try {
            return mailSettingRepository.findById("mail").orElseGet(MailSetting::new);
        } catch (Exception e) {
            return new MailSetting();
        }
    }

    public void saveSettings(MailSetting incoming, String adminName) {
        MailSetting existing = mailSettingRepository.findById("mail").orElseGet(MailSetting::new);
        if (existing.getId() == null || existing.getId().isBlank()) existing.setId("mail");
        existing.setEnabled(incoming.isEnabled());
        existing.setHost(hasText(incoming.getHost()) ? incoming.getHost().trim() : "smtp.gmail.com");
        existing.setPort(incoming.getPort() != null && incoming.getPort() > 0 ? incoming.getPort() : 587);
        existing.setUsername(trim(incoming.getUsername()));
        existing.setFromEmail(hasText(incoming.getFromEmail()) ? incoming.getFromEmail().trim() : existing.getUsername());
        // keep existing password if incoming is blank (so admin doesn't have to retype)
        if (hasText(incoming.getPassword())) {
            existing.setPassword(incoming.getPassword().replace(" ", "").trim());
        }
        existing.setUpdatedBy(adminName);
        existing.setUpdatedAt(java.time.Instant.now());
        mailSettingRepository.save(existing);
    }

    public boolean sendPdf(String toEmail, String subject, String bodyText, byte[] pdfBytes, String filename) {
        if (!hasText(toEmail)) {
            log.info("Email skipped: no recipient email provided (subject={})", subject);
            return false;
        }
        Resolved r = resolve();
        if (!r.enabled) {
            log.info("Email skipped (disabled, source={}). To={} Subject={}", r.source, toEmail, subject);
            return false;
        }
        if (!hasText(r.username) || !hasText(r.password)) {
            log.warn("Email skipped: SMTP credentials not configured (source={}). To={} Subject={}", r.source, toEmail, subject);
            return false;
        }
        String fromAddr = hasText(r.from) ? r.from : r.username;
        try {
            JavaMailSender mailSender = buildSender(r);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddr);
            helper.setTo(toEmail.trim());
            helper.setSubject(subject != null ? subject : "Temple ERP Document");
            helper.setText(bodyText != null ? bodyText : "Please find the attached document.", false);
            if (pdfBytes != null && pdfBytes.length > 0) {
                String name = (filename != null && !filename.isBlank()) ? filename : "document.pdf";
                helper.addAttachment(name, new ByteArrayResource(pdfBytes));
            }
            mailSender.send(message);
            log.info("Email sent (source={}) to {} subject='{}' attachment='{}'", r.source, toEmail, subject, filename);
            return true;
        } catch (Exception e) {
            log.warn("Email send failed (source={}) to {} subject='{}': {}", r.source, toEmail, subject, e.getMessage());
            return false;
        }
    }

    public String testSend(String toEmail) {
        Resolved r = resolve();
        if (!hasText(toEmail)) return "Enter a valid recipient/test email address.";
        if (!r.enabled) return "Email is disabled. Enable it and save settings first.";
        if (!hasText(r.username) || !hasText(r.password)) return "Gmail/App Password not configured. Save settings first.";
        boolean ok = sendPdf(toEmail, "Temple ERP - Test Email",
                "This is a test email from Temple ERP admin email settings. If you received this, Gmail SMTP is configured correctly.",
                null, null);
        return ok ? "Test email sent successfully to " + toEmail.trim() + ". Check Inbox/Spam."
                  : "Test email failed. Verify Gmail address, 16-character App Password (not normal password), 2-Step Verification, and server logs.";
    }

    private JavaMailSender buildSender(Resolved r) {
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(r.host);
        s.setPort(r.port);
        s.setUsername(r.username);
        s.setPassword(r.password);
        Properties p = s.getJavaMailProperties();
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.starttls.required", "true");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "10000");
        p.put("mail.smtp.writetimeout", "10000");
        return s;
    }

    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
}
