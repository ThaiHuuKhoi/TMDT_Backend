package com.KhoiCG.TMDT.modules.product.repository;

import com.KhoiCG.TMDT.modules.product.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    Optional<Attribute> findByNameIgnoreCase(String name);
}