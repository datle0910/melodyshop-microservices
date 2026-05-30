package com.melodyshop.user.client.fallback;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.user.client.AuthClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthClientFallback implements AuthClient {

    @Override
    public void revokeUserTokens(String userId) {
        log.warn("Fallback: Token revocation skipped for user {} (service unavailable)", userId);
    }

    @Override
    public ApiResponse<PageResponse<AuthClient.UserSearchResult>> searchUsers(String keyword, int page, int size) {
        log.warn("Fallback: searchUsers skipped (service unavailable)");
        return null;
    }
}
