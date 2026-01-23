package com.KhoiCG.TMDT.productService.repository;

import com.KhoiCG.TMDT.productService.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

// Kế thừa JpaSpecificationExecutor để hỗ trợ filter dynamic
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}