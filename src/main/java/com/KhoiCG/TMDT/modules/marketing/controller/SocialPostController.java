package com.KhoiCG.TMDT.modules.marketing.controller;

import com.KhoiCG.TMDT.modules.marketing.dto.FacebookConnectionStatusResponse;
import com.KhoiCG.TMDT.modules.marketing.dto.FacebookPostRequest;
import com.KhoiCG.TMDT.modules.marketing.dto.SocialPostResponse;
import com.KhoiCG.TMDT.modules.marketing.entity.SocialPostStatus;
import com.KhoiCG.TMDT.modules.marketing.service.SocialPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialPostController {

    private final SocialPostService socialPostService;

    @GetMapping("/facebook/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<FacebookConnectionStatusResponse> getFacebookStatus() {
        return ResponseEntity.ok(socialPostService.getFacebookStatus());
    }

    @GetMapping("/facebook/posts")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SocialPostResponse>> listFacebookPosts() {
        return ResponseEntity.ok(socialPostService.listFacebookPosts());
    }

    @PostMapping("/facebook/publish")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SocialPostResponse> publishToFacebook(@Valid @RequestBody FacebookPostRequest request) {
        SocialPostResponse result = socialPostService.publishToFacebook(request);
        if (result.getStatus() == SocialPostStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
