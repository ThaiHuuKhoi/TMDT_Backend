package com.KhoiCG.TMDT.modules.product.entity;

import com.KhoiCG.TMDT.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlists")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishList {

    @EmbeddedId
    private WishListId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}