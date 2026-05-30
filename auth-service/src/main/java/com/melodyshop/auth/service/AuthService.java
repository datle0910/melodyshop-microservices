package com.melodyshop.auth.service;

import com.melodyshop.auth.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthService {
    void sendVerificationCode(String email, String fullName);
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(TokenRefreshRequest request);
    void logout(String userId);
    void revokeUserTokens(String userId);
    UserInfoResponse getUserInfo(String userId);
    Page<UserSearchDTO> searchUsers(String keyword, Pageable pageable);
}
