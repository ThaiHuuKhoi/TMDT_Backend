package com.KhoiCG.TMDT.modules.product.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductCreateRequest {
    private String name;
    private Long price;
    private String description;
    private String slug;
    private Long categoryId;
    private List<String> colors;
    private Map<String, String> images;
    private List<String> sizes;
}