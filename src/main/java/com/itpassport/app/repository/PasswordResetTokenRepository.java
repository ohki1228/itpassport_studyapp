package com.itpassport.app.repository;

import com.itpassport.app.entity.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUserId(Long userId);
}
