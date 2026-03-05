package com.KhoiCG.TMDT.modules.marketing.controller;

import com.KhoiCG.TMDT.modules.marketing.dto.BannerRequest;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerResponse;
import com.KhoiCG.TMDT.modules.marketing.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<BannerResponse>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BannerResponse> createBanner(@RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.saveBanner(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok("Banner đã được xóa thành công");
    }
}