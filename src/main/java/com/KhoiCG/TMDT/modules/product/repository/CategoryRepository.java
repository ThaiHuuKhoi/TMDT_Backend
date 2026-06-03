package com.KhoiCG.TMDT.modules.product.repository;


import com.KhoiCG.TMDT.modules.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL")
    List<Category> findAllActive();

    @Query("SELECT c FROM Category c WHERE c.slug = :slug AND c.deletedAt IS NULL")
    Optional<Category> findBySlug(String slug);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.slug = :slug AND c.deletedAt IS NULL AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsBySlugActive(String slug, Long excludeId);
}