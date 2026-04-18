package com.melodyshop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150)
    private String fullName;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String avatarUrl;
}
