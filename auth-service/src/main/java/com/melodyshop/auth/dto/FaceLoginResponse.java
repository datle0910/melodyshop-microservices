package com.melodyshop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceLoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String userId;
    private String email;
    private String fullName;
    private Object roles;
    private Boolean isMatched;
    private Double similarity;
}
