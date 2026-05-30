package com.melodyshop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    @Email(message = "Invalid email format")
    @NotBlank(message = "Recipient email is required")
    private String to;

    private String recipientName;

    private String otp;
}
