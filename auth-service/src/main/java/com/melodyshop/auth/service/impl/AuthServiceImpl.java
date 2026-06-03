package com.melodyshop.auth.service.impl;

import com.melodyshop.auth.config.JwtTokenProvider;
import com.melodyshop.auth.dto.*;
import com.melodyshop.auth.entity.RefreshToken;
import com.melodyshop.auth.entity.Role;
import com.melodyshop.auth.entity.User;
import com.melodyshop.auth.entity.VerificationCode;
import com.melodyshop.auth.repository.RefreshTokenRepository;
import com.melodyshop.auth.repository.RoleRepository;
import com.melodyshop.auth.repository.UserRepository;
import com.melodyshop.auth.repository.VerificationCodeRepository;
import com.melodyshop.auth.service.AuthService;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.auth.client.NotificationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final NotificationServiceClient notificationServiceClient;

    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 10;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;

    @Override
    @Transactional
    public void sendVerificationCode(String email, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng: " + email);
        }

        // Mark all existing codes for this email/purpose as used
        verificationCodeRepository.markAllAsUsed(email, "REGISTRATION");

        // Generate 6-digit code
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));

        // Save to database
        VerificationCode verificationCode = VerificationCode.builder()
                .email(email)
                .code(code)
                .purpose("REGISTRATION")
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES))
                .isUsed(false)
                .isVerified(false)
                .build();
        verificationCodeRepository.save(verificationCode);

        // Send OTP email via NotificationService
        try {
            var result = notificationServiceClient.sendOtp(OtpRequest.builder()
                    .to(email)
                    .recipientName(fullName)
                    .otp(code)
                    .build());
            if (!result.isSuccess() && "MAIL_SERVICE_UNAVAILABLE".equals(result.getMessage())) {
                // Notification service is down - allow registration anyway, log the code
                log.warn("Notification service unavailable. Registration code for {}: {}", email, code);
            } else {
                log.info("Verification code sent to {}", email);
            }
        } catch (Exception e) {
            // Notification service unreachable - allow registration anyway, log the code
            log.warn("Failed to send verification email to {}: {}. Code: {}", email, e.getMessage(), code);
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Verify the code
        if (request.getVerificationCode() == null || request.getVerificationCode().isBlank()) {
            throw new BadRequestException("Mã xác nhận không được để trống");
        }

        VerificationCode verification = verificationCodeRepository
                .findTopByEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(request.getEmail(), "REGISTRATION")
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã xác nhận. Vui lòng yêu cầu gửi lại mã."));

        if (verification.isExpired()) {
            throw new BadRequestException("Mã xác nhận đã hết hạn. Vui lòng yêu cầu gửi lại mã.");
        }

        if (!verification.getCode().equals(request.getVerificationCode().trim())) {
            throw new BadRequestException("Mã xác nhận không đúng. Vui lòng kiểm tra lại.");
        }

        // Mark code as used
        verification.setIsUsed(true);
        verification.setIsVerified(true);
        verificationCodeRepository.save(verification);

        // Check email uniqueness (in case race condition)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng: " + request.getEmail());
        }

        // Find ROLE_CUSTOMER
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_CUSTOMER"));

        // Create user (verified now)
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .isActive(true)
                .isVerified(true)
                .loyaltyPoints(0)
                .build();
        user.getRoles().add(customerRole);

        user = userRepository.save(user);

        // Send welcome email
        try {
            notificationServiceClient.sendWelcomeEmail(WelcomeRequest.builder()
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .build());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }

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
    @Transactional
    public void revokeUserTokens(String userId) {
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

    @Override
    public Page<UserSearchDTO> searchUsers(String keyword, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(keyword, pageable);
        return users.map(user -> UserSearchDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build());
    }

    @Override
    @Transactional
    public void requestForgotPasswordOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại với email này"));

        // Mark all existing codes for this email/purpose as used
        verificationCodeRepository.markAllAsUsed(email, "FORGOT_PASSWORD");

        // Generate 6-digit code
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));

        // Save to database
        VerificationCode verificationCode = VerificationCode.builder()
                .email(email)
                .code(code)
                .purpose("FORGOT_PASSWORD")
                .expiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES))
                .isUsed(false)
                .isVerified(false)
                .build();
        verificationCodeRepository.save(verificationCode);

        // Send OTP email
        try {
            var result = notificationServiceClient.sendOtp(OtpRequest.builder()
                    .to(email)
                    .recipientName(user.getFullName())
                    .otp(code)
                    .build());
            if (result == null || !result.isSuccess()) {
                throw new FeignClientException(502, "Không thể gửi email lúc này. Vui lòng thử lại.");
            }
            log.info("Forgot password OTP email sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send forgot password email to {}", email, e);
            throw new FeignClientException(502, "Không thể gửi email lúc này. Vui lòng thử lại.");
        }
    }

    @Override
    @Transactional
    public String verifyForgotPasswordOtp(String email, String otp) {
        VerificationCode verification = verificationCodeRepository
                .findTopByEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(email, "FORGOT_PASSWORD")
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã xác nhận. Vui lòng yêu cầu lại."));

        if (verification.isExpired()) {
            throw new BadRequestException("Mã xác nhận đã hết hạn. Vui lòng yêu cầu lại.");
        }

        if (!verification.getCode().equals(otp.trim())) {
            throw new BadRequestException("Mã xác nhận không đúng. Vui lòng kiểm tra lại.");
        }

        // Generate 6-char reset token to fit the DB 'code' column length (which is 6)
        String resetToken = java.util.UUID.randomUUID().toString().substring(0, 6);
        
        verification.setCode(resetToken);
        verification.setIsVerified(true);
        // Do NOT set isUsed to true yet, we need it for the final step.
        // It will expire based on the original expiration time (or we can extend it).
        verificationCodeRepository.save(verification);

        return resetToken;
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        VerificationCode verification = verificationCodeRepository
                .findTopByCodeAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(resetToken, "FORGOT_PASSWORD")
                .orElseThrow(() -> new BadRequestException("Token đổi mật khẩu không hợp lệ hoặc đã hết hạn"));

        if (verification.isExpired()) {
            throw new BadRequestException("Token đổi mật khẩu đã hết hạn. Vui lòng thực hiện lại từ đầu.");
        }

        User user = userRepository.findByEmail(verification.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        verification.setIsUsed(true);
        verificationCodeRepository.save(verification);
        
        // Revoke all existing sessions to force login again
        revokeUserTokens(user.getId());
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
                user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), primaryRole);

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
