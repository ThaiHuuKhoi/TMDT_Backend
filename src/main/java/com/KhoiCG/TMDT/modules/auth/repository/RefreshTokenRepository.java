package com.KhoiCG.TMDT.modules.auth.repository;

import com.KhoiCG.TMDT.modules.auth.entity.RefreshToken;
import com.KhoiCG.TMDT.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findFirstByUserOrderByExpiryDateDesc(User user);

    void deleteByUser(User user);

    void deleteByExpiryDateBefore(LocalDateTime now);
}