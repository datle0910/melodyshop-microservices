package com.melodyshop.media.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.melodyshop.media.dto.UploadResponse;
import com.melodyshop.media.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${media.allowed-types}")
    private List<String> allowedTypes;

    @Value("#{${media.folders}}")
    private Map<String, String> folders;

    @Override
    public UploadResponse upload(MultipartFile file, String type) {
        validateFile(file);

        String folder = resolveFolder(type);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",          folder,
                            "resource_type",   "image",
                            "transformation",  "q_auto,f_auto",  // auto quality + format
                            "use_filename",    true,
                            "unique_filename", true
                    )
            );

            log.info("Uploaded {} to Cloudinary folder '{}': {}", file.getOriginalFilename(), folder, result.get("public_id"));

            return UploadResponse.builder()
                    .url(result.get("secure_url").toString())
                    .publicId(result.get("public_id").toString())
                    .format(result.get("format").toString())
                    .bytes(Long.parseLong(result.get("bytes").toString()))
                    .width(Integer.parseInt(result.get("width").toString()))
                    .height(Integer.parseInt(result.get("height").toString()))
                    .folder(folder)
                    .build();

        } catch (IOException e) {
            log.error("Cloudinary upload failed for file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Upload to Cloudinary failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String outcome = result.get("result").toString();

            if ("ok".equals(outcome)) {
                log.info("Deleted Cloudinary asset: {}", publicId);
            } else {
                log.warn("Cloudinary delete returned '{}' for publicId: {}", outcome, publicId);
            }
        } catch (IOException e) {
            log.error("Cloudinary delete failed for publicId '{}': {}", publicId, e.getMessage(), e);
            throw new RuntimeException("Delete from Cloudinary failed: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException(
                "Unsupported file type: " + contentType +
                ". Allowed: " + allowedTypes
            );
        }
    }

    private String resolveFolder(String type) {
        if (type == null || !folders.containsKey(type)) {
            return folders.getOrDefault("product", "melodyshop/products");
        }
        return folders.get(type);
    }
}
