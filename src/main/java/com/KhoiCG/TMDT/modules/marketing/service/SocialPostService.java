package com.KhoiCG.TMDT.modules.marketing.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.marketing.dto.FacebookConnectionStatusResponse;
import com.KhoiCG.TMDT.modules.marketing.dto.FacebookPostRequest;
import com.KhoiCG.TMDT.modules.marketing.dto.SocialPostResponse;
import com.KhoiCG.TMDT.modules.marketing.entity.SocialPlatform;
import com.KhoiCG.TMDT.modules.marketing.entity.SocialPost;
import com.KhoiCG.TMDT.modules.marketing.entity.SocialPostStatus;
import com.KhoiCG.TMDT.modules.marketing.repository.SocialPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialPostService {

    private final SocialPostRepository socialPostRepository;
    private final FacebookGraphService facebookGraphService;

    public FacebookConnectionStatusResponse getFacebookStatus() {
        boolean configured = facebookGraphService.isConfigured();
        return FacebookConnectionStatusResponse.builder()
                .configured(configured)
                .pageId(configured ? facebookGraphService.getPageId() : null)
                .message(configured
                        ? "Đã cấu hình token Page. Có thể đăng bài qua Graph API."
                        : "Thiếu FACEBOOK_PAGE_ID hoặc FACEBOOK_PAGE_ACCESS_TOKEN trong cấu hình server.")
                .build();
    }

    public List<SocialPostResponse> listFacebookPosts() {
        return socialPostRepository.findByPlatformOrderByCreatedAtDesc(SocialPlatform.FACEBOOK)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SocialPostResponse publishToFacebook(FacebookPostRequest request) {
        SocialPost post = SocialPost.builder()
                .platform(SocialPlatform.FACEBOOK)
                .message(request.getMessage().trim())
                .linkUrl(trimToNull(request.getLinkUrl()))
                .imageUrl(trimToNull(request.getImageUrl()))
                .status(SocialPostStatus.DRAFT)
                .build();
        post = socialPostRepository.save(post);

        try {
            String externalId = facebookGraphService.publishPagePost(
                    post.getMessage(),
                    post.getLinkUrl(),
                    post.getImageUrl()
            );
            post.setStatus(SocialPostStatus.PUBLISHED);
            post.setExternalPostId(externalId);
            post.setPublishedAt(LocalDateTime.now());
            post.setErrorMessage(null);
        } catch (ApiException ex) {
            log.warn("Facebook publish failed for post id={}: {}", post.getId(), ex.getMessage());
            post.setStatus(SocialPostStatus.FAILED);
            post.setErrorMessage(ex.getMessage());
            socialPostRepository.save(post);
            throw ex;
        }

        return toResponse(socialPostRepository.save(post));
    }

    private SocialPostResponse toResponse(SocialPost post) {
        return SocialPostResponse.builder()
                .id(post.getId())
                .platform(post.getPlatform())
                .message(post.getMessage())
                .linkUrl(post.getLinkUrl())
                .imageUrl(post.getImageUrl())
                .status(post.getStatus())
                .externalPostId(post.getExternalPostId())
                .errorMessage(post.getErrorMessage())
                .createdAt(post.getCreatedAt())
                .publishedAt(post.getPublishedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
