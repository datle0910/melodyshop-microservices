package com.melodyshop.media.controller;

import com.melodyshop.media.dto.UploadResponse;
import com.melodyshop.media.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final CloudinaryService cloudinaryService;

    /**
     * Upload an image.
     * POST /api/media/upload?type=product|avatar|review
     *
     * Requires JWT authentication (enforced by API Gateway).
     * Accepts: multipart/form-data with field "file"
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "product") String type) {

        log.info("Upload request: file='{}', type='{}'", file.getOriginalFilename(), type);
        UploadResponse response = cloudinaryService.upload(file, type);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an image by Cloudinary public ID.
     * DELETE /api/media/{publicId}  (Admin only — enforced by API Gateway role check)
     *
     * Note: publicId may contain slashes (folder/filename), use ** mapping.
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> delete(
            @RequestParam("publicId") String publicId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {

        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Only admins can delete media assets"));
        }

        cloudinaryService.delete(publicId);
        return ResponseEntity.ok(Map.of("status", "deleted", "publicId", publicId));
    }

    /** Smoke test */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("service", "media-service", "status", "UP"));
    }
}
