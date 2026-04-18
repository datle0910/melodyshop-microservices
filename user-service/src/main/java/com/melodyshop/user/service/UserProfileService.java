package com.melodyshop.user.service;

import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.user.dto.UpdateProfileRequest;
import com.melodyshop.user.dto.UserProfileDTO;
import com.melodyshop.user.entity.UserProfile;
import com.melodyshop.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepository;

    public UserProfileDTO getProfile(String userId, String fullName) {
        UserProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setId(userId);
                    p.setFullName(fullName);
                    return profileRepository.save(p);
                });
        return toDTO(profile);
    }

    @Transactional
    public UserProfileDTO createOrUpdateProfile(String userId, UpdateProfileRequest request) {
        UserProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setId(userId); // Override auto-gen, use auth user id
                    return p;
                });

        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setAvatarUrl(request.getAvatarUrl());

        profile = profileRepository.save(profile);
        return toDTO(profile);
    }

    // --- Admin methods ---

    public Page<UserProfileDTO> getAllProfiles(Pageable pageable) {
        return profileRepository.findAll(pageable).map(this::toDTO);
    }

    public UserProfileDTO getProfileById(String id) {
        return getProfile(id, null);
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
