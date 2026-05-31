package com.melodyshop.user.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.user.client.AuthClient;
import com.melodyshop.user.client.OrderClient;
import com.melodyshop.user.dto.UpdateProfileRequest;
import com.melodyshop.user.dto.UserProfileDTO;
import com.melodyshop.user.entity.UserProfile;
import com.melodyshop.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.melodyshop.user.client.MediaServiceClient;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final MediaServiceClient mediaServiceClient;
    private final OrderClient orderClient;
    private final AuthClient authClient;

    public UserProfileDTO getProfile(String userId, String fullName, String phone) {
        UserProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setId(userId);
                    p.setFullName(fullName);
                    p.setPhone(phone);
                    return profileRepository.save(p);
                });
        return toDTO(profile);
    }

    @Transactional
    public UserProfileDTO createOrUpdateProfile(String userId, UpdateProfileRequest request, MultipartFile avatar) {
        UserProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setId(userId);
                    return p;
                });

        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());

        if (avatar != null && !avatar.isEmpty()) {
            ApiResponse<Map<String, Object>> uploadResponse = mediaServiceClient.uploadFile("avatar", avatar);
            if (uploadResponse != null && uploadResponse.isSuccess() && uploadResponse.getData() != null) {
                Map<String, Object> uploadResult = uploadResponse.getData();
                if (uploadResult.containsKey("url")) {
                    profile.setAvatarUrl((String) uploadResult.get("url"));
                }
            }
        } else if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }

        profile = profileRepository.save(profile);
        return toDTO(profile);
    }

    // --- Admin methods ---

    public Page<UserProfileDTO> getAllProfiles(Pageable pageable) {
        Page<UserProfile> profiles = profileRepository.findAll(pageable);
        return enrichWithAuthData(profiles, pageable);
    }

    public Page<UserProfileDTO> searchProfiles(String keyword, Pageable pageable) {
        try {
            var authResponse = authClient.searchUsers(keyword, pageable.getPageNumber(), pageable.getPageSize());
            if (authResponse == null || authResponse.getData() == null) {
                return Page.empty(pageable);
            }

            List<AuthClient.UserSearchResult> authList = authResponse.getData().getContent();
            if (authList == null || authList.isEmpty()) {
                return Page.empty(pageable);
            }

            List<String> userIds = authList.stream()
                    .map(u -> u.id)
                    .collect(Collectors.toList());

            Map<String, AuthClient.UserSearchResult> authMap = authList.stream()
                    .collect(Collectors.toMap(u -> u.id, u -> u));

            List<UserProfile> profiles = profileRepository.findAllById(userIds);
            Map<String, UserProfile> profileMap = profiles.stream()
                    .collect(Collectors.toMap(p -> p.getId(), p -> p));

            List<UserProfileDTO> dtos = userIds.stream().map(id -> {
                UserProfileDTO dto = new UserProfileDTO();
                dto.setId(id);
                AuthClient.UserSearchResult auth = authMap.get(id);
                if (auth != null) {
                    dto.setEmail(auth.email);
                    dto.setFullName(auth.fullName);
                    if (auth.createdAt != null) {
                        try {
                            dto.setCreatedAt(LocalDateTime.parse(auth.createdAt));
                        } catch (Exception e) {
                            dto.setCreatedAt(null);
                        }
                    }
                }
                UserProfile profile = profileMap.get(id);
                if (profile != null) {
                    dto.setPhone(profile.getPhone());
                    dto.setAvatarUrl(profile.getAvatarUrl());
                    dto.setUpdatedAt(profile.getUpdatedAt());
                }
                return dto;
            }).collect(Collectors.toList());

            long total = authResponse.getData().getTotalElements();
            return new PageImpl<>(dtos, pageable, total);
        } catch (Exception e) {
            return Page.empty(pageable);
        }
    }

    public UserProfileDTO getProfileById(String id) {
        UserProfile profile = profileRepository.findById(id).orElse(null);
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(id);
        if (profile != null) {
            dto.setFullName(profile.getFullName());
            dto.setPhone(profile.getPhone());
            dto.setAvatarUrl(profile.getAvatarUrl());
            dto.setCreatedAt(profile.getCreatedAt());
            dto.setUpdatedAt(profile.getUpdatedAt());
        }
        return dto;
    }

    @Transactional
    public void deleteUser(String userId) {
        ApiResponse<Boolean> orderResponse = orderClient.hasOrdersByUserId(userId);
        boolean hasOrders = orderResponse != null && Boolean.TRUE.equals(orderResponse.getData());
        if (hasOrders) {
            throw new BadRequestException("Khong the xoa tai khoan: nguoi dung da co don hang trong he thong");
        }

        try {
            authClient.revokeUserTokens(userId);
        } catch (Exception e) {
            // Log but continue
        }

        profileRepository.deleteById(userId);
    }

    private Page<UserProfileDTO> enrichWithAuthData(Page<UserProfile> profiles, Pageable pageable) {
        if (profiles.getContent().isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<String> ids = profiles.getContent().stream()
                .map(p -> p.getId())
                .collect(Collectors.toList());

        try {
            var authResponse = authClient.searchUsers(null, 0, ids.size());
            if (authResponse != null && authResponse.getData() != null) {
                Map<String, AuthClient.UserSearchResult> authMap =
                        authResponse.getData().getContent().stream()
                                .collect(Collectors.toMap(u -> u.id, u -> u));

                List<UserProfileDTO> dtos = profiles.getContent().stream()
                        .map(p -> {
                            UserProfileDTO dto = toDTO(p);
                            AuthClient.UserSearchResult auth = authMap.get(p.getId());
                            if (auth != null) {
                                dto.setEmail(auth.email);
                                if (auth.createdAt != null) {
                                    try {
                                        dto.setCreatedAt(LocalDateTime.parse(auth.createdAt));
                                    } catch (Exception e) {
                                        // ignore
                                    }
                                }
                            }
                            return dto;
                        })
                        .collect(Collectors.toList());

                return new PageImpl<>(dtos, pageable, profiles.getTotalElements());
            }
        } catch (Exception e) {
            // If auth call fails, return profiles without email
        }

        return profiles.map(this::toDTO);
    }

    private UserProfileDTO toDTO(UserProfile p) {
        return UserProfileDTO.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .phone(p.getPhone())
                .avatarUrl(p.getAvatarUrl())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
