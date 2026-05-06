package com.tuna.ecommerce.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Service
@AllArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public Map uploadFile(MultipartFile file) throws IOException {
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "fetch_format", "auto",
                "quality", "auto"));
    }

    public Map uploadFile(java.io.File file) throws IOException {
        return cloudinary.uploader().upload(file, ObjectUtils.asMap(
                "resource_type", "auto",
                "fetch_format", "auto",
                "quality", "auto"));
    }

    public java.util.List<Map> uploadFiles(java.util.List<MultipartFile> files) {
        java.util.List<java.util.concurrent.CompletableFuture<Map>> futures = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return this.uploadFile(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload file to Cloudinary", e);
                    }
                }))
                .collect(java.util.stream.Collectors.toList());

        return futures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList());
    }

    public void deleteFile(String publicId) {
        if (publicId == null || publicId.trim().isEmpty()) {
            return;
        }
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception e) {
                System.err.println(">>> Background Cloudinary Delete Error: " + e.getMessage());
            }
        });
    }
}
