package com.melodyshop.media.controller;

import com.melodyshop.media.dto.UploadResponse;
import com.melodyshop.media.service.CloudinaryService;
import com.melodyshop.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "product") String type) {

        log.info("Upload request: file='{}', type='{}'", file.getOriginalFilename(), type);
        UploadResponse response = cloudinaryService.upload(file, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UploadResponse>builder()
                .success(true)
                .message("Upload successful")
                .data(response)
                .build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Map<String, String>>> delete(
            @RequestParam("publicId") String publicId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String userRole) {

        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Admin role required")
                            .build());
        }

        cloudinaryService.delete(publicId);
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Delete successful")
                .data(Map.of(
                        "status", "deleted",
                        "publicId", publicId
                ))
                .build());
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> ping() {
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Media service is running")
                .data(Map.of(
                        "service", "media-service",
                        "status", "UP"
                ))
                .build());
    }
}
