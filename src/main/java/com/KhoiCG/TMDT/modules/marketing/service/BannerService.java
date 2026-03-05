// File: src/main/java/com/KhoiCG/TMDT/modules/marketing/service/BannerService.java
package com.KhoiCG.TMDT.modules.marketing.service;

import com.KhoiCG.TMDT.modules.marketing.dto.BannerRequest;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerResponse;
import com.KhoiCG.TMDT.modules.marketing.entity.Banner;
import com.KhoiCG.TMDT.modules.marketing.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public BannerResponse saveBanner(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .linkUrl(request.getLinkUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(true)
                .build();

        return mapToResponse(bannerRepository.save(banner));
    }

    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new RuntimeException("Banner không tồn tại!");
        }
        bannerRepository.deleteById(id);
    }

    private BannerResponse mapToResponse(Banner banner) {
        String finalUrl = switch (banner.getTargetType()) {
            case PRODUCT -> "/products/" + banner.getTargetId();
            case CATEGORY -> "/categories/" + banner.getTargetId();
            case EXTERNAL_LINK -> banner.getLinkUrl();
        };

        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .description(banner.getDescription())
                .imageUrl(banner.getImageUrl())
                .targetUrl(finalUrl)
                .displayOrder(banner.getDisplayOrder())
                .build();
    }
}