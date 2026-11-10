package com.temple.erp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.temple.erp.model.DarshanPass;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QrCodeService {

    private final Path root;
    private final String configuredBaseUrl;

    public QrCodeService(@Value("${app.upload.dir:${UPLOAD_DIR:uploads}}") String uploadDir,
                         @Value("${app.base-url:}") String configuredBaseUrl) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.configuredBaseUrl = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
        Files.createDirectories(root.resolve("passes").resolve("qr"));
    }

    /**
     * Ensures the pass has a permanent, unique QR code (image + token) linked to its passId.
     * Idempotent: if qrCode and qrToken already exist and the image file is present, does nothing.
     * Call this on admin approval (and any equivalent approval transition) before saving.
     */
    public void ensurePassQr(DarshanPass pass, String requestBaseUrl) {
        if (pass == null) return;
        String passId = pass.getPassId() != null && !pass.getPassId().isBlank() ? pass.getPassId() : pass.getId();
        if (!StringUtils.hasText(passId)) return;

        String existingToken = pass.getQrToken();
        String existingCode = pass.getQrCode();
        if (StringUtils.hasText(existingToken) && StringUtils.hasText(existingCode) && Files.exists(qrPath(existingCode))) {
            return; // permanent QR already present
        }

        // Generate or reuse token (permanent). Token uniquely identifies this pass verification.
        String token = StringUtils.hasText(existingToken) ? existingToken : generateToken(passId);
        String filename = StringUtils.hasText(existingCode) ? existingCode : ("qr_" + sanitize(passId) + "_" + System.currentTimeMillis() + ".png");

        String payload = buildVerificationUrl(token, requestBaseUrl);
        try {
            byte[] png = generateQrPng(payload, 320);
            Files.write(qrPath(filename), png);
            pass.setQrToken(token);
            pass.setQrCode(filename);
        } catch (Exception e) {
            // Fallback: try once more with a fresh filename if write failed
            try {
                String filename2 = "qr_" + sanitize(passId) + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ".png";
                byte[] png = generateQrPng(payload, 320);
                Files.write(qrPath(filename2), png);
                pass.setQrToken(token);
                pass.setQrCode(filename2);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to generate/store QR code for pass " + passId, ex);
            }
        }
    }

    public String buildVerificationUrl(String token, String requestBaseUrl) {
        String base = StringUtils.hasText(configuredBaseUrl) ? configuredBaseUrl : (requestBaseUrl == null ? "" : requestBaseUrl.trim());
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (StringUtils.hasText(base)) {
            return base + "/passes/verify/" + token;
        }
        // If base URL unknown, encode a resolvable path; scanners will see text, but app stores token for verification.
        // Prefer absolute URL when possible (request/config). This is a safe fallback.
        return "/passes/verify/" + token;
    }

    public byte[] generateQrPng(String content, int size) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content == null ? "" : content, BarcodeFormat.QR_CODE, size, size, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    public Path qrPath(String filename) {
        return root.resolve("passes").resolve("qr").resolve(filename == null ? "" : filename);
    }

    public String toDataUri(String filename) {
        if (!StringUtils.hasText(filename)) return null;
        try {
            Path p = qrPath(filename);
            if (!Files.exists(p)) return null;
            byte[] bytes = Files.readAllBytes(p);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    public String qrWebPath(String filename) {
        if (!StringUtils.hasText(filename)) return null;
        return "/uploads/passes/qr/" + filename;
    }

    private String generateToken(String passId) {
        String rand = UUID.randomUUID().toString().replace("-", "") + Long.toUnsignedString(System.nanoTime(), 36);
        String safePass = sanitize(passId);
        return safePass + "-" + rand;
    }

    private String sanitize(String s) {
        if (s == null) return "pass";
        return s.replaceAll("[^A-Za-z0-9_-]", "");
    }
}