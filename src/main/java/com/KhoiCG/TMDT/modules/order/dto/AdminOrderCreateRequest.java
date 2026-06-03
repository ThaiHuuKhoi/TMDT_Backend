package com.KhoiCG.TMDT.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AdminOrderCreateRequest {
    @NotNull(message = "userId là bắt buộc")
    private Long userId;

    @NotEmpty(message = "Danh sách sản phẩm không được rỗng")
    @Valid
    private List<AdminOrderLineRequest> lines;

    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String couponCode;
}
