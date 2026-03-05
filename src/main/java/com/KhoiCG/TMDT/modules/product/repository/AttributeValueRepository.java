package com.KhoiCG.TMDT.modules.product.repository;

import com.KhoiCG.TMDT.modules.product.entity.AttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {
    Optional<AttributeValue> findByAttributeIdAndValueIgnoreCase(Long attributeId, String value);
}