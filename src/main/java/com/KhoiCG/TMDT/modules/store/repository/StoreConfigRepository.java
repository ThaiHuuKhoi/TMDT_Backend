package com.KhoiCG.TMDT.modules.store.repository;

import com.KhoiCG.TMDT.modules.store.entity.StoreConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreConfigRepository extends JpaRepository<StoreConfig, Long> {
}
