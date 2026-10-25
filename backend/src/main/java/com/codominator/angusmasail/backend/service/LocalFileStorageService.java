package com.codominator.angusmasail.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "mp3", "m4a", "wav", "webp"
    );

    public String storeFile(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String rawFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String cleanName = StringUtils.cleanPath(rawFilename);
        
        // Strip out any directory path components to avoid directory traversal
        if (cleanName.contains("/") || cleanName.contains("\\")) {
            cleanName = Paths.get(cleanName).getFileName().toString();
        }
        
        // Remove potentially dangerous characters
        cleanName = cleanName.replaceAll("[^a-zA-Z0-9._-]", "_");

        String extension = "";
        int i = cleanName.lastIndexOf('.');
        if (i > 0) {
            extension = cleanName.substring(i + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension: " + extension + ". Allowed: " + ALLOWED_EXTENSIONS);
        }

        String uniqueFilename = UUID.randomUUID() + "_" + cleanName;
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(subFolder).normalize();

        // Enforce that target directory remains within the configured upload root
        if (!targetDir.startsWith(baseDir)) {
            throw new SecurityException("Unauthorized target directory traversal attempt");
        }

        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(uniqueFilename).normalize();
            if (!targetPath.startsWith(baseDir)) {
                throw new SecurityException("Unauthorized file path traversal attempt");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + subFolder + "/" + uniqueFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + uniqueFilename + ". Please try again!", ex);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            String relative = fileUrl.replaceFirst("^/uploads/", "");
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetPath = baseDir.resolve(relative).normalize();

            // Strict check: never delete anything outside baseDir
            if (targetPath.startsWith(baseDir)) {
                Files.deleteIfExists(targetPath);
            }
        } catch (Exception ignored) {}
    }
}
