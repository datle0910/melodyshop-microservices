package com.melodyshop.auth.service;

import com.melodyshop.auth.dto.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(TokenRefreshRequest request);
    void logout(String userId);
    UserInfoResponse getUserInfo(String userId);
}
