package com.rts.modules.auth.persistence;

import com.rts.modules.auth.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, String> {

    Optional<EmailVerificationToken> findByTokenAndConfirmedFalse(String token);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.userId = :userId AND t.confirmed = false")
    void deleteUnconfirmedByUserId(String userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(LocalDateTime cutoff);
}
