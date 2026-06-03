package com.KhoiCG.TMDT.modules.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreConfigDto {
    private String storeName;
    private String description;
    private String address;
    private String hotline;
    private String email;
    private String facebook;
    private String instagram;
    private String youtube;
    private String tiktok;
    private List<FooterLinkDto> customerCareLinks;
    private List<FooterLinkDto> aboutLinks;
    private boolean showVisa;
    private boolean showMastercard;
    private boolean showVnpay;
    private String countryRegion;
    private String logoUrl;
}
