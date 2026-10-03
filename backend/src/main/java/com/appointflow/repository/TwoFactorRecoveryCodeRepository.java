package com.appointflow.repository;

import com.appointflow.entity.TwoFactorRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TwoFactorRecoveryCodeRepository extends JpaRepository<TwoFactorRecoveryCode, Long> {

    List<TwoFactorRecoveryCode> findByKullaniciIdAndUsedAtIsNull(Long kullaniciId);

    Optional<TwoFactorRecoveryCode> findByCodeHashAndUsedAtIsNull(String codeHash);

    @Modifying
    @Query("DELETE FROM TwoFactorRecoveryCode r WHERE r.kullaniciId = :kullaniciId")
    void deleteByKullaniciId(@Param("kullaniciId") Long kullaniciId);

    long countByKullaniciIdAndUsedAtIsNull(Long kullaniciId);
}
