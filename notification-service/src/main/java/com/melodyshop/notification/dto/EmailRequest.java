package com.melodyshop.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Recipient email is required")
    private String to;

    @NotBlank(message = "Subject is required")
    private String subject;

    /**
     * Template name (without .html). Must exist in templates/email/
     * Examples: "welcome", "order-confirmed", "otp"
     */
    @NotBlank(message = "Template name is required")
    private String templateName;

    /**
     * Variables to inject into the Thymeleaf template.
     * Keys map to Thymeleaf ${variable} expressions.
     */
    private Map<String, Object> variables;
}
