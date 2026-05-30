package com.melodyshop.user.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.user.dto.UpdateProfileRequest;
import com.melodyshop.user.dto.UserProfileDTO;
import com.melodyshop.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserProfileDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<UserProfileDTO> result;
        if (keyword != null && !keyword.isBlank()) {
            result = profileService.searchProfiles(keyword, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        } else {
            result = profileService.getAllProfiles(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        }

        PageResponse<UserProfileDTO> pageResponse = PageResponse.<UserProfileDTO>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfileById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateUser(
            @PathVariable String id,
            @ModelAttribute UpdateProfileRequest request,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        UserProfileDTO updated = profileService.createOrUpdateProfile(id, request, avatar);
        return ResponseEntity.ok(ApiResponse.ok("Cap nhat tai khoan thanh cong", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @RequestHeader("X-User-Id") String adminId,
            @PathVariable String id) {
        profileService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("Xoa tai khoan thanh cong", null));
    }
}