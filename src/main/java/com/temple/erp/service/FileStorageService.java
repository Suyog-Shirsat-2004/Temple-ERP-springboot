package com.temple.erp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    public static final Set<String> ALLOWED_IMAGES = Set.of("jpg", "jpeg", "png", "gif", "webp");
    public static final Set<String> ALLOWED_IMAGES_AND_PDF = Set.of("jpg", "jpeg", "png", "gif", "webp", "pdf");
    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("passes"));
        Files.createDirectories(root.resolve("profile"));
    }

    public String store(MultipartFile file, String subdir, String prefix) throws IOException {
        return store(file, subdir, prefix, ALLOWED_IMAGES);
    }

    public String store(MultipartFile file, String subdir, String prefix, Set<String> allowedExtensions) throws IOException {
        if (file == null || file.isEmpty()) return null;
        Set<String> allowed = (allowedExtensions == null || allowedExtensions.isEmpty()) ? ALLOWED_IMAGES : allowedExtensions;
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot + 1).toLowerCase();
        if (!allowed.contains(ext)) {
            if (allowed.contains("pdf")) {
                throw new IllegalArgumentException("Only image files (jpg, jpeg, png, gif, webp) and PDF are allowed");
            }
            throw new IllegalArgumentException("Only image files (jpg, jpeg, png, gif, webp) are allowed");
        }
        String filename = prefix + "_" + System.currentTimeMillis() + "." + ext;
        Path dir = root.resolve(subdir);
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    public Path getRoot() { return root; }
}
