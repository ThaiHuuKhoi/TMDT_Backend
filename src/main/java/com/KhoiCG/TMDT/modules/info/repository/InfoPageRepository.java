package com.KhoiCG.TMDT.modules.info.repository;

import com.KhoiCG.TMDT.modules.info.entity.InfoPageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InfoPageRepository extends JpaRepository<InfoPageEntity, Long> {
    Optional<InfoPageEntity> findBySlug(String slug);
}
