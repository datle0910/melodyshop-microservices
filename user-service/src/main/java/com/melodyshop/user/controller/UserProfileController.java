package com.melodyshop.user.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.user.dto.UpdateProfileRequest;
import com.melodyshop.user.dto.UserProfileDTO;
import com.melodyshop.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-FullName", required = false) String fullName) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile(userId, fullName)));
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @ModelAttribute @Valid UpdateProfileRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin thành công",
                profileService.createOrUpdateProfile(userId, request, avatar)));
    }
}
