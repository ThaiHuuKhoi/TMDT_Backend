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
@Slf4j // Bổ sung log để theo dõi Redis
public class BannerService {

    private final BannerRepository bannerRepository;

    // 1. ÁP DỤNG CACHE KHI LẤY DỮ LIỆU
    // Dùng key tĩnh 'active' vì hàm này luôn trả về cùng 1 danh sách cho tất cả người dùng
    @Cacheable(value = "banners", key = "'active'")
    public List<BannerResponse> getActiveBanners() {
        log.info("🚀 [CACHE MISS] - Đang query Database để lấy danh sách Banner quảng cáo...");
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 2. XÓA CACHE KHI THÊM/SỬA DỮ LIỆU
    // allEntries = true: Xóa sạch toàn bộ rác trong hộp chứa "banners"
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
                .isActive(true) // Có thể bạn sẽ muốn update thêm logic bật/tắt banner sau này
                .build();

        return mapToResponse(bannerRepository.save(banner));
    }

    // 3. XÓA CACHE KHI XÓA DỮ LIỆU
    @CacheEvict(value = "banners", allEntries = true)
    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new RuntimeException("Banner không tồn tại!");
        }
        log.info("🧹 [CACHE EVICT] - Đã xóa cache Banners do xóa quảng cáo ID: {}", id);
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