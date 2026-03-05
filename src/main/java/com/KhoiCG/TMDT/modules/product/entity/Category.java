package com.KhoiCG.TMDT.modules.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String slug;

    private String image;

    // Quan hệ 1-N với Product (nếu cần mapping ngược lại)
    // @OneToMany(mappedBy = "category")
    // private List<Product> products;
}