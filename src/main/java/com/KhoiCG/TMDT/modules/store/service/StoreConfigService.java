package com.KhoiCG.TMDT.modules.store.service;

import com.KhoiCG.TMDT.modules.store.dto.FooterLinkDto;
import com.KhoiCG.TMDT.modules.store.dto.StoreConfigDto;
import com.KhoiCG.TMDT.modules.store.entity.StoreConfig;
import com.KhoiCG.TMDT.modules.store.repository.StoreConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreConfigService {

    private static final Long CONFIG_ID = 1L;

    private final StoreConfigRepository repository;
    private final ObjectMapper objectMapper;

    private static final List<FooterLinkDto> DEFAULT_CUSTOMER_CARE = List.of(
            new FooterLinkDto("Trung tâm trợ giúp", "/thong-tin/trung-tam-tro-giup"),
            new FooterLinkDto("Hướng dẫn mua hàng", "/thong-tin/huong-dan-mua-hang"),
            new FooterLinkDto("Chính sách vận chuyển", "/thong-tin/chinh-sach-van-chuyen"),
            new FooterLinkDto("Chính sách đổi trả & hoàn tiền", "/thong-tin/doi-tra-hoan-tien"),
            new FooterLinkDto("Chính sách bảo hành", "/thong-tin/chinh-sach-bao-hanh")
    );

    private static final List<FooterLinkDto> DEFAULT_ABOUT = List.of(
            new FooterLinkDto("Giới thiệu công ty", "/thong-tin/ve-chung-toi"),
            new FooterLinkDto("Tuyển dụng", "/thong-tin/tuyen-dung"),
            new FooterLinkDto("Điều khoản sử dụng", "/thong-tin/dieu-khoan-su-dung"),
            new FooterLinkDto("Chính sách bảo mật", "/thong-tin/chinh-sach-bao-mat"),
            new FooterLinkDto("Chương trình tiếp thị liên kết", "/thong-tin/chuong-trinh-affiliate")
    );

    public StoreConfig getConfig() {
        return repository.findById(CONFIG_ID).orElseGet(() ->
                repository.save(StoreConfig.builder()
                        .id(CONFIG_ID)
                        .storeName("KCG Shop")
                        .description("")
                        .address("123 Đường Cầu Giấy, Quận Đống Đa, TP. Hà Nội")
                        .hotline("1900 8888")
                        .email("cskh@kcgshop.com")
                        .facebook("")
                        .instagram("")
                        .youtube("")
                        .tiktok("")
                        .customerCareLinksJson(toJson(DEFAULT_CUSTOMER_CARE))
                        .aboutLinksJson(toJson(DEFAULT_ABOUT))
                        .showVisa(true)
                        .showMastercard(true)
                        .showVnpay(true)
                        .countryRegion("Quốc gia & Khu vực: Việt Nam")
                        .build())
        );
    }

    public StoreConfigDto getConfigDto() {
        return toDto(getConfig());
    }

    @Transactional
    public StoreConfigDto updateConfig(StoreConfigDto req) {
        StoreConfig cfg = getConfig();
        cfg.setStoreName(req.getStoreName());
        cfg.setDescription(req.getDescription());
        cfg.setAddress(req.getAddress());
        cfg.setHotline(req.getHotline());
        cfg.setEmail(req.getEmail());
        cfg.setFacebook(req.getFacebook());
        cfg.setInstagram(req.getInstagram());
        cfg.setYoutube(req.getYoutube());
        cfg.setTiktok(req.getTiktok());
        cfg.setCustomerCareLinksJson(toJson(req.getCustomerCareLinks()));
        cfg.setAboutLinksJson(toJson(req.getAboutLinks()));
        cfg.setShowVisa(req.isShowVisa());
        cfg.setShowMastercard(req.isShowMastercard());
        cfg.setShowVnpay(req.isShowVnpay());
        cfg.setCountryRegion(req.getCountryRegion());
        cfg.setLogoUrl(req.getLogoUrl());
        return toDto(repository.save(cfg));
    }

    private StoreConfigDto toDto(StoreConfig cfg) {
        return StoreConfigDto.builder()
                .storeName(cfg.getStoreName())
                .description(cfg.getDescription())
                .address(cfg.getAddress())
                .hotline(cfg.getHotline())
                .email(cfg.getEmail())
                .facebook(cfg.getFacebook())
                .instagram(cfg.getInstagram())
                .youtube(cfg.getYoutube())
                .tiktok(cfg.getTiktok())
                .customerCareLinks(parseLinks(cfg.getCustomerCareLinksJson(), DEFAULT_CUSTOMER_CARE))
                .aboutLinks(parseLinks(cfg.getAboutLinksJson(), DEFAULT_ABOUT))
                .showVisa(cfg.getShowVisa() == null || cfg.getShowVisa())
                .showMastercard(cfg.getShowMastercard() == null || cfg.getShowMastercard())
                .showVnpay(cfg.getShowVnpay() == null || cfg.getShowVnpay())
                .countryRegion(cfg.getCountryRegion() != null ? cfg.getCountryRegion() : "Quốc gia & Khu vực: Việt Nam")
                .logoUrl(cfg.getLogoUrl())
                .build();
    }

    private List<FooterLinkDto> parseLinks(String json, List<FooterLinkDto> fallback) {
        if (json == null || json.isBlank()) return fallback;
        try {
            return objectMapper.readValue(json, new TypeReference<List<FooterLinkDto>>() {});
        } catch (Exception e) {
            return fallback;
        }
    }

    private String toJson(List<FooterLinkDto> links) {
        if (links == null) return "[]";
        try {
            return objectMapper.writeValueAsString(links);
        } catch (Exception e) {
            return "[]";
        }
    }
}
