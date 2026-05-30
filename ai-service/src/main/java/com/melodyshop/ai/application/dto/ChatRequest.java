package com.melodyshop.ai.application.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String userId;
    private String message;
}
