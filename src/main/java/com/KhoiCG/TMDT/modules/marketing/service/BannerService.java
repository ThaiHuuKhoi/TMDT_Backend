package com.KhoiCG.TMDT.modules.marketing.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerRequest;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerPageResponse;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerResponse;
import com.KhoiCG.TMDT.modules.marketing.entity.Banner;
import com.KhoiCG.TMDT.modules.marketing.entity.TargetType;
import com.KhoiCG.TMDT.modules.marketing.repository.BannerRepository;
import com.KhoiCG.TMDT.modules.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {

    private final BannerRepository bannerRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = "banners", key = "'active'")
    public List<BannerResponse> getActiveBanners() {
        log.info("[CACHE MISS] - Đang query Database để lấy danh sách Banner quảng cáo...");
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BannerPageResponse getAllBannersPage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<Banner> bannerPage = bannerRepository.findAll(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by("id")))
        );
        return BannerPageResponse.builder()
                .items(bannerPage.getContent().stream().map(this::mapToResponse).toList())
                .page(bannerPage.getNumber())
                .size(bannerPage.getSize())
                .totalItems(bannerPage.getTotalElements())
                .totalPages(bannerPage.getTotalPages())
                .hasNext(bannerPage.hasNext())
                .hasPrevious(bannerPage.hasPrevious())
                .build();
    }

    @CacheEvict(value = "banners", allEntries = true)
    @Transactional
    public BannerResponse saveBanner(BannerRequest request) {
        validateTarget(request);
        log.info("[CACHE EVICT] - Đã xóa cache Banners vì có quảng cáo mới được lưu.");
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .linkUrl(request.getLinkUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToResponse(bannerRepository.save(banner));
    }

    @CacheEvict(value = "banners", allEntries = true)
    @Transactional
    public BannerResponse updateBanner(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BANNER_NOT_FOUND", "Banner không tồn tại!"));
        validateTarget(request);
        banner.setTitle(request.getTitle());
        banner.setDescription(request.getDescription());
        banner.setImageUrl(request.getImageUrl());
        banner.setTargetType(request.getTargetType());
        banner.setTargetId(request.getTargetId());
        banner.setLinkUrl(request.getLinkUrl());
        if (request.getDisplayOrder() != null) banner.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) banner.setIsActive(request.getIsActive());
        log.info("[CACHE EVICT] - Đã xóa cache Banners do cập nhật quảng cáo ID: {}", id);
        return mapToResponse(bannerRepository.save(banner));
    }

    @CacheEvict(value = "banners", allEntries = true)
    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BANNER_NOT_FOUND", "Banner không tồn tại!");
        }
        log.info("[CACHE EVICT] - Đã xóa cache Banners do xóa quảng cáo ID: {}", id);
        bannerRepository.deleteById(id);
    }

    private void validateTarget(BannerRequest request) {
        if (request.getTargetType() == TargetType.EXTERNAL_LINK) {
            if (request.getLinkUrl() == null || request.getLinkUrl().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "BANNER_LINK_REQUIRED",
                        "linkUrl là bắt buộc khi targetType là EXTERNAL_LINK");
            }
            return;
        }
        if (request.getTargetId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BANNER_TARGET_ID_REQUIRED",
                    "targetId là bắt buộc khi targetType là PRODUCT hoặc CATEGORY");
        }
    }

    private BannerResponse mapToResponse(Banner banner) {
        String finalUrl = "";
        if (banner.getTargetType() != null) {
            finalUrl = switch (banner.getTargetType()) {
                case PRODUCT -> "/products/" + banner.getTargetId();
                case CATEGORY -> categoryRepository.findById(banner.getTargetId())
                        .map(cat -> "/products?category=" + cat.getSlug())
                        .orElse("/products");
                case EXTERNAL_LINK -> banner.getLinkUrl();
            };
        }

        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .description(banner.getDescription())
                .imageUrl(banner.getImageUrl())
                .targetUrl(finalUrl)
                .displayOrder(banner.getDisplayOrder())
                .targetType(banner.getTargetType())
                .targetId(banner.getTargetId())
                .linkUrl(banner.getLinkUrl())
                .isActive(banner.getIsActive())
                .build();
    }
}