package com.KhoiCG.TMDT.modules.shipping.entity;

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
@Table(name = "shipping_config")
public class ShippingConfig {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long defaultFeeVnd;

    @Column(nullable = false)
    private Integer gramsPerItemUnit;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
