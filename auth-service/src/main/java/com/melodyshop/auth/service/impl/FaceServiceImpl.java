package com.melodyshop.auth.service.impl;

import com.melodyshop.auth.client.FaceRecognitionClient;
import com.melodyshop.auth.config.JwtTokenProvider;
import com.melodyshop.auth.dto.*;
import com.melodyshop.auth.entity.FaceData;
import com.melodyshop.auth.entity.RefreshToken;
import com.melodyshop.auth.entity.Role;
import com.melodyshop.auth.entity.User;
import com.melodyshop.auth.repository.FaceDataRepository;
import com.melodyshop.auth.repository.RefreshTokenRepository;
import com.melodyshop.auth.repository.UserRepository;
import com.melodyshop.auth.service.FaceService;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceServiceImpl implements FaceService {

    private final FaceDataRepository faceDataRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FaceRecognitionClient faceRecognitionClient;
    private final JwtTokenProvider jwtTokenProvider;

    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.6;

    @Override
    @Transactional
    public FaceRegisterResponse registerFace(String userId, FaceRegisterRequest request) {
        if (request.getImage() == null || request.getImage().isBlank()) {
            throw new BadRequestException("Image data is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Cannot register face for inactive account");
        }

        Map<String, Object> extractRequest = new HashMap<>();
        extractRequest.put("image", request.getImage());
        extractRequest.put("validate_single_face", true);

        AiExtractEmbeddingResponse aiResponse = faceRecognitionClient.extractEmbedding(extractRequest);

        if (!aiResponse.isSuccess()) {
            throw new BadRequestException(aiResponse.getMessage());
        }

        if (aiResponse.getEmbedding() == null || aiResponse.getEmbedding().isEmpty()) {
            throw new BadRequestException(
                    "Failed to extract face embedding. Please try again with a clearer photo.");
        }

        faceDataRepository.findByUserId(userId).ifPresent(faceDataRepository::delete);

        FaceData faceData = FaceData.builder()
                .userId(userId)
                .embedding(aiResponse.getEmbedding())
                .qualityScore(request.getQualityScore())
                .isActive(true)
                .build();
        faceDataRepository.save(faceData);

        log.info("Face registered successfully for user: {} ({})", userId, user.getEmail());

        return FaceRegisterResponse.builder()
                .userId(userId)
                .message("Face registered successfully")
                .isRegistered(true)
                .build();
    }

    @Override
    @Transactional
    public FaceLoginResponse loginWithFace(FaceLoginRequest request) {
        if (request.getImage() == null || request.getImage().isBlank()) {
            throw new BadRequestException("Image data is required");
        }

        List<User> allUsers = userRepository.findAll();
        User matchedUser = null;
        double bestSimilarity = 0.0;

        for (User user : allUsers) {
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                continue;
            }

            Optional<FaceData> faceDataOpt = faceDataRepository.findByUserIdAndIsActiveTrue(user.getId());
            if (faceDataOpt.isEmpty()) {
                continue;
            }

            FaceData storedFace = faceDataOpt.get();
            List<Double> storedEmbedding = storedFace.getEmbedding();

            Map<String, Object> compareRequest = new HashMap<>();
            compareRequest.put("image", request.getImage());
            compareRequest.put("stored_embedding", storedEmbedding);
            compareRequest.put("similarity_threshold", 0.0);

            AiVerifyFaceResponse compareResult = faceRecognitionClient.verifyFace(compareRequest);

            log.info("Face login compare: userId={}, success={}, matched={}, similarity={}",
                    user.getId(), compareResult.isSuccess(), compareResult.isMatched(), compareResult.getSimilarity());

            if (compareResult.isSuccess() && compareResult.isMatched()) {
                if (compareResult.getSimilarity() > bestSimilarity) {
                    bestSimilarity = compareResult.getSimilarity();
                    matchedUser = user;
                }
            }
        }

        if (matchedUser == null) {
            log.warn("Face login failed: no matching face found. Best similarity was: {}", bestSimilarity);
            throw new BadRequestException(
                    "Face does not match any registered account. Please use email/password login, "
                    + "or register your face again with a clearer photo.");
        }

        Set<String> roleNames = matchedUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String primaryRole = roleNames.contains("ROLE_ADMIN")
                ? "ROLE_ADMIN"
                : roleNames.stream().findFirst().orElse("ROLE_CUSTOMER");

        String accessToken = jwtTokenProvider.generateAccessToken(
                matchedUser.getId(),
                matchedUser.getEmail(),
                matchedUser.getFullName(),
                matchedUser.getPhone(),
                primaryRole
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(matchedUser.getId());

        refreshTokenRepository.deleteByUserId(matchedUser.getId());

        RefreshToken storedRefresh = new RefreshToken();
        storedRefresh.setUserId(matchedUser.getId());
        storedRefresh.setTokenHash(hashToken(refreshToken));
        storedRefresh.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(storedRefresh);

        log.info("Face login successful for user: {} ({})", matchedUser.getId(), matchedUser.getEmail());

        return FaceLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(matchedUser.getId())
                .email(matchedUser.getEmail())
                .fullName(matchedUser.getFullName())
                .roles(roleNames)
                .isMatched(true)
                .similarity(Math.round(bestSimilarity * 1000.0) / 1000.0)
                .build();
    }

    @Override
    public FaceStatusResponse getFaceStatus(String userId) {
        Optional<FaceData> faceDataOpt = faceDataRepository.findByUserId(userId);

        if (faceDataOpt.isEmpty()) {
            return FaceStatusResponse.builder()
                    .userId(userId)
                    .isRegistered(false)
                    .message("Face not registered. You can register your face for quick login.")
                    .build();
        }

        FaceData faceData = faceDataOpt.get();
        boolean isActive = Boolean.TRUE.equals(faceData.getIsActive());
        return FaceStatusResponse.builder()
                .userId(userId)
                .isRegistered(isActive)
                .message(isActive
                        ? "Face is registered and active. You can use face login."
                        : "Face registration is inactive.")
                .qualityScore(faceData.getQualityScore())
                .registeredAt(faceData.getCreatedAt() != null
                        ? faceData.getCreatedAt().toString()
                        : null)
                .build();
    }

    @Override
    @Transactional
    public void deleteFaceRegistration(String userId) {
        faceDataRepository.findByUserId(userId).ifPresent(faceData -> {
            faceData.setIsActive(false);
            faceDataRepository.save(faceData);
            log.info("Face registration deactivated for user: {}", userId);
        });
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
