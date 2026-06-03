package com.KhoiCG.TMDT.modules.auth.repository;

import com.KhoiCG.TMDT.modules.auth.entity.RefreshToken;
import com.KhoiCG.TMDT.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findFirstByUserOrderByExpiryDateDesc(User user);

    void deleteByUser(User user);

    void deleteByUserAndExpiryDateBefore(User user, LocalDateTime now);

    void deleteByExpiryDateBefore(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user.email = :email")
    void deleteByUserEmail(@Param("email") String email);
}