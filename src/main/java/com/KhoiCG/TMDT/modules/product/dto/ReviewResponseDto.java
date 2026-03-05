package com.KhoiCG.TMDT.modules.product.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReviewResponseDto {
    private Long id;
    private String userName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}