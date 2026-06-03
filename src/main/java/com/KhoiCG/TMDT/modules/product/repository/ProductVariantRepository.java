package com.KhoiCG.TMDT.modules.product.repository;

import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    boolean existsBySku(String sku);

    @Query("SELECT v.sku FROM ProductVariant v WHERE v.sku IN :skus")
    List<String> findExistingSkus(Collection<String> skus);
}