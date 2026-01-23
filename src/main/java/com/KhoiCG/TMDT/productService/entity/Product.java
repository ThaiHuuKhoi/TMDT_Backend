package com.KhoiCG.TMDT.productService.entity;

import com.KhoiCG.TMDT.productService.utils.StringListConverter;
import com.KhoiCG.TMDT.productService.utils.StringMapConverter;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
@Data
@EntityListeners(AuditingEntityListener.class) // Tự động điền createdAt
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Long price; // Java Long thay cho Int để tránh tràn số tiền
    private String description;

    @Column(unique = true)
    private String slug;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT") // MySQL lưu JSON dưới dạng TEXT hoặc JSON
    private List<String> colors;

    @Convert(converter = StringMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> images;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @CreatedDate
    private LocalDateTime createdAt;
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> sizes;
}