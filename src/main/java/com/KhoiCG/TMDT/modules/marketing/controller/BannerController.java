package com.KhoiCG.TMDT.modules.marketing.controller;

import com.KhoiCG.TMDT.modules.marketing.dto.BannerRequest;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerPageResponse;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerResponse;
import com.KhoiCG.TMDT.modules.marketing.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<BannerResponse> createBanner(@Valid @RequestBody BannerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.saveBanner(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BannerResponse> updateBanner(@PathVariable Long id,
                                                       @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.updateBanner(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok("Banner đã được xóa thành công");
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BannerPageResponse> getAllBannersForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bannerService.getAllBannersPage(page, size));
    }
}