package com.KhoiCG.TMDT.modules.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShippingDetailsRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 120)
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{7,15}$", message = "Số điện thoại chỉ gồm 7–15 chữ số")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500)
    private String address;

    @NotBlank(message = "Thành phố / tỉnh không được để trống")
    @Size(max = 120)
    private String city;

    public String toFormattedAddress() {
        return "Người nhận: " + name + "\n"
                + "Email: " + email + "\n"
                + "Điện thoại: " + phone + "\n"
                + "Địa chỉ: " + address + "\n"
                + "Tỉnh/Thành: " + city;
    }
}
