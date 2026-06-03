package com.KhoiCG.TMDT.modules.store.controller;

import com.KhoiCG.TMDT.modules.store.dto.StoreConfigDto;
import com.KhoiCG.TMDT.modules.store.service.StoreConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class StoreConfigController {

    private final StoreConfigService storeConfigService;

    // Public: client fetch để hiển thị Footer, trang thông tin
    @GetMapping("/api/store/config")
    public ResponseEntity<StoreConfigDto> getPublicConfig() {
        return ResponseEntity.ok(storeConfigService.getConfigDto());
    }

    // Admin: lấy config
    @GetMapping("/api/admin/store-config")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<StoreConfigDto> getAdminConfig() {
        return ResponseEntity.ok(storeConfigService.getConfigDto());
    }

    // Admin: cập nhật
    @PutMapping("/api/admin/store-config")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<StoreConfigDto> updateConfig(@RequestBody StoreConfigDto request) {
        return ResponseEntity.ok(storeConfigService.updateConfig(request));
    }
}
