package com.melodyshop.auth.controller;

import com.melodyshop.auth.dto.*;
import com.melodyshop.auth.service.FaceService;
import com.melodyshop.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/face")
@RequiredArgsConstructor
public class FaceAuthController {

    private final FaceService faceService;

    /**
     * Register face for authenticated user.
     * Requires X-User-Id header from Gateway.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<FaceRegisterResponse>> registerFace(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody FaceRegisterRequest request) {
        FaceRegisterResponse response = faceService.registerFace(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Face registered successfully", response));
    }

    /**
     * Login with face only (no password required).
     * Public endpoint - no authentication needed.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<FaceLoginResponse>> loginWithFace(
            @Valid @RequestBody FaceLoginRequest request) {
        FaceLoginResponse response = faceService.loginWithFace(request);
        return ResponseEntity.ok(ApiResponse.ok("Face login successful", response));
    }

    /**
     * Check face registration status for authenticated user.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<FaceStatusResponse>> getFaceStatus(
            @RequestHeader("X-User-Id") String userId) {
        FaceStatusResponse response = faceService.getFaceStatus(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Delete face registration for authenticated user.
     */
    @DeleteMapping("/unregister")
    public ResponseEntity<ApiResponse<Void>> deleteFaceRegistration(
            @RequestHeader("X-User-Id") String userId) {
        faceService.deleteFaceRegistration(userId);
        return ResponseEntity.ok(ApiResponse.ok("Face registration removed", null));
    }
}
