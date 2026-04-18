package com.melodyshop.auth.service.impl;

import com.melodyshop.auth.config.JwtTokenProvider;
import com.melodyshop.auth.dto.*;
import com.melodyshop.auth.entity.RefreshToken;
import com.melodyshop.auth.entity.Role;
import com.melodyshop.auth.entity.User;
import com.melodyshop.auth.repository.RefreshTokenRepository;
import com.melodyshop.auth.repository.RoleRepository;
import com.melodyshop.auth.repository.UserRepository;
import com.melodyshop.auth.service.AuthService;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng: " + request.getEmail());
        }

        // Find ROLE_CUSTOMER
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_CUSTOMER"));

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .isActive(true)
                .isVerified(false)
                .loyaltyPoints(0)
                .build();
        user.getRoles().add(customerRole);

        user = userRepository.save(user);

        // Generate tokens
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không đúng"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Email hoặc mật khẩu không đúng");
        }

        // Check active
        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ"));

        if (storedToken.getIsRevoked()) {
            throw new BadRequestException("Refresh token đã bị thu hồi");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token đã hết hạn");
        }

        // Revoke old token
        storedToken.setIsRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Find user and generate new tokens
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", storedToken.getUserId()));

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    public UserInfoResponse getUserInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .isVerified(user.getIsVerified())
                .loyaltyPoints(user.getLoyaltyPoints())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }

    // ===== Private helpers =====

    private AuthResponse generateAuthResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String primaryRole = roleNames.stream()
                .filter(r -> r.equals("ROLE_ADMIN"))
                .findFirst()
                .orElse(roleNames.iterator().next());

        // Generate access token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getFullName(), primaryRole);

        // Generate refresh token
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Store refresh token hash
        RefreshToken storedRefresh = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(refreshToken))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(storedRefresh);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roleNames)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
