package com.melodyshop.user.client;

import com.melodyshop.user.client.fallback.AuthClientFallback;
import com.melodyshop.user.client.fallback.AuthClientFallbackFactory;
import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service", fallbackFactory = AuthClientFallbackFactory.class)
public interface AuthClient {

    @DeleteMapping("/api/auth/revoke-tokens")
    void revokeUserTokens(@RequestParam("userId") String userId);

    @GetMapping("/api/auth/search")
    ApiResponse<PageResponse<UserSearchResult>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    class UserSearchResult {
        public String id;
        public String email;
        public String fullName;
        public String phone;
        public String createdAt;
    }
}
