package com.melodyshop.media.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaSeeder implements CommandLineRunner {

    private final Cloudinary cloudinary;

    @Value("#{${media.folders}}")
    private Map<String, String> folders;

    private static final String IMG_SOURCE_PATH = "/app/img-product";

    @Override
    public void run(String... args) {
        File folder = new File(IMG_SOURCE_PATH);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Image source directory {} not found. Skipping auto-upload.", IMG_SOURCE_PATH);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".jpg") || 
            name.toLowerCase().endsWith(".png") || 
            name.toLowerCase().endsWith(".webp")
        );

        if (files == null || files.length == 0) {
            log.info("No images found in {}. Skipping.", IMG_SOURCE_PATH);
            return;
        }

        String targetFolder = folders.getOrDefault("product", "melodyshop/products");
        log.info("Starting automatic upload of {} images to Cloudinary folder: {}", files.length, targetFolder);

        for (File file : files) {
            String fileName = file.getName();
            String publicId = fileName.substring(0, fileName.lastIndexOf('.'));
            
            try {
                // Check if already exists by attempting to get resource info (Admin API)
                // However, simple way: Upload with 'overwrite: false' if using public_id
                // But Cloudinary upload API 'overwrite' defaults to true.
                // We'll use a simple check: if we already uploaded it in this session or just trust Cloudinary to handle it.
                // To be safe and efficient, we use the filename as public_id.
                
                Map<?, ?> result = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "folder", targetFolder,
                    "public_id", publicId,
                    "overwrite", false, // Do not overwrite if exists
                    "unique_filename", false,
                    "use_filename", true
                ));

                log.info("Processed {}: {}", fileName, result.get("secure_url"));
            } catch (IOException e) {
                log.error("Failed to upload {}: {}", fileName, e.getMessage());
            }
        }
        
        log.info("Auto-upload process completed.");
    }
}
