package com.appointflow.repository;

import com.appointflow.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.kullaniciId = :kullaniciId")
    void deleteByKullaniciId(@Param("kullaniciId") Long kullaniciId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :before")
    void deleteExpiredBefore(@Param("before") LocalDateTime before);
}
