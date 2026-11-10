package com.temple.erp.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import com.temple.erp.model.DarshanPass;
import com.temple.erp.model.Donation;
import com.temple.erp.model.RestaurantOrder;
import com.temple.erp.model.RoomBooking;
import com.temple.erp.model.User;
import com.temple.erp.repository.UserRepository;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final SpringTemplateEngine templateEngine;
    private final FileStorageService fileStorageService;
    private final QrCodeService qrCodeService;
    private final UserRepository userRepository;
    private final String uploadDir;

    public PdfService(SpringTemplateEngine templateEngine,
                      FileStorageService fileStorageService,
                      QrCodeService qrCodeService,
                      UserRepository userRepository,
                      @Value("${app.upload.dir:${UPLOAD_DIR:uploads}}") String uploadDir) {
        this.templateEngine = templateEngine;
        this.fileStorageService = fileStorageService;
        this.qrCodeService = qrCodeService;
        this.userRepository = userRepository;
        this.uploadDir = uploadDir;
    }

    // Temple theme colors (matched to pass-detail.html, extra vibrant for PDF)
    private static final Color MAROON = new Color(122, 31, 31);
    private static final Color MAROON_DARK = new Color(74, 14, 14);
    private static final Color SAFFRON = new Color(255, 140, 0);
    private static final Color SAFFRON_LIGHT = new Color(255, 186, 73);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color GOLD_BRIGHT = new Color(255, 215, 0);
    private static final Color GOLD_DARK = new Color(161, 98, 7);
    private static final Color CREAM = new Color(255, 253, 248);
    private static final Color CREAM_WARM = new Color(255, 246, 220);
    private static final Color CREAM_BORDER = new Color(234, 217, 176);
    private static final Color TEXT_DARK = new Color(44, 36, 22);
    private static final Color LABEL = new Color(138, 106, 36);
    private static final Color GREEN_BG = new Color(220, 252, 231);
    private static final Color GREEN_BORDER = new Color(74, 222, 128);
    private static final Color GREEN_TEXT = new Color(21, 128, 61);
    private static final Color GREEN_DARK = new Color(20, 83, 45);
    private static final Color YELLOW_BG = new Color(254, 249, 195);
    private static final Color YELLOW_BORDER = new Color(250, 204, 21);
    private static final Color YELLOW_TEXT = new Color(161, 98, 7);
    private static final Color RED_BG = new Color(254, 226, 226);
    private static final Color RED_BORDER = new Color(248, 113, 113);
    private static final Color RED_TEXT = new Color(185, 28, 28);

    public byte[] generatePassPdf(DarshanPass pass, String recipientName) {
        return generatePassPdf(pass, recipientName, null);
    }

    /**
     * Generates the pass PDF from the single-source HTML template (same as approved Admin/User web view)
     * for both download and Gmail send. On failure, logs the full root cause and fails the operation
     * (no silent fallback to a differently designed PDF).
     */
    public byte[] generatePassPdf(DarshanPass pass, String recipientName, String profilePicFilename) {
        String ref = pass != null ? (pass.getPassId() != null ? pass.getPassId() : pass.getId()) : "null";
        try {
            return generatePassPdfHtml(pass, recipientName, profilePicFilename, ref);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTML PDF render failed [template=pdf/pass-render, type=DarshanPass, ref={}]", ref, e);
            throw new RuntimeException("Failed to generate Darshan Pass PDF (HTML render). Ref=" + ref, e);
        }
    }

    private byte[] generatePassPdfHtml(DarshanPass pass, String recipientName, String profilePicFilename, String ref) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("pdfMode", true);
        ctx.setVariable("pass", pass);
        ctx.setVariable("passOwner", null);
        ctx.setVariable("displayName", recipientName);
        // Provide profile photo and QR as data URIs for reliable PDF embedding (same visual content as web)
        String photoDataUri = resolveImageDataUri(resolveProfileImage(profilePicFilename));
        ctx.setVariable("passOwnerProfilePic", photoDataUri);
        String logoDataUri = resolveClasspathImageDataUri(
                "images/temple/icon symbol.png",
                "images/temple/icon symbol.webp",
                "images/temple/icon-symbol.png",
                "images/temple/icon-symbol.webp",
                "static/images/temple/icon-symbol.png",
                "static/images/temple/icon-symbol.webp",
                "static/images/temple/icon symbol.webp",
                "static/favicon.svg",
                "static/images/temple/logo.png");
        if (logoDataUri == null) {
            logoDataUri = toPngDataUriIfPossible(resolveClasspathImageBytes(
                    "static/images/temple/icon-symbol.webp",
                    "static/images/temple/icon symbol.webp"));
        }
        ctx.setVariable("passLogoSrc", logoDataUri);
        String qrDataUri = (pass != null && pass.getQrCode() != null) ? qrCodeService.toDataUri(pass.getQrCode()) : null;
        ctx.setVariable("passQrSrc", qrDataUri);
        ctx.setVariable("passQrImageUrl", qrDataUri);

        return renderHtmlToPdf("pdf/pass-render", "DarshanPass", ref, ctx);
    }

    /**
     * Centralized HTML-to-PDF renderer (openhtmltopdf). Fails fast with full diagnostics on error.
     */
    private byte[] renderHtmlToPdf(String templateName, String docType, String refId, Context ctx) {
        String html;
        try {
            html = templateEngine.process(templateName, ctx);
        } catch (Exception e) {
            log.error("HTML PDF template processing failed [template={}, type={}, ref={}]", templateName, docType, refId, e);
            throw new RuntimeException("PDF template processing failed (" + templateName + ", ref=" + refId + ")", e);
        }
        if (html == null) {
            log.error("HTML PDF render produced null output [template={}, type={}, ref={}]", templateName, docType, refId);
            throw new RuntimeException("PDF template produced null output (" + templateName + ", ref=" + refId + ")");
        }
        // Defensive: strip UTF-8 BOM and leading characters before prolog for XML parser safety
        if (!html.isEmpty() && html.charAt(0) == '\uFEFF') {
            html = html.substring(1);
        }
        html = html.stripLeading();
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerUnicodeFonts(builder);
            String baseUri = Paths.get(".").toAbsolutePath().toUri().toString();
            builder.withHtmlContent(html, baseUri);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            String prolog = html.length() > 120 ? html.substring(0, 120) : html;
            log.error("HTML PDF render failed [template={}, type={}, ref={}, prologPreview={}]", templateName, docType, refId, prolog.replace("\n", "\\n").replace("\r", "\\r"), e);
            throw new RuntimeException("PDF HTML render failed (" + templateName + ", ref=" + refId + ")", e);
        }
    }

    /**
     * Registers Unicode-capable fonts (including Devanagari for ॐ) so symbols do not render as '#'.
     * Tries bundled/classpath fonts first, then common OS font locations. Best-effort: missing fonts are skipped.
     */
    private void registerUnicodeFonts(PdfRendererBuilder builder) {
        // Register Unicode/Devanagari-capable fonts so symbols like ॐ render instead of '#'.
        // Use common family aliases so CSS font-family matches even when renderer resolves by name.
        registerFontIfExists(builder, "fonts/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari", "Nirmala UI", "Mangal");
        registerFontIfExists(builder, "static/fonts/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari", "Nirmala UI", "Mangal");
        registerFontIfExists(builder, "fonts/NotoSans-Regular.ttf", "Noto Sans");
        registerFontIfExists(builder, "static/fonts/NotoSans-Regular.ttf", "Noto Sans");
        registerFontIfExists(builder, "C:/Windows/Fonts/NIRMALA.TTF", "Nirmala UI", "Noto Sans Devanagari", "Mangal");
        registerFontIfExists(builder, "C:/Windows/Fonts/Nirmala.ttf", "Nirmala UI", "Noto Sans Devanagari", "Mangal");
        registerFontIfExists(builder, "C:/Windows/Fonts/mangal.ttf", "Mangal", "Nirmala UI", "Noto Sans Devanagari");
        registerFontIfExists(builder, "C:/Windows/Fonts/seguisym.ttf", "Segoe UI Symbol");
        registerFontIfExists(builder, "C:/Windows/Fonts/arial.ttf", "Arial");
        registerFontIfExists(builder, "/usr/share/fonts/truetype/noto/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari", "Nirmala UI", "Mangal");
        registerFontIfExists(builder, "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf", "Noto Sans");
        registerFontIfExists(builder, "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "DejaVu Sans");
    }

    private void registerFontIfExists(PdfRendererBuilder builder, String path, String... families) {
        try {
            File f = new File(path);
            if (!f.exists() || !f.isFile()) {
                if (!f.isAbsolute()) {
                    Path p1 = Paths.get("src/main/resources", path);
                    Path p2 = Paths.get("target/classes", path);
                    if (Files.exists(p1)) f = p1.toFile();
                    else if (Files.exists(p2)) f = p2.toFile();
                    else return;
                } else {
                    return;
                }
            }
            if (families == null || families.length == 0) {
                builder.useFont(f, f.getName());
                return;
            }
            for (String fam : families) {
                if (fam != null && !fam.isBlank()) {
                    builder.useFont(f, fam);
                }
            }
        } catch (Exception e) {
            log.debug("Unicode font registration skipped for {}: {}", path, e.toString());
        }
    }

    private byte[] stampPassWatermark(byte[] pdfBytes, DarshanPass pass) {
        try (PDDocument doc = PDDocument.load(pdfBytes); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String statusRaw = pass != null && pass.getStatus() != null ? pass.getStatus() : "pending";
            boolean isApproved = "approved".equalsIgnoreCase(statusRaw);
            boolean isRejected = "rejected".equalsIgnoreCase(statusRaw);
            String line1;
            String line2;
            java.awt.Color color;
            if (isRejected) {
                line1 = "INVALID PASS";
                line2 = "YOU DO NOT HAVE ENTRY ON TEMPLE";
                color = new java.awt.Color(185, 28, 28);
            } else if (isApproved) {
                line1 = "VALID PASS";
                line2 = "ENTRY PERMITTED";
                color = new java.awt.Color(22, 101, 52);
            } else {
                line1 = "PENDING APPROVAL";
                line2 = "AWAITING ADMIN";
                color = new java.awt.Color(161, 98, 7);
            }
            for (PDPage page : doc.getPages()) {
                PDRectangle media = page.getMediaBox();
                float cx = media.getWidth() / 2f;
                float cy = media.getHeight() / 2f;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setNonStrokingAlphaConstant(0.16f);
                    gs.setStrokingAlphaConstant(0.18f);
                    cs.setGraphicsStateParameters(gs);
                    cs.setNonStrokingColor(color);
                    cs.setStrokingColor(color);

                    // border box (rotated)
                    cs.saveGraphicsState();
                    cs.transform(Matrix.getRotateInstance(Math.toRadians(28), cx, cy));
                    float bw = Math.min(430, media.getWidth() * 0.78f);
                    float bh = 92;
                    cs.setLineWidth(3f);
                    cs.addRect(cx - bw / 2f, cy - bh / 2f, bw, bh);
                    cs.stroke();
                    cs.restoreGraphicsState();

                    cs.saveGraphicsState();
                    cs.transform(Matrix.getRotateInstance(Math.toRadians(28), cx, cy));
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 30);
                    float w1 = PDType1Font.HELVETICA_BOLD.getStringWidth(line1) / 1000f * 30f;
                    cs.newLineAtOffset(cx - w1 / 2f, cy + 8);
                    cs.showText(line1);
                    cs.endText();
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                    float w2 = PDType1Font.HELVETICA_BOLD.getStringWidth(line2) / 1000f * 13f;
                    cs.newLineAtOffset(cx - w2 / 2f, cy - 20);
                    cs.showText(line2);
                    cs.endText();
                    cs.restoreGraphicsState();
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("Pass watermark stamp failed, returning rendered PDF without stamp: {}", e.toString());
            return pdfBytes;
        }
    }

    private byte[] resolveClasspathImageBytes(String... candidates) {
        byte[] b = readResourceBytes(candidates);
        if (b != null) return b;
        return readFileBytes(candidates);
    }

    private byte[] readResourceBytes(String... candidates) {
        for (String c : candidates) {
            try {
                ClassPathResource res = new ClassPathResource(c);
                if (res.exists()) {
                    return res.getInputStream().readAllBytes();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private byte[] readFileBytes(String... candidates) {
        String[] bases = {
                "src/main/resources",
                "target/classes",
                "images",
                "."
        };
        for (String c : candidates) {
            if (c == null || c.isBlank()) continue;
            String rel = c.startsWith("static/") ? c : c;
            for (String base : bases) {
                try {
                    Path p = Paths.get(base, rel).normalize();
                    if (Files.exists(p) && Files.isRegularFile(p)) {
                        return Files.readAllBytes(p);
                    }
                } catch (Exception ignored) {}
                try {
                    // also try without static/ prefix under images/
                    if (c.startsWith("static/")) {
                        Path p = Paths.get(base, c.substring("static/".length())).normalize();
                        if (Files.exists(p) && Files.isRegularFile(p)) return Files.readAllBytes(p);
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private String resolveClasspathImageDataUri(String... candidates) {
        for (String c : candidates) {
            try {
                byte[] bytes = readResourceBytes(c);
                if (bytes == null) bytes = readFileBytes(c);
                if (bytes != null) {
                    // Resize/compress logos/images for compatibility and to keep PDFs under 5MB
                    String optimized = toOptimizedImageDataUri(bytes, 500, 500, false, 0.85f);
                    if (optimized != null) return optimized;
                    String mime = guessMime(c);
                    if ("image/svg+xml".equalsIgnoreCase(mime)) {
                        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                    }
                    return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                }
            } catch (Exception ignored) {}
        }
        // if passed paths not found individually, try combined scan
        byte[] bytes = resolveClasspathImageBytes(candidates);
        if (bytes != null) {
            String optimized = toOptimizedImageDataUri(bytes, 500, 500, false, 0.85f);
            if (optimized != null) return optimized;
            String mime = guessMime(candidates.length > 0 ? candidates[0] : "");
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
        return null;
    }

    private String toPngDataUriIfPossible(byte[] bytes) {
        return toOptimizedImageDataUri(bytes, 800, 800, false, 0.85f);
    }

    private String toOptimizedImageDataUri(byte[] bytes, int maxW, int maxH, boolean preferJpeg, float jpegQuality) {
        if (bytes == null || bytes.length == 0) return null;
        try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(bytes)) {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
            if (img == null) return null;

            int w = img.getWidth();
            int h = img.getHeight();
            if (w > maxW || h > maxH) {
                double scale = Math.min((double) maxW / (double) w, (double) maxH / (double) h);
                int nw = Math.max(1, (int) Math.round(w * scale));
                int nh = Math.max(1, (int) Math.round(h * scale));
                boolean hasAlpha = img.getColorModel() != null && img.getColorModel().hasAlpha();
                int type = (preferJpeg || !hasAlpha) ? java.awt.image.BufferedImage.TYPE_INT_RGB : java.awt.image.BufferedImage.TYPE_INT_ARGB;
                java.awt.image.BufferedImage resized = new java.awt.image.BufferedImage(nw, nh, type);
                java.awt.Graphics2D g = resized.createGraphics();
                try {
                    if (type == java.awt.image.BufferedImage.TYPE_INT_RGB) {
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, nw, nh);
                    }
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g.drawImage(img, 0, 0, nw, nh, null);
                } finally {
                    g.dispose();
                }
                img = resized;
            } else if (preferJpeg && img.getColorModel() != null && img.getColorModel().hasAlpha()) {
                // flatten alpha for JPEG
                java.awt.image.BufferedImage rgb = new java.awt.image.BufferedImage(img.getWidth(), img.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = rgb.createGraphics();
                try {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                    g.drawImage(img, 0, 0, null);
                } finally { g.dispose(); }
                img = rgb;
            }

            boolean hasAlphaNow = img.getColorModel() != null && img.getColorModel().hasAlpha();
            boolean useJpeg = preferJpeg && !hasAlphaNow;
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (useJpeg) {
                    java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO.getImageWritersByFormatName("jpeg");
                    if (writers.hasNext()) {
                        javax.imageio.ImageWriter writer = writers.next();
                        try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(out)) {
                            writer.setOutput(ios);
                            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                            if (param.canWriteCompressed()) {
                                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                                param.setCompressionQuality(Math.max(0.5f, Math.min(0.95f, jpegQuality)));
                            }
                            writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
                        } finally {
                            writer.dispose();
                        }
                        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
                    }
                }
                boolean ok = javax.imageio.ImageIO.write(img, "png", out);
                if (!ok) return null;
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveImageDataUri(Path path) {
        if (path == null) return null;
        try {
            if (!Files.exists(path) || !Files.isRegularFile(path)) return null;
            byte[] bytes = Files.readAllBytes(path);
            // Resize/compress photos to keep generated PDFs under ~5MB
            String optimized = toOptimizedImageDataUri(bytes, 900, 900, true, 0.82f);
            if (optimized != null) return optimized;
            String mime = guessMime(path.toString());
            if ("image/svg+xml".equalsIgnoreCase(mime)) {
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String guessMime(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }

    private byte[] generatePassPdfOpenPdf(DarshanPass pass, String recipientName, String profilePicFilename) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            String statusRaw = pass != null && pass.getStatus() != null ? pass.getStatus() : "pending";
            boolean isApproved = "approved".equalsIgnoreCase(statusRaw);
            boolean isRejected = "rejected".equalsIgnoreCase(statusRaw);
            boolean isFreeEntry = isFreeEntry(pass);

            drawPageBackground(writer, doc);
            drawPageFrame(writer, doc);
            drawWatermark(writer, isApproved, isRejected);

            doc.add(buildHeader(isApproved, isRejected));
            doc.add(buildStatusBanner(isApproved, isRejected));

            if (isRejected) {
                Paragraph rej = new Paragraph(
                        "INVALID PASS ? You do not have entry on temple. This pass was rejected by admin. Details below are for reference only.",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, RED_TEXT));
                rej.setSpacingAfter(8);
                PdfPTable rejWrap = singleCellWrap(rej, RED_BG, RED_BORDER, 8);
                rejWrap.setSpacingAfter(8);
                doc.add(rejWrap);
            } else if (isApproved) {
                Paragraph ok = new Paragraph(
                        "Pass approved by temple administration. This darshan pass is valid as per the details below. Please carry a valid government ID at the temple entrance.",
                        FontFactory.getFont(FontFactory.HELVETICA, 9.5f, GREEN_DARK));
                PdfPTable okWrap = singleCellWrap(ok, GREEN_BG, GREEN_BORDER, 8);
                okWrap.setSpacingAfter(8);
                doc.add(okWrap);
            } else {
                Paragraph pend = new Paragraph(
                        "Awaiting admin approval. Your pass request has been recorded. Once approved, this pass becomes valid as per temple administration.",
                        FontFactory.getFont(FontFactory.HELVETICA, 9.5f, YELLOW_TEXT));
                PdfPTable pWrap = singleCellWrap(pend, YELLOW_BG, YELLOW_BORDER, 8);
                pWrap.setSpacingAfter(8);
                doc.add(pWrap);
            }

            doc.add(buildIdentityPanel(pass, recipientName, profilePicFilename, isApproved, isRejected, isFreeEntry));

            PdfPTable sectionBar = new PdfPTable(1);
            sectionBar.setWidthPercentage(100);
            sectionBar.setSpacingBefore(8);
            sectionBar.setSpacingAfter(5);
            PdfPCell sbc = new PdfPCell(new Phrase("  PASS DETAILS",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11.5f, Color.WHITE)));
            sbc.setBackgroundColor(MAROON);
            sbc.setBorderColor(GOLD_BRIGHT);
            sbc.setBorderWidth(1.2f);
            sbc.setPadding(7);
            sbc.setPaddingLeft(10);
            sectionBar.addCell(sbc);
            doc.add(sectionBar);
            doc.add(buildDetailsTable(pass, recipientName, isApproved, isRejected, isFreeEntry));

            Paragraph note = new Paragraph(
                    "Present this pass (PDF/digital/print) with a valid photo ID at the temple entry. "
                            + (isRejected ? "This pass is NOT valid for entry."
                            : (isApproved ? "This pass is approved as per temple administration records." : "Entry is subject to admin approval.")),
                    FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(107, 83, 22)));
            note.setSpacingBefore(10);
            PdfPTable noteWrap = singleCellWrap(note, CREAM_WARM, CREAM_BORDER, 8);
            noteWrap.setSpacingBefore(10);
            doc.add(noteWrap);

            Paragraph footer = new Paragraph("? ??? ?????  |  Shri Trimbakeshwar Jyotirlinga Temple, Trimbak, Nashik",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, GOLD_DARK));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(8);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pass PDF", e);
        }
    }

    private void drawPageBackground(PdfWriter writer, Document doc) {
        try {
            PdfContentByte under = writer.getDirectContentUnder();
            Rectangle page = doc.getPageSize();
            float margin = 12f;
            // warm page background (matches pass-detail .pass-page / temple-pass feel)
            under.saveState();
            under.setColorFill(new Color(248, 243, 234)); // #f8f3ea
            under.rectangle(0, 0, page.getWidth(), page.getHeight());
            under.fill();
            // inner pass card area (lighter parchment)
            under.setColorFill(new Color(255, 253, 248)); // #fffdf8
            under.rectangle(margin + 8, margin + 8, page.getWidth() - (margin + 8) * 2, page.getHeight() - (margin + 8) * 2);
            under.fill();
            // subtle saffron/gold glow bands for richer background color
            under.setColorFill(new Color(255, 248, 225));
            under.rectangle(margin + 8, page.getHeight() - margin - 90, page.getWidth() - (margin + 8) * 2, 75);
            under.fill();
            under.setColorFill(new Color(255, 245, 235));
            under.rectangle(margin + 8, margin + 8, page.getWidth() - (margin + 8) * 2, 70);
            under.fill();
            under.restoreState();
        } catch (Exception ignored) {}
    }

    private void drawPageFrame(PdfWriter writer, Document doc) {
        try {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = doc.getPageSize();
            float margin = 12f;
            float w = page.getWidth(), h = page.getHeight();
            cb.saveState();
            // outer saffron border
            cb.setColorStroke(SAFFRON);
            cb.setLineWidth(3f);
            cb.rectangle(margin, margin, w - margin * 2, h - margin * 2);
            cb.stroke();
            // gold border
            cb.setColorStroke(GOLD_BRIGHT);
            cb.setLineWidth(1.5f);
            cb.rectangle(margin + 4, margin + 4, w - (margin + 4) * 2, h - (margin + 4) * 2);
            cb.stroke();
            // inner maroon accent
            cb.setColorStroke(MAROON);
            cb.setLineWidth(0.8f);
            cb.rectangle(margin + 7, margin + 7, w - (margin + 7) * 2, h - (margin + 7) * 2);
            cb.stroke();
            // top multicolor band
            cb.setColorFill(MAROON_DARK);
            cb.rectangle(margin, h - margin - 8, w - margin * 2, 8);
            cb.fill();
            cb.setColorFill(GOLD_BRIGHT);
            cb.rectangle(margin, h - margin - 12, w - margin * 2, 4);
            cb.fill();
            cb.setColorFill(SAFFRON);
            cb.rectangle(margin, h - margin - 15, w - margin * 2, 3);
            cb.fill();
            // bottom multicolor band
            cb.setColorFill(SAFFRON);
            cb.rectangle(margin, margin, w - margin * 2, 3);
            cb.fill();
            cb.setColorFill(GOLD_BRIGHT);
            cb.rectangle(margin, margin + 3, w - margin * 2, 4);
            cb.fill();
            cb.setColorFill(MAROON_DARK);
            cb.rectangle(margin, margin + 7, w - margin * 2, 6);
            cb.fill();
            // corner accents
            float cs = 18f;
            cb.setColorFill(GOLD_BRIGHT);
            cb.rectangle(margin, h - margin - cs, cs, cs); cb.fill();
            cb.rectangle(w - margin - cs, h - margin - cs, cs, cs); cb.fill();
            cb.rectangle(margin, margin, cs, cs); cb.fill();
            cb.rectangle(w - margin - cs, margin, cs, cs); cb.fill();
            cb.setColorFill(MAROON);
            float ci = 10f, off = 4f;
            cb.rectangle(margin + off, h - margin - off - ci, ci, ci); cb.fill();
            cb.rectangle(w - margin - off - ci, h - margin - off - ci, ci, ci); cb.fill();
            cb.rectangle(margin + off, margin + off, ci, ci); cb.fill();
            cb.rectangle(w - margin - off - ci, margin + off, ci, ci); cb.fill();
            cb.restoreState();
        } catch (Exception ignored) {}
    }

    private void drawWatermark(PdfWriter writer, boolean isApproved, boolean isRejected) {
        try {
            PdfContentByte under = writer.getDirectContentUnder();
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(isRejected ? 0.10f : 0.08f);
            under.saveState();
            under.setGState(gs);
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            under.beginText();
            under.setFontAndSize(bf, isRejected ? 34 : 30);
            under.setColorFill(isRejected ? RED_TEXT : (isApproved ? GREEN_TEXT : GOLD_DARK));
            float x = 300f, y = 420f, angle = 35f;
            if (isRejected) {
                under.showTextAligned(Element.ALIGN_CENTER, "INVALID PASS", x, y + 18, angle);
                under.showTextAligned(Element.ALIGN_CENTER, "YOU DO NOT HAVE ENTRY ON TEMPLE", x, y - 18, angle);
            } else if (isApproved) {
                under.showTextAligned(Element.ALIGN_CENTER, "VALID PASS", x, y + 14, angle);
                under.showTextAligned(Element.ALIGN_CENTER, "ENTRY PERMITTED", x, y - 18, angle);
            } else {
                under.showTextAligned(Element.ALIGN_CENTER, "PENDING APPROVAL", x, y, angle);
            }
            under.endText();
            under.restoreState();
        } catch (Exception ignored) {}
    }

    private PdfPTable buildHeader(boolean isApproved, boolean isRejected) {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingAfter(0);

        PdfPCell topBand = new PdfPCell(new Phrase("à¥¥ SHRI TRIMBAKESHWAR JYOTIRLINGA TEMPLE à¥¥",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MAROON_DARK)));
        topBand.setBackgroundColor(GOLD_BRIGHT);
        topBand.setBorderColor(SAFFRON);
        topBand.setBorderWidth(1.2f);
        topBand.setHorizontalAlignment(Element.ALIGN_CENTER);
        topBand.setPadding(5);
        header.addCell(topBand);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(MAROON);
        cell.setBorderColor(GOLD_BRIGHT);
        cell.setBorderWidthLeft(1.5f);
        cell.setBorderWidthRight(1.5f);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthBottom(0f);
        cell.setPadding(14);
        cell.setPaddingTop(14);
        cell.setPaddingBottom(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 21, Color.WHITE);
        Font sub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11.5f, new Color(255, 236, 179));
        Font mantra = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10.5f, SAFFRON_LIGHT);
        Paragraph p1 = new Paragraph("Shri Trimbakeshwar Jyotirlinga Temple", title);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph("OFFICIAL DARSHAN PASS  â€¢  TEMPLE ERP", sub);
        p2.setAlignment(Element.ALIGN_CENTER);
        p2.setSpacingBefore(3);
        Paragraph p3 = new Paragraph("à¥ à¤¨à¤®à¤ƒ à¤¶à¤¿à¤µà¤¾à¤¯  |  Trimbak, Nashik, Maharashtra", mantra);
        p3.setAlignment(Element.ALIGN_CENTER);
        p3.setSpacingBefore(5);
        Paragraph orn = new Paragraph("âœ¦  âœ¦  âœ¦  âœ¦  âœ¦", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, GOLD_BRIGHT));
        orn.setAlignment(Element.ALIGN_CENTER);
        orn.setSpacingBefore(4);
        cell.addElement(p1);
        cell.addElement(p2);
        cell.addElement(p3);
        cell.addElement(orn);
        header.addCell(cell);

        PdfPCell bottomBand = new PdfPCell(new Phrase("DARSHAN ENTRY PASS",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        bottomBand.setBackgroundColor(SAFFRON);
        bottomBand.setBorderColor(GOLD_BRIGHT);
        bottomBand.setBorderWidth(1.2f);
        bottomBand.setHorizontalAlignment(Element.ALIGN_CENTER);
        bottomBand.setPadding(5);
        header.addCell(bottomBand);
        return header;
    }

    private PdfPTable buildStatusBanner(boolean isApproved, boolean isRejected) {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(8);
        banner.setSpacingAfter(8);
        String statusLabel = isApproved ? "âœ”  APPROVED / VALID PASS  â€”  ENTRY PERMITTED"
                : (isRejected ? "âœ˜  REJECTED â€” INVALID PASS  (NO ENTRY ON TEMPLE)"
                : "â³  PENDING ADMIN APPROVAL");
        Color bg = isRejected ? RED_BG : (isApproved ? GREEN_BG : YELLOW_BG);
        Color border = isRejected ? RED_BORDER : (isApproved ? GREEN_BORDER : YELLOW_BORDER);
        Color fg = isRejected ? RED_TEXT : (isApproved ? GREEN_TEXT : YELLOW_TEXT);
        PdfPCell bcell = new PdfPCell(new Phrase(statusLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, fg)));
        bcell.setPadding(11);
        bcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        bcell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bcell.setBackgroundColor(bg);
        bcell.setBorderColor(border);
        bcell.setBorderWidth(2f);
        banner.addCell(bcell);
        return banner;
    }

    private PdfPTable buildIdentityPanel(DarshanPass pass, String recipientName, String profilePicFilename,
                                         boolean isApproved, boolean isRejected, boolean isFreeEntry) throws Exception {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(6);
        PdfPCell outerCell = new PdfPCell();
        outerCell.setBackgroundColor(new Color(255, 250, 240));
        outerCell.setBorderColor(CREAM_BORDER);
        outerCell.setBorderWidth(1f);
        outerCell.setPadding(10);

        PdfPTable inner = new PdfPTable(new float[]{22f, 46f, 32f});
        inner.setWidthPercentage(100);

        // Photo
        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorderColor(GOLD);
        photoCell.setBorderWidth(1f);
        photoCell.setBackgroundColor(Color.WHITE);
        photoCell.setPadding(6);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph photoLabel = new Paragraph("DEVOTEE PHOTO",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, LABEL));
        photoLabel.setAlignment(Element.ALIGN_CENTER);
        photoLabel.setSpacingAfter(4);
        photoCell.addElement(photoLabel);
        boolean addedImage = false;
        try {
            Path imgPath = resolveProfileImage(profilePicFilename);
            if (imgPath != null && Files.exists(imgPath)) {
                Image img = Image.getInstance(imgPath.toAbsolutePath().toString());
                img.scaleToFit(78, 90);
                img.setAlignment(Element.ALIGN_CENTER);
                photoCell.addElement(img);
                addedImage = true;
            }
        } catch (Exception ignored) {}
        if (!addedImage) {
            PdfPTable ph = new PdfPTable(1);
            ph.setWidthPercentage(90);
            PdfPCell phc = new PdfPCell(new Phrase("No\nPhoto", FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(148, 163, 184))));
            phc.setFixedHeight(85);
            phc.setHorizontalAlignment(Element.ALIGN_CENTER);
            phc.setVerticalAlignment(Element.ALIGN_MIDDLE);
            phc.setBackgroundColor(new Color(248, 250, 252));
            phc.setBorderColor(CREAM_BORDER);
            ph.addCell(phc);
            photoCell.addElement(ph);
        }
        inner.addCell(photoCell);

        // Middle info
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setPaddingLeft(10);
        infoCell.setPaddingRight(6);
        infoCell.setVerticalAlignment(Element.ALIGN_TOP);
        String passId = safe(pass != null ? pass.getPassId() : null, pass != null ? pass.getId() : null);
        String name = safe(pass != null ? pass.getDevoteeName() : null, recipientName);
        Paragraph idLbl = new Paragraph("DARSHAN PASS ID", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, LABEL));
        Paragraph idVal = new Paragraph(passId, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, TEXT_DARK));
        idVal.setSpacingAfter(4);
        infoCell.addElement(idLbl);
        infoCell.addElement(idVal);
        infoCell.addElement(infoLine("Devotee: ", name));
        if (pass != null && pass.getPassType() != null && !pass.getPassType().isBlank()) {
            Phrase pt = new Phrase();
            pt.add(new Chunk("Pass Type: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, TEXT_DARK)));
            pt.add(new Chunk(pass.getPassType(), FontFactory.getFont(FontFactory.HELVETICA, 9.5f, TEXT_DARK)));
            if (isFreeEntry) {
                pt.add(new Chunk("  [FREE ENTRY]", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, GREEN_DARK)));
            }
            Paragraph ptp = new Paragraph(pt);
            ptp.setSpacingAfter(2);
            infoCell.addElement(ptp);
        }
        String visit = (pass != null && pass.getVisitDate() != null ? pass.getVisitDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "-");
        if (pass != null && pass.getVisitTime() != null && !pass.getVisitTime().isBlank()) visit += " | " + pass.getVisitTime();
        int persons = pass != null && pass.getNoOfPersons() != null && pass.getNoOfPersons() > 0 ? pass.getNoOfPersons() : 1;
        infoCell.addElement(infoLine("Visit: ", visit + "   â€¢   Persons: " + persons));
        Paragraph idNote = new Paragraph("Present this pass with a valid photo ID at temple entry.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, new Color(107, 83, 22)));
        idNote.setSpacingBefore(6);
        infoCell.addElement(idNote);
        inner.addCell(infoCell);

        // Payment card
        inner.addCell(buildPaymentCard(pass, isApproved, isRejected, isFreeEntry));

        outerCell.addElement(inner);
        outer.addCell(outerCell);
        return outer;
    }

    private Paragraph infoLine(String label, String value) {
        Phrase p = new Phrase();
        p.add(new Chunk(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, TEXT_DARK)));
        p.add(new Chunk(value == null ? "-" : value, FontFactory.getFont(FontFactory.HELVETICA, 9.5f, TEXT_DARK)));
        Paragraph para = new Paragraph(p);
        para.setSpacingAfter(2);
        return para;
    }

    private PdfPCell buildPaymentCard(DarshanPass pass, boolean isApproved, boolean isRejected, boolean isFreeEntry) {
        Color bg = isRejected ? RED_BG : (isApproved ? GREEN_BG : YELLOW_BG);
        Color border = isRejected ? RED_BORDER : (isApproved ? GREEN_BORDER : YELLOW_BORDER);
        Color fg = isRejected ? RED_TEXT : (isApproved ? GREEN_TEXT : YELLOW_TEXT);
        Color fgDark = isRejected ? new Color(127, 29, 29) : (isApproved ? GREEN_DARK : new Color(113, 63, 18));

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setBorderWidth(1.2f);
        cell.setPadding(9);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String payStatus = isFreeEntry ? "Free / Not Required" : safe(pass != null ? pass.getPaymentStatus() : null);
        int persons = pass != null && pass.getNoOfPersons() != null && pass.getNoOfPersons() > 0 ? pass.getNoOfPersons() : 1;
        double amt = pass != null && pass.getAmount() != null ? pass.getAmount() : 0.0;
        double computed = (!isFreeEntry && amt * persons > 0) ? (amt * persons) : (pass != null && pass.getTotalAmount() != null ? pass.getTotalAmount() : 0.0);
        if (!isFreeEntry && computed <= 0) computed = amt * persons;
        String totalText = isFreeEntry ? "₹0.00 (Free Entry)" : "₹" + MONEY.format(computed);

        Paragraph lbl = new Paragraph("PAYMENT STATUS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, fgDark));
        lbl.setSpacingAfter(3);
        Paragraph val = new Paragraph(payStatus.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, fg));
        val.setSpacingAfter(6);
        cell.addElement(lbl);
        cell.addElement(val);
        cell.addElement(smallMeta("Method: ", safe(pass != null ? pass.getPaymentMethod() : null), fgDark));
        cell.addElement(smallMeta("Total: ", totalText, fgDark));
        if (!isFreeEntry && persons > 1) {
            cell.addElement(smallMeta("", persons + " \u00D7 \u20B9" + MONEY.format(amt) + " per person", fgDark));
        }
        return cell;
    }

    private Paragraph smallMeta(String label, String value, Color color) {
        Phrase p = new Phrase();
        if (label != null && !label.isEmpty()) p.add(new Chunk(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, color)));
        p.add(new Chunk(value == null ? "-" : value, FontFactory.getFont(FontFactory.HELVETICA, 8.5f, color)));
        Paragraph para = new Paragraph(p);
        para.setSpacingAfter(2);
        return para;
    }

    private PdfPTable buildDetailsTable(DarshanPass pass, String recipientName, boolean isApproved, boolean isRejected, boolean isFreeEntry) {
        int persons = pass != null && pass.getNoOfPersons() != null && pass.getNoOfPersons() > 0 ? pass.getNoOfPersons() : 1;
        double amt = pass != null && pass.getAmount() != null ? pass.getAmount() : 0.0;
        double computed = (!isFreeEntry && amt * persons > 0) ? (amt * persons) : (pass != null && pass.getTotalAmount() != null ? pass.getTotalAmount() : 0.0);
        if (!isFreeEntry && computed <= 0) computed = amt * persons;
        String totalText = isFreeEntry ? "₹0.00 (Free Entry / Not Required)" : "₹" + MONEY.format(computed) + (persons > 1 ? " (" + persons + " Ã— ₹" + MONEY.format(amt) + " per person)" : "");
        String payStatus = isFreeEntry ? "Free / Not Required" : safe(pass != null ? pass.getPaymentStatus() : null);
        Color statusColor = isRejected ? RED_TEXT : (isApproved ? GREEN_TEXT : YELLOW_TEXT);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{22f, 28f, 22f, 28f}); } catch (Exception ignored) {}

        addField(table, "Devotee Name", safe(pass != null ? pass.getDevoteeName() : null, recipientName));
        addField(table, "Darshan Pass ID", safe(pass != null ? pass.getPassId() : null, pass != null ? pass.getId() : null));
        addField(table, "Pass Type", pass != null ? pass.getPassType() : null);
        addField(table, "Status", isApproved ? "Approved / Valid" : (isRejected ? "Rejected / Invalid" : "Pending Approval"), statusColor, true);
        addField(table, "Visit Date", pass != null && pass.getVisitDate() != null ? pass.getVisitDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        addField(table, "Visit Time", pass != null ? pass.getVisitTime() : null);
        addField(table, "No. of Persons", String.valueOf(persons));
        addField(table, "Gender", pass != null ? pass.getGender() : null);
        addField(table, "Phone", pass != null ? pass.getPhone() : null);
        addField(table, "Email", pass != null ? pass.getEmail() : null);
        addField(table, "Aadhar Number", pass != null ? pass.getAadharNumber() : null);
        addField(table, "Amount (Per Person)", formatMoney(pass != null ? pass.getAmount() : null));
        addField(table, "Total Amount", totalText, isFreeEntry ? GREEN_DARK : TEXT_DARK, true);
        addField(table, "Payment Method", pass != null ? pass.getPaymentMethod() : null);
        addField(table, "Payment Status", payStatus, statusColor, true);
        addField(table, "Payment Date", formatInstant(pass != null ? pass.getPaymentDate() : null));
        addField(table, "Transaction / Trust Ref", pass != null ? pass.getTransactionId() : null);
        addField(table, "Created At", formatInstant(pass != null ? pass.getCreatedAt() : null));
        addFieldWide(table, "Address", pass != null ? pass.getAddress() : null);
        addFieldWide(table, "Aadhar Document", pass != null && pass.getAadharCard() != null && !pass.getAadharCard().isBlank() ? ("Uploaded: " + pass.getAadharCard()) : "-");
        addFieldWide(table, "Special Requests", pass != null ? pass.getSpecialRequests() : null);
        return table;
    }

    private void addField(PdfPTable table, String label, String value) {
        addField(table, label, value, TEXT_DARK, false);
    }

    private void addField(PdfPTable table, String label, String value, Color valueColor, boolean boldValue) {
        PdfPCell l = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, LABEL)));
        l.setBackgroundColor(new Color(255, 248, 235));
        l.setBorderColor(CREAM_BORDER);
        l.setPadding(6);
        Font vf = boldValue ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, valueColor)
                : FontFactory.getFont(FontFactory.HELVETICA, 9.5f, valueColor);
        PdfPCell v = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, vf));
        v.setBackgroundColor(Color.WHITE);
        v.setBorderColor(CREAM_BORDER);
        v.setPadding(6);
        table.addCell(l);
        table.addCell(v);
    }

    private void addFieldWide(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, LABEL)));
        l.setBackgroundColor(new Color(255, 248, 235));
        l.setBorderColor(CREAM_BORDER);
        l.setPadding(6);
        table.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value,
                FontFactory.getFont(FontFactory.HELVETICA, 9.5f, TEXT_DARK)));
        v.setColspan(3);
        v.setBackgroundColor(Color.WHITE);
        v.setBorderColor(CREAM_BORDER);
        v.setPadding(6);
        table.addCell(v);
    }

    private PdfPTable singleCellWrap(Paragraph p, Color bg, Color border, float padding) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.addElement(p);
        c.setBackgroundColor(bg);
        c.setBorderColor(border);
        c.setBorderWidth(1f);
        c.setPadding(padding);
        t.addCell(c);
        return t;
    }

    private Path resolveProfileImage(String profilePicFilename) {
        if (profilePicFilename == null) return null;
        String raw = profilePicFilename.trim();
        if (raw.isEmpty()) return null;
        // strip common URL/path prefixes
        if (raw.startsWith("http://") || raw.startsWith("https://")) return null;
        if (raw.startsWith("/uploads/profile/")) raw = raw.substring("/uploads/profile/".length());
        else if (raw.startsWith("uploads/profile/")) raw = raw.substring("uploads/profile/".length());
        else if (raw.startsWith("/")) raw = raw.substring(raw.lastIndexOf('/') + 1);
        try {
            Path root = fileStorageService.getRoot();
            Path p = root.resolve("profile").resolve(raw).normalize();
            if (Files.exists(p)) return p;
            // fallback: try direct under root
            p = root.resolve(raw).normalize();
            if (Files.exists(p)) return p;
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isFreeEntry(DarshanPass pass) {
        if (pass == null) return false;
        try {
            if (pass.getTotalAmount() != null && pass.getTotalAmount() <= 0) return true;
            String ps = pass.getPaymentStatus() != null ? pass.getPaymentStatus().toLowerCase(Locale.ROOT) : "";
            if (java.util.Set.of("not_required", "free", "waived", "na", "n/a").contains(ps)) return true;
            String pm = pass.getPaymentMethod() != null ? pass.getPaymentMethod().toLowerCase(Locale.ROOT) : "";
            if (pm.contains("trust") || pm.contains("free")) return true;
            String pt = pass.getPassType() != null ? pass.getPassType().toLowerCase(Locale.ROOT) : "";
            return pt.contains("trust") || pt.contains("guest entry") || pt.contains("special guest") || pt.contains("free") || pt.contains("complimentary");
        } catch (Exception e) { return false; }
    }

    private String formatInstant(java.time.Instant i) {
        if (i == null) return "-";
        try {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(i);
        } catch (Exception e) { return i.toString(); }
    }

    public byte[] generateBookingTokenPdf(RoomBooking b, String recipientName) {
        return generateBookingTokenPdf(b, recipientName, null);
    }

    public byte[] generateBookingTokenPdf(RoomBooking b, String recipientName, String profilePicFilename) {
        String ref = b != null ? (b.getBookingId() != null ? b.getBookingId() : b.getId()) : "null";
        try {
            return generateBookingTokenPdfHtml(b, recipientName, profilePicFilename, ref);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTML PDF render failed [template=pdf/booking-token-render, type=RoomBooking, ref={}]", ref, e);
            throw new RuntimeException("Failed to generate Booking token PDF (HTML render). Ref=" + ref, e);
        }
    }

    private byte[] generateBookingTokenPdfHtml(RoomBooking b, String recipientName, String profilePicFilename, String ref) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("pdfMode", true);
        ctx.setVariable("booking", b);
        if (b != null && (b.getGuestName() == null || b.getGuestName().isBlank()) && recipientName != null) {
            b.setGuestName(recipientName);
        }

        // Match web booking token view context (single source of truth): tokenApproved + bookingUser/profile
        String st = b != null && b.getBookingStatus() != null ? b.getBookingStatus().toLowerCase() : "";
        boolean approved = "confirmed".equals(st) || "approved".equals(st);
        ctx.setVariable("tokenApproved", approved);

        User bookingUser = null;
        try {
            if (b != null && b.getUserId() != null && !b.getUserId().isBlank()) {
                bookingUser = userRepository.findById(b.getUserId()).orElse(null);
            }
        } catch (Exception ignored) {}
        ctx.setVariable("bookingUser", bookingUser);

        String pic = profilePicFilename;
        if ((pic == null || pic.isBlank()) && bookingUser != null) pic = bookingUser.getProfilePic();
        String photoDataUri = resolveImageDataUri(resolveProfileImage(pic));
        // Provide both raw and data-URI forms so fragment renders same as web (with embeddable image for PDF)
        ctx.setVariable("bookingUserProfilePic", pic);
        ctx.setVariable("bookingPhotoSrc", photoDataUri);

        String logoDataUri = resolveClasspathImageDataUri(
                "images/temple/icon symbol.png",
                "images/temple/icon symbol.webp",
                "images/temple/icon-symbol.png",
                "images/temple/icon-symbol.webp",
                "static/images/temple/icon-symbol.png",
                "static/images/temple/icon-symbol.webp",
                "static/images/temple/icon symbol.webp",
                "static/favicon.svg",
                "static/images/temple/logo.png");
        if (logoDataUri == null) {
            logoDataUri = toPngDataUriIfPossible(resolveClasspathImageBytes(
                    "static/images/temple/icon-symbol.webp",
                    "static/images/temple/icon symbol.webp",
                    "images/temple/icon symbol.png"));
        }
        ctx.setVariable("bookingLogoSrc", logoDataUri);

        return renderHtmlToPdf("pdf/booking-token-render", "RoomBooking", ref, ctx);
    }

    private byte[] generateBookingTokenPdfOpenPdf(RoomBooking b, String recipientName, String profilePicFilename) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();
            drawPageBackground(writer, doc);
            drawPageFrame(writer, doc);

            String bs = b != null && b.getBookingStatus() != null ? b.getBookingStatus() : "Pending";
            boolean approved = "confirmed".equalsIgnoreCase(bs) || "approved".equalsIgnoreCase(bs);
            boolean rejected = "rejected".equalsIgnoreCase(bs) || "cancelled".equalsIgnoreCase(bs);
            String statusLabel = approved ? "APPROVED / TOKEN GENERATED" : (rejected ? "REJECTED BY ADMIN" : "SUBMITTED / PENDING ADMIN APPROVAL");
            Color stBg = approved ? GREEN_BG : (rejected ? RED_BG : YELLOW_BG);
            Color stBorder = approved ? GREEN_BORDER : (rejected ? RED_BORDER : YELLOW_BORDER);
            Color stFg = approved ? GREEN_TEXT : (rejected ? RED_TEXT : YELLOW_TEXT);

            PdfPTable header = simpleTempleHeader("Shri Trimbakeshwar Temple - Guest House", "Room Booking Token / Confirmation Pass  ·  Temple ERP", MAROON, new Color(37, 99, 235));
            header.setSpacingAfter(8);
            doc.add(header);
            doc.add(statusBanner(statusLabel, stBg, stBorder, stFg));

            String name = safe(b != null ? b.getGuestName() : null, recipientName);
            String tokenId = safe(b != null ? b.getBookingId() : null, b != null ? b.getId() : null);
            doc.add(identityPanelWithPhoto("GUEST PHOTO", "BOOKING / TOKEN ID", tokenId,
                    new String[]{"Guest: " + name,
                            "Room: " + safe(b != null ? b.getRoomType() : null) + "  |  Guests: " + (b != null && b.getNoOfGuests() != null ? b.getNoOfGuests() : "-")},
                    "This booking token is generated after admin approval (payment may be recorded during booking). Present it (digital or printed) at check-in.",
                    profilePicFilename, new Color(255, 250, 240)));

            doc.add(sectionBar("BOOKING DETAILS", MAROON));
            PdfPTable table = themedDetailsTable(4);
            addField(table, "Booking ID", tokenId);
            addField(table, "Guest Name", name);
            addField(table, "Phone", b != null ? b.getPhone() : null);
            addField(table, "Email", b != null ? b.getEmail() : null);
            addField(table, "Room Type", b != null ? b.getRoomType() : null);
            addField(table, "No. of Rooms", b != null && b.getNoOfRooms() != null ? String.valueOf(b.getNoOfRooms()) : "1");
            addField(table, "Check-in", b != null && b.getCheckIn() != null ? b.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            addField(table, "Check-out", b != null && b.getCheckOut() != null ? b.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            addField(table, "No. of Guests", b != null && b.getNoOfGuests() != null ? String.valueOf(b.getNoOfGuests()) : null);
            addField(table, "Payment Method", b != null ? b.getPaymentMethod() : null);
            addField(table, "Payment Status", b != null ? b.getPaymentStatus() : null, approved ? GREEN_TEXT : YELLOW_TEXT, true);
            addField(table, "Booking Status", bs, stFg, true);
            doc.add(table);

            doc.add(totalHighlight("Total Amount", "Booking token / confirmation", formatMoney(b != null ? b.getTotalAmount() : null), MAROON, new Color(37, 99, 235)));
            doc.add(templeFooterNote());
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate booking token PDF", e);
        }
    }
    public byte[] generateRestaurantTokenPdf(RestaurantOrder o, String recipientName) {
        return generateRestaurantTokenPdf(o, recipientName, null);
    }

    public byte[] generateRestaurantTokenPdf(RestaurantOrder o, String recipientName, String profilePicFilename) {
        String ref = o != null ? (o.getOrderId() != null ? o.getOrderId() : o.getId()) : "null";
        try {
            return generateRestaurantTokenPdfHtml(o, recipientName, profilePicFilename, ref);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTML PDF render failed [template=pdf/restaurant-token-render, type=RestaurantOrder, ref={}]", ref, e);
            throw new RuntimeException("Failed to generate Restaurant token PDF (HTML render). Ref=" + ref, e);
        }
    }

    private byte[] generateRestaurantTokenPdfHtml(RestaurantOrder o, String recipientName, String profilePicFilename, String ref) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("pdfMode", true);
        ctx.setVariable("order", o);
        User orderUser = null;
        try { if (o != null && o.getUserId() != null) orderUser = userRepository.findById(o.getUserId()).orElse(null); } catch (Exception ignored) {}
        ctx.setVariable("orderUser", orderUser);
        String pic = profilePicFilename;
        if ((pic == null || pic.isBlank()) && orderUser != null) pic = orderUser.getProfilePic();
        String photoDataUri = resolveImageDataUri(resolveProfileImage(pic));
        ctx.setVariable("orderPhotoSrc", photoDataUri);
        ctx.setVariable("orderUserProfilePic", photoDataUri);
        String logoDataUri = resolveClasspathImageDataUri(
                "images/temple/icon symbol.png",
                "images/temple/icon symbol.webp",
                "images/temple/icon-symbol.png",
                "images/temple/icon-symbol.webp",
                "static/images/temple/icon-symbol.png",
                "static/images/temple/icon-symbol.webp",
                "static/images/temple/icon symbol.webp",
                "static/favicon.svg",
                "static/images/temple/logo.png");
        if (logoDataUri == null) {
            logoDataUri = toPngDataUriIfPossible(resolveClasspathImageBytes(
                    "static/images/temple/icon-symbol.webp",
                    "static/images/temple/icon symbol.webp",
                    "images/temple/icon symbol.png"));
        }
        ctx.setVariable("orderLogoSrc", logoDataUri);
        return renderHtmlToPdf("pdf/restaurant-token-render", "RestaurantOrder", ref, ctx);
    }

    private byte[] generateRestaurantTokenPdfOpenPdf(RestaurantOrder o, String recipientName, String profilePicFilename) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();
            drawPageBackground(writer, doc);
            drawPageFrame(writer, doc);

            String status = o != null && o.getStatus() != null ? o.getStatus() : "pending";
            String s = status.toLowerCase(Locale.ROOT);
            boolean rejected = s.contains("reject") || s.contains("cancel");
            boolean approved = !rejected && !s.isEmpty() && !s.equals("pending");
            String statusLabel = approved ? "APPROVED / TOKEN GENERATED" : (rejected ? "REJECTED BY ADMIN" : "PENDING ADMIN APPROVAL");
            Color stBg = approved ? GREEN_BG : (rejected ? RED_BG : YELLOW_BG);
            Color stBorder = approved ? GREEN_BORDER : (rejected ? RED_BORDER : YELLOW_BORDER);
            Color stFg = approved ? GREEN_TEXT : (rejected ? RED_TEXT : YELLOW_TEXT);

            PdfPTable header = simpleTempleHeader("Shri Trimbakeshwar Temple - Annakshetra", "Restaurant Order Token Pass  ·  Temple ERP", new Color(20, 83, 45), new Color(15, 118, 110));
            header.setSpacingAfter(8);
            doc.add(header);
            doc.add(statusBanner(statusLabel, stBg, stBorder, stFg));

            String name = safe(o != null ? o.getName() : null, recipientName);
            String tokenId = safe(o != null ? o.getOrderId() : null, o != null ? o.getId() : null);
            doc.add(identityPanelWithPhoto("USER PHOTO", "ORDER / TOKEN ID", tokenId,
                    new String[]{"Name: " + name, "Phone: " + safe(o != null ? o.getPhone() : null)},
                    approved ? "This order has been approved/updated by admin. Show this token at the restaurant counter if required."
                            : (rejected ? "This order was rejected by admin." : "Order details are shown below. Token becomes valid after admin approval/update."),
                    profilePicFilename, new Color(240, 253, 244)));

            if (o != null && o.getItems() != null && !o.getItems().isEmpty()) {
                doc.add(sectionBar("ORDER ITEMS", new Color(20, 83, 45)));
                PdfPTable items = new PdfPTable(4);
                items.setWidthPercentage(100);
                items.setWidths(new float[]{46f, 14f, 20f, 20f});
                Font lbl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, LABEL);
                Font val = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, TEXT_DARK);
                headerCell(items, "Item", lbl); headerCell(items, "Qty", lbl); headerCell(items, "Price", lbl); headerCell(items, "Line Total", lbl);
                for (var it : o.getItems()) {
                    String iname = it != null && it.getName() != null ? it.getName() : "-";
                    int q = it != null && it.getQty() != null ? it.getQty() : 1;
                    double p = it != null && it.getPrice() != null ? it.getPrice() : 0.0;
                    PdfPCell c1 = new PdfPCell(new Phrase(iname, val)); c1.setPadding(6); c1.setBorderColor(CREAM_BORDER); items.addCell(c1);
                    PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(q), val)); c2.setPadding(6); c2.setBorderColor(CREAM_BORDER); c2.setHorizontalAlignment(Element.ALIGN_CENTER); items.addCell(c2);
                    PdfPCell c3 = new PdfPCell(new Phrase("₹" + MONEY.format(p), val)); c3.setPadding(6); c3.setBorderColor(CREAM_BORDER); c3.setHorizontalAlignment(Element.ALIGN_RIGHT); items.addCell(c3);
                    PdfPCell c4 = new PdfPCell(new Phrase("₹" + MONEY.format(p * q), val)); c4.setPadding(6); c4.setBorderColor(CREAM_BORDER); c4.setHorizontalAlignment(Element.ALIGN_RIGHT); items.addCell(c4);
                }
                items.setSpacingAfter(8);
                doc.add(items);
            }

            doc.add(sectionBar("TOKEN DETAILS", new Color(20, 83, 45)));
            PdfPTable table = themedDetailsTable(4);
            addField(table, "Order ID", tokenId);
            addField(table, "Status", status, stFg, true);
            addField(table, "Payment Method", o != null ? o.getPaymentMethod() : null);
            addField(table, "Payment Status", o != null ? o.getPaymentStatus() : null);
            addField(table, "Order Date", o != null && o.getOrderDate() != null ? o.getOrderDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            addField(table, "Order Time", o != null ? o.getOrderTime() : null);
            doc.add(table);

            doc.add(totalHighlight("Grand Total", "Multi-item restaurant order token", formatMoney(o != null ? o.getTotalAmount() : null), new Color(20, 83, 45), new Color(15, 118, 110)));
            doc.add(templeFooterNote());
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate restaurant token PDF", e);
        }
    }
    public byte[] generateDonationReceiptPdf(Donation d, String recipientName) {
        return generateDonationReceiptPdf(d, recipientName, null);
    }

    public byte[] generateDonationReceiptPdf(Donation d, String recipientName, String profilePicFilename) {
        String ref = d != null ? (d.getReceiptNo() != null ? d.getReceiptNo() : d.getId()) : "null";
        try {
            return generateDonationReceiptPdfHtml(d, recipientName, profilePicFilename, ref);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTML PDF render failed [template=pdf/donation-receipt-render, type=Donation, ref={}]", ref, e);
            throw new RuntimeException("Failed to generate Donation receipt PDF (HTML render). Ref=" + ref, e);
        }
    }

    private byte[] generateDonationReceiptPdfHtml(Donation d, String recipientName, String profilePicFilename, String ref) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("pdfMode", true);
        ctx.setVariable("donation", d);
        User donorUser = null;
        try { if (d != null && d.getUserId() != null) donorUser = userRepository.findById(d.getUserId()).orElse(null); } catch (Exception ignored) {}
        ctx.setVariable("donorUser", donorUser);
        String pic = profilePicFilename;
        if ((pic == null || pic.isBlank()) && donorUser != null) pic = donorUser.getProfilePic();
        String photoDataUri = resolveImageDataUri(resolveProfileImage(pic));
        ctx.setVariable("donationPhotoSrc", photoDataUri);
        ctx.setVariable("donorProfilePic", photoDataUri);
        String logoDataUri = resolveClasspathImageDataUri(
                "images/temple/icon symbol.png",
                "images/temple/icon symbol.webp",
                "images/temple/icon-symbol.png",
                "images/temple/icon-symbol.webp",
                "static/images/temple/icon-symbol.png",
                "static/images/temple/icon-symbol.webp",
                "static/images/temple/icon symbol.webp",
                "static/favicon.svg",
                "static/images/temple/logo.png");
        if (logoDataUri == null) {
            logoDataUri = toPngDataUriIfPossible(resolveClasspathImageBytes(
                    "static/images/temple/icon-symbol.webp",
                    "static/images/temple/icon symbol.webp",
                    "images/temple/icon symbol.png"));
        }
        ctx.setVariable("donationLogoSrc", logoDataUri);
        return renderHtmlToPdf("pdf/donation-receipt-render", "Donation", ref, ctx);
    }

    private byte[] generateDonationReceiptPdfOpenPdf(Donation d, String recipientName, String profilePicFilename) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();
            drawPageBackground(writer, doc);
            drawPageFrame(writer, doc);

            String status = d != null && d.getPaymentStatus() != null ? d.getPaymentStatus() : "-";
            String sl = status.toLowerCase(Locale.ROOT);
            boolean completed = sl.contains("complete") || sl.contains("paid") || sl.contains("success");
            boolean failed = sl.contains("fail") || sl.contains("reject");
            String statusLabel = completed ? "COMPLETED / RECEIPT GENERATED" : (failed ? "PAYMENT FAILED / REJECTED" : "PENDING / " + status.toUpperCase());
            Color stBg = completed ? GREEN_BG : (failed ? RED_BG : YELLOW_BG);
            Color stBorder = completed ? GREEN_BORDER : (failed ? RED_BORDER : YELLOW_BORDER);
            Color stFg = completed ? GREEN_TEXT : (failed ? RED_TEXT : YELLOW_TEXT);

            PdfPTable header = simpleTempleHeader("Shri Trimbakeshwar Jyotirlinga Temple", "Official Donation Receipt  ·  Temple ERP", new Color(20, 83, 45), new Color(15, 118, 110));
            header.setSpacingAfter(8);
            doc.add(header);
            doc.add(statusBanner(statusLabel, stBg, stBorder, stFg));

            String name = safe(d != null ? d.getName() : null, recipientName);
            String receiptNo = safe(d != null ? d.getReceiptNo() : null, d != null ? d.getId() : null);
            String noteText = completed ? "Donation completed. This is your official generated receipt. You can print or save it for your records."
                    : "Receipt details are shown below. Status is not completed.";
            doc.add(identityPanelWithPhoto("DONOR PHOTO", "RECEIPT NUMBER", receiptNo,
                    new String[]{"Donor: " + name,
                            "Email: " + safe(d != null ? d.getEmail() : null),
                            "Phone: " + safe(d != null ? d.getPhone() : null)},
                    noteText + "  Thank you for your contribution to Shri Trimbakeshwar Temple.",
                    profilePicFilename, new Color(240, 253, 244)));

            doc.add(totalHighlight((d != null && d.getDonationType() != null ? d.getDonationType() : "Donation Amount"),
                    "Official temple donation receipt",
                    formatMoney(d != null ? d.getAmount() : null), new Color(20, 83, 45), new Color(15, 118, 110)));

            doc.add(sectionBar("RECEIPT / DONOR DETAILS", new Color(20, 83, 45)));
            PdfPTable table = themedDetailsTable(4);
            addField(table, "Receipt No", receiptNo);
            addField(table, "Donor Name", name);
            addField(table, "Email", d != null ? d.getEmail() : null);
            addField(table, "Phone", d != null ? d.getPhone() : null);
            addField(table, "Amount", formatMoney(d != null ? d.getAmount() : null), completed ? GREEN_DARK : TEXT_DARK, true);
            addField(table, "Donation Type", d != null ? d.getDonationType() : null);
            addField(table, "Payment Method", d != null ? d.getPaymentMethod() : null);
            addField(table, "Payment Status", status, stFg, true);
            addField(table, "Donation Date", formatInstant(d != null && d.getDonationDate() != null ? d.getDonationDate() : (d != null ? d.getCreatedAt() : null)));
            addField(table, "Created At", formatInstant(d != null ? d.getCreatedAt() : null));
            addFieldWide(table, "Notes / Message", d != null ? d.getNotes() : null);
            doc.add(table);

            Paragraph thanks = new Paragraph("Thank you for your donation. Please keep this receipt (PDF/digital/print) for your records.",
                    FontFactory.getFont(FontFactory.HELVETICA, 9.5f, new Color(53, 90, 70)));
            PdfPTable nw = singleCellWrap(thanks, new Color(240, 253, 244), new Color(134, 239, 172), 8);
            nw.setSpacingBefore(8);
            doc.add(nw);
            doc.add(templeFooterNote());
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate donation receipt PDF", e);
        }
    }
    private PdfPTable simpleTempleHeader(String title, String subtitle, Color accent1, Color accent2) {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell band = new PdfPCell(new Phrase("à¥¥ SHRI TRIMBAKESHWAR JYOTIRLINGA TEMPLE à¥¥",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, MAROON_DARK)));
        band.setBackgroundColor(GOLD_BRIGHT);
        band.setBorderColor(SAFFRON);
        band.setHorizontalAlignment(Element.ALIGN_CENTER);
        band.setPadding(5);
        header.addCell(band);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(accent1 != null ? accent1 : MAROON);
        cell.setBorderColor(GOLD_BRIGHT);
        cell.setBorderWidthLeft(1.2f); cell.setBorderWidthRight(1.2f); cell.setBorderWidthTop(0); cell.setBorderWidthBottom(0);
        cell.setPadding(12); cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p1 = new Paragraph(title == null ? "" : title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE));
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(subtitle == null ? "" : subtitle, FontFactory.getFont(FontFactory.HELVETICA, 10.5f, new Color(255, 236, 179)));
        p2.setAlignment(Element.ALIGN_CENTER); p2.setSpacingBefore(3);
        Paragraph p3 = new Paragraph("à¥ à¤¨à¤®à¤ƒ à¤¶à¤¿à¤µà¤¾à¤¯  |  Trimbak, Nashik, Maharashtra", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9.5f, SAFFRON_LIGHT));
        p3.setAlignment(Element.ALIGN_CENTER); p3.setSpacingBefore(4);
        cell.addElement(p1); cell.addElement(p2); cell.addElement(p3);
        header.addCell(cell);
        PdfPCell bottom = new PdfPCell(new Phrase("OFFICIAL TEMPLE ERP DOCUMENT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        bottom.setBackgroundColor(accent2 != null ? accent2 : SAFFRON);
        bottom.setBorderColor(GOLD_BRIGHT);
        bottom.setHorizontalAlignment(Element.ALIGN_CENTER);
        bottom.setPadding(4);
        header.addCell(bottom);
        return header;
    }

    private PdfPTable statusBanner(String label, Color bg, Color border, Color fg) {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(6);
        banner.setSpacingAfter(8);
        PdfPCell bcell = new PdfPCell(new Phrase(label == null ? "-" : label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, fg != null ? fg : TEXT_DARK)));
        bcell.setPadding(9);
        bcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        bcell.setBackgroundColor(bg != null ? bg : CREAM_WARM);
        bcell.setBorderColor(border != null ? border : CREAM_BORDER);
        bcell.setBorderWidth(1.5f);
        banner.addCell(bcell);
        return banner;
    }

    private PdfPTable sectionBar(String title, Color accent) {
        PdfPTable bar = new PdfPTable(1);
        bar.setWidthPercentage(100);
        bar.setSpacingBefore(8);
        bar.setSpacingAfter(5);
        PdfPCell sbc = new PdfPCell(new Phrase("  " + (title == null ? "" : title),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, Color.WHITE)));
        sbc.setBackgroundColor(accent != null ? accent : MAROON);
        sbc.setBorderColor(GOLD_BRIGHT);
        sbc.setBorderWidth(1f);
        sbc.setPadding(6);
        bar.addCell(sbc);
        return bar;
    }

    private PdfPTable themedDetailsTable(int cols) {
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        if (cols == 4) {
            try { table.setWidths(new float[]{22f, 28f, 22f, 28f}); } catch (Exception ignored) {}
        }
        return table;
    }

    private PdfPTable identityPanelWithPhoto(String photoLabel, String idLabel, String idValue, String[] lines, String note,
                                              String profilePicFilename, Color bg) throws Exception {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(6);
        PdfPCell outerCell = new PdfPCell();
        outerCell.setBackgroundColor(bg != null ? bg : new Color(255, 250, 240));
        outerCell.setBorderColor(CREAM_BORDER);
        outerCell.setPadding(10);
        PdfPTable inner = new PdfPTable(new float[]{22f, 78f});
        inner.setWidthPercentage(100);
        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorderColor(GOLD); photoCell.setBackgroundColor(Color.WHITE); photoCell.setPadding(6);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph pl = new Paragraph(photoLabel == null ? "PHOTO" : photoLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, LABEL));
        pl.setAlignment(Element.ALIGN_CENTER); pl.setSpacingAfter(4);
        photoCell.addElement(pl);
        boolean addedImage = false;
        try {
            Path imgPath = resolveProfileImage(profilePicFilename);
            if (imgPath != null && Files.exists(imgPath)) {
                Image img = Image.getInstance(imgPath.toAbsolutePath().toString());
                img.scaleToFit(72, 84); img.setAlignment(Element.ALIGN_CENTER);
                photoCell.addElement(img); addedImage = true;
            }
        } catch (Exception ignored) {}
        if (!addedImage) {
            PdfPTable ph = new PdfPTable(1); ph.setWidthPercentage(90);
            PdfPCell phc = new PdfPCell(new Phrase("No\nPhoto", FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(148, 163, 184))));
            phc.setFixedHeight(78); phc.setHorizontalAlignment(Element.ALIGN_CENTER); phc.setVerticalAlignment(Element.ALIGN_MIDDLE);
            phc.setBackgroundColor(new Color(248, 250, 252)); phc.setBorderColor(CREAM_BORDER);
            ph.addCell(phc); photoCell.addElement(ph);
        }
        inner.addCell(photoCell);
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER); infoCell.setPaddingLeft(10); infoCell.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph idLbl = new Paragraph(idLabel == null ? "ID" : idLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, LABEL));
        Paragraph idVal = new Paragraph(idValue == null ? "-" : idValue, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, TEXT_DARK));
        idVal.setSpacingAfter(4);
        infoCell.addElement(idLbl); infoCell.addElement(idVal);
        if (lines != null) {
            for (String line : lines) {
                if (line != null && !line.isBlank()) {
                    Paragraph lp = new Paragraph(line, FontFactory.getFont(FontFactory.HELVETICA, 9.5f, TEXT_DARK));
                    lp.setSpacingAfter(2); infoCell.addElement(lp);
                }
            }
        }
        if (note != null && !note.isBlank()) {
            Paragraph np = new Paragraph(note, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, new Color(107, 83, 22)));
            np.setSpacingBefore(6);
            PdfPTable nw = singleCellWrap(np, CREAM_WARM, CREAM_BORDER, 6);
            nw.setSpacingBefore(6);
            infoCell.addElement(nw);
        }
        inner.addCell(infoCell);
        outerCell.addElement(inner);
        outer.addCell(outerCell);
        return outer;
    }

    private PdfPTable totalHighlight(String label, String sublabel, String amount, Color c1, Color c2) {
        PdfPTable t = new PdfPTable(new float[]{70f, 30f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(c1 != null ? c1 : MAROON);
        left.setBorderColor(GOLD_BRIGHT);
        left.setPadding(10);
        left.addElement(new Paragraph(label == null ? "Total" : label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(255,236,179))));
        left.addElement(new Paragraph(sublabel == null ? "" : sublabel, FontFactory.getFont(FontFactory.HELVETICA, 9, Color.WHITE)));
        PdfPCell right = new PdfPCell(new Phrase(amount == null ? "-" : amount, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE)));
        right.setBackgroundColor(c2 != null ? c2 : SAFFRON);
        right.setBorderColor(GOLD_BRIGHT);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        right.setPadding(10);
        t.addCell(left); t.addCell(right);
        return t;
    }

    private Paragraph templeFooterNote() {
        Paragraph footer = new Paragraph("à¥ à¤¨à¤®à¤ƒ à¤¶à¤¿à¤µà¤¾à¤¯  |  Shri Trimbakeshwar Jyotirlinga Temple, Trimbak, Nashik",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, GOLD_DARK));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        return footer;
    }

    private void addRow(PdfPTable table, String k, String v, Font label, Font value) {
        PdfPCell c1 = new PdfPCell(new Phrase(k, label));
        c1.setBackgroundColor(new Color(250, 247, 240));
        c1.setPadding(7);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(v == null || v.isBlank() ? "-" : v, value));
        c2.setPadding(7);
        table.addCell(c2);
    }

    private void headerCell(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(new Color(250, 247, 240));
        c.setPadding(6);
        table.addCell(c);
    }

    private String formatMoney(Double v) {
        if (v == null) return "₹0.00";
        return "₹" + MONEY.format(v);
    }

    private String safe(String... vals) {
        if (vals == null) return "-";
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return "-";
    }
}
