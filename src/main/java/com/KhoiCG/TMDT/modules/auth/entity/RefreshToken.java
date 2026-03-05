package com.KhoiCG.TMDT.modules.auth.entity;

import com.KhoiCG.TMDT.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private boolean revoked;

    public boolean isExpired() {
        return java.time.LocalDateTime.now().isAfter(this.expiryDate);
    }

}