package com.KhoiCG.TMDT.modules.marketing.repository;

import com.KhoiCG.TMDT.modules.marketing.entity.Banner;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends MongoRepository<Banner, String> {
    // Lấy banner đang bật, sắp xếp theo thứ tự
    List<Banner> findByIsActiveTrueOrderByDisplayOrderAsc();
}