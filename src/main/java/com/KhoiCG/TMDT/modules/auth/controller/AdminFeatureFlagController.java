package com.KhoiCG.TMDT.modules.auth.controller;

import com.KhoiCG.TMDT.modules.auth.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/feature-flags")
@RequiredArgsConstructor
public class AdminFeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Boolean>> getFlags() {
        return ResponseEntity.ok(Map.of("otpRegistration", featureFlagService.isOtpRegistrationEnabled()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Boolean>> updateFlags(@RequestBody Map<String, Boolean> flags) {
        if (flags.containsKey("otpRegistration")) {
            featureFlagService.setOtpRegistrationEnabled(Boolean.TRUE.equals(flags.get("otpRegistration")));
        }
        return ResponseEntity.ok(Map.of("otpRegistration", featureFlagService.isOtpRegistrationEnabled()));
    }
}
