package com.KhoiCG.TMDT.modules.info.controller;

import com.KhoiCG.TMDT.modules.info.dto.InfoPageDto;
import com.KhoiCG.TMDT.modules.info.service.InfoPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InfoPageController {

    private final InfoPageService service;

    @GetMapping("/api/admin/info-pages")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<InfoPageDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/api/info-pages/{slug}")
    public ResponseEntity<InfoPageDto> getPublic(@PathVariable String slug) {
        return service.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/admin/info-pages/{slug}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<InfoPageDto> getAdmin(@PathVariable String slug) {
        return service.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/api/admin/info-pages/{slug}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<InfoPageDto> upsert(@PathVariable String slug,
                                               @RequestBody InfoPageDto request) {
        return ResponseEntity.ok(service.upsert(slug, request));
    }
}
