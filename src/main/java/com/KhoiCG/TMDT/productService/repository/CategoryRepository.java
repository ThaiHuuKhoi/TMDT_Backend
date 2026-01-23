package com.KhoiCG.TMDT.productService.repository;


import com.KhoiCG.TMDT.productService.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}