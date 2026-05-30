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
public class AiExtractEmbeddingResponse {
    private boolean success;
    private List<Double> embedding;
    private int faceCount;
    private Integer imageWidth;
    private Integer imageHeight;
    private String message;
    private String requestId;
}
