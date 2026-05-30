package com.melodyshop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceStatusResponse {
    private String userId;
    private Boolean isRegistered;
    private String message;
    private Integer qualityScore;
    private String registeredAt;
}
