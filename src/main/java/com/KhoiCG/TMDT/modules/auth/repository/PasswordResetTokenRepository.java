package com.KhoiCG.TMDT.modules.auth.repository;

import com.KhoiCG.TMDT.modules.auth.entity.PasswordResetToken;
import com.KhoiCG.TMDT.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}