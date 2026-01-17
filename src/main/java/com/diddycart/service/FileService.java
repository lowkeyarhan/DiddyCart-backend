package com.diddycart.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    // Upload Image File
    public String uploadImage(MultipartFile file) throws IOException {
        // Create directory if not exists
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate unique filename
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        // Save file
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.write(filePath, file.getBytes());

        // Return relative path for database
        return "/uploads/" + fileName;
    }

    // Remove Image File (for vendor/admin updating products)
    public void removeImage(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        String fileName = imageUrl.replace("/uploads/", "");
        Path filePath = Paths.get(UPLOAD_DIR + fileName);

        // Delete file if it exists
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }
}