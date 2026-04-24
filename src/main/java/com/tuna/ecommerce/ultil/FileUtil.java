package com.tuna.ecommerce.ultil;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileUtil {
    private static final String TEMP_DIR = "temp-uploads";

    public static List<String> saveTempFiles(List<MultipartFile> files) throws IOException {
        Path uploadPath = Paths.get(TEMP_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<String> filePaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String originalName = file.getOriginalFilename();
                String extension = "";
                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(originalName.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString() + extension;
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath);
                filePaths.add(filePath.toAbsolutePath().toString());
            }
        }
        return filePaths;
    }
}
