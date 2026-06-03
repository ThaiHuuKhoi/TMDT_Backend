package com.KhoiCG.TMDT.modules.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderBulkIdsRequest {
    @NotEmpty(message = "ids không được rỗng")
    private List<Long> ids;
}
