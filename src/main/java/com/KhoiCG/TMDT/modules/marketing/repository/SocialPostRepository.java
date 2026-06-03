package com.KhoiCG.TMDT.modules.marketing.repository;

import com.KhoiCG.TMDT.modules.marketing.entity.SocialPlatform;
import com.KhoiCG.TMDT.modules.marketing.entity.SocialPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface SocialPostRepository extends JpaRepository<SocialPost, Long> {
    List<SocialPost> findByPlatformOrderByCreatedAtDesc(SocialPlatform platform);
}
