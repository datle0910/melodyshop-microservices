package com.melodyshop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AddressDTO {
    private String id;

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 150)
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressDetail;

    private Boolean isDefault;
}
