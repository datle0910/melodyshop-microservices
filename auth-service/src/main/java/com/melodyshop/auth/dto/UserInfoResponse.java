package com.melodyshop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Boolean isActive;
    private Boolean isVerified;
    private Integer loyaltyPoints;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
