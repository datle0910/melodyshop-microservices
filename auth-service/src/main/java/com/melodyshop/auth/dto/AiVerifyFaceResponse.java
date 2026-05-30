package com.melodyshop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVerifyFaceResponse {
    private boolean success;
    private boolean matched;
    private double similarity;
    private int faceCount;
    private String message;
    private String requestId;
}
