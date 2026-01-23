package com.KhoiCG.TMDT.marketingService.controller;

import com.KhoiCG.TMDT.marketingService.entity.Banner;
import com.KhoiCG.TMDT.marketingService.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerRepository bannerRepository;

    // 1. PUBLIC: Lấy danh sách banner để hiển thị trang chủ
    @GetMapping
    public ResponseEntity<List<Banner>> getActiveBanners() {
        return ResponseEntity.ok(bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }

    // 2. ADMIN: Thêm banner mới
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Banner> createBanner(@RequestBody Banner banner) {
        return ResponseEntity.ok(bannerRepository.save(banner));
    }

    // 3. ADMIN: Xóa banner
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteBanner(@PathVariable String id) {
        bannerRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }
}