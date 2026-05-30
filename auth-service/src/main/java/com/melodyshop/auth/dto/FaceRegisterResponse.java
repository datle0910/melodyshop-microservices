package com.melodyshop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceRegisterResponse {
    private String userId;
    private String message;
    private Boolean isRegistered;
}
