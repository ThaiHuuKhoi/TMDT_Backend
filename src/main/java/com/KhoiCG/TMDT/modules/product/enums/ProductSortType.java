package com.KhoiCG.TMDT.modules.product.enums;

import org.springframework.data.domain.Sort;
import lombok.Getter;

@Getter
public enum ProductSortType {

    DEFAULT("default", Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC("asc", Sort.by(Sort.Direction.ASC, "price")),
    PRICE_DESC("desc", Sort.by(Sort.Direction.DESC, "price")),
    OLDEST("oldest", Sort.by(Sort.Direction.ASC, "createdAt"));

    private final String code;
    private final Sort sort;

    ProductSortType(String code, Sort sort) {
        this.code = code;
        this.sort = sort;
    }

    // Hàm tiện ích để map từ String gửi lên sang Sort
    public static Sort getSortStrategy(String code) {
        if (code == null || code.trim().isEmpty()) {
            return DEFAULT.getSort();
        }

        for (ProductSortType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type.getSort();
            }
        }
        return DEFAULT.getSort();
    }
}