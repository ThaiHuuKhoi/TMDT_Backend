package com.KhoiCG.TMDT.modules.marketing.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookGraphService {

    private final ObjectMapper objectMapper;

    @Value("${app.facebook.page-id:}")
    private String pageId;

    @Value("${app.facebook.page-access-token:}")
    private String pageAccessToken;

    @Value("${app.facebook.graph-version:v21.0}")
    private String graphVersion;

    private RestClient graphClient;

    @PostConstruct
    private void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        graphClient = RestClient.builder()
                .baseUrl("https://graph.facebook.com/" + graphVersion)
                .requestFactory(factory)
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(pageId) && StringUtils.hasText(pageAccessToken);
    }

    public String getPageId() {
        return pageId;
    }

    /**
     * Đăng bài lên Facebook Page.
     * Ưu tiên: có ảnh -> photo post; có link -> feed + link; chỉ text -> feed.
     */
    public String publishPagePost(String message, String linkUrl, String imageUrl) {
        if (!isConfigured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "FACEBOOK_NOT_CONFIGURED",
                    "Chưa cấu hình FACEBOOK_PAGE_ID và FACEBOOK_PAGE_ACCESS_TOKEN trên server."
            );
        }

        String trimmedMessage = message != null ? message.trim() : "";
        if (!StringUtils.hasText(trimmedMessage)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MESSAGE", "Nội dung bài đăng không được để trống.");
        }

        try {
            if (StringUtils.hasText(imageUrl)) {
                return postPhoto(trimmedMessage, imageUrl.trim());
            }
            return postFeed(trimmedMessage, StringUtils.hasText(linkUrl) ? linkUrl.trim() : null);
        } catch (RestClientResponseException ex) {
            log.error("Facebook Graph API error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "FACEBOOK_PUBLISH_FAILED",
                    parseFacebookError(ex.getResponseBodyAsString())
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Facebook publish failed", ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "FACEBOOK_PUBLISH_FAILED",
                    "Không thể đăng bài lên Facebook: " + ex.getMessage()
            );
        }
    }

    private String postFeed(String message, String linkUrl) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("message", message);
        form.add("access_token", pageAccessToken);
        if (StringUtils.hasText(linkUrl)) {
            form.add("link", linkUrl);
        }

        JsonNode body = graphClient
                .post()
                .uri("/{pageId}/feed", pageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        return requirePostId(body);
    }

    private String postPhoto(String caption, String imageUrl) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("url", imageUrl);
        form.add("caption", caption);
        form.add("access_token", pageAccessToken);

        JsonNode body = graphClient
                .post()
                .uri("/{pageId}/photos", pageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        return requirePostId(body);
    }



    private String requirePostId(JsonNode body) {
        if (body == null || !body.hasNonNull("id")) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "FACEBOOK_INVALID_RESPONSE",
                    "Facebook không trả về ID bài đăng."
            );
        }
        return body.get("id").asText();
    }

    private String parseFacebookError(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "Facebook từ chối yêu cầu đăng bài.";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode err = root.path("error");
            if (err.hasNonNull("message")) {
                return err.get("message").asText();
            }
        } catch (Exception ignored) {
            /* fallback below */
        }
        return "Facebook từ chối yêu cầu đăng bài.";
    }
}
