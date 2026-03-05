package com.KhoiCG.TMDT.modules.marketing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    private Long targetId;

    @Column(length = 500)
    private String linkUrl;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    private Integer displayOrder;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}