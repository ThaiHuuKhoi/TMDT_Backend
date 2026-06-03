package com.KhoiCG.TMDT.modules.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store_config")
public class StoreConfig {

    @Id
    private Long id;

    private String storeName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;
    private String hotline;
    private String email;
    private String facebook;
    private String instagram;
    private String youtube;
    private String tiktok;

    @Column(columnDefinition = "TEXT")
    private String customerCareLinksJson;

    @Column(columnDefinition = "TEXT")
    private String aboutLinksJson;

    private Boolean showVisa;
    private Boolean showMastercard;
    private Boolean showVnpay;

    private String countryRegion;

    private String logoUrl;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
