package com.KhoiCG.TMDT.modules.info.entity;

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
@Table(name = "info_pages")
public class InfoPageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug;

    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String contentHtml;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
