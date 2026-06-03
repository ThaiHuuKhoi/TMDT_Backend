package com.KhoiCG.TMDT.modules.product.enums;

import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
public enum ProductSortType {

    DEFAULT("default", false, Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC("asc",   true,  null),
    PRICE_DESC("desc", true,  null),
    OLDEST("oldest",   false, Sort.by(Sort.Direction.ASC, "createdAt"));

    private final String code;
    private final boolean priceBased;
    /** null for price-based sorts — ordering is applied via CriteriaQuery instead */
    private final Sort sort;

    ProductSortType(String code, boolean priceBased, Sort sort) {
        this.code = code;
        this.priceBased = priceBased;
        this.sort = sort;
    }

    public static ProductSortType fromCode(String code) {
        if (code == null || code.isBlank()) return DEFAULT;
        for (ProductSortType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        return DEFAULT;
    }
}
