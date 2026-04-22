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
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
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

    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
