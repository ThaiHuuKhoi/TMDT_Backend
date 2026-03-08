package com.KhoiCG.TMDT.modules.marketing.service;

import com.KhoiCG.TMDT.modules.marketing.dto.BannerRequest;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerResponse;
import com.KhoiCG.TMDT.modules.marketing.entity.Banner;
import com.KhoiCG.TMDT.modules.marketing.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {

    private final BannerRepository bannerRepository;

    // --- DÀNH CHO HOMEPAGE (CÓ CACHE) ---
    @Cacheable(value = "banners", key = "'active'")
    public List<BannerResponse> getActiveBanners() {
        log.info("🚀 [CACHE MISS] - Đang query Database để lấy danh sách Banner quảng cáo...");
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // --- DÀNH CHO ADMIN DASHBOARD (KHÔNG CACHE) ---
    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @CacheEvict(value = "banners", allEntries = true)
    @Transactional
    public BannerResponse saveBanner(BannerRequest request) {
        log.info("🧹 [CACHE EVICT] - Đã xóa cache Banners vì có quảng cáo mới được lưu.");
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .linkUrl(request.getLinkUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                // Lấy trạng thái từ Request (Frontend truyền xuống)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToResponse(bannerRepository.save(banner));
    }

    @CacheEvict(value = "banners", allEntries = true)
    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new RuntimeException("Banner không tồn tại!");
        }
        log.info("🧹 [CACHE EVICT] - Đã xóa cache Banners do xóa quảng cáo ID: {}", id);
        bannerRepository.deleteById(id);
    }

    // Hàm chuyển đổi Entity -> DTO
    private BannerResponse mapToResponse(Banner banner) {
        String finalUrl = "";
        if (banner.getTargetType() != null) {
            finalUrl = switch (banner.getTargetType()) {
                case PRODUCT -> "/products/" + banner.getTargetId();
                case CATEGORY -> "/categories/" + banner.getTargetId();
                case EXTERNAL_LINK -> banner.getLinkUrl();
            };
        }

        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .description(banner.getDescription())
                .imageUrl(banner.getImageUrl())
                .targetUrl(finalUrl) // Dành cho Slider ở Homepage
                .displayOrder(banner.getDisplayOrder())
                // Trả về thêm dữ liệu cho Bảng quản trị Admin
                .targetType(banner.getTargetType())
                .targetId(banner.getTargetId())
                .linkUrl(banner.getLinkUrl())
                .isActive(banner.getIsActive())
                .build();
    }
}