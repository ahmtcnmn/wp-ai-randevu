package com.appointflow.repository;

import com.appointflow.entity.Kullanici;
import com.appointflow.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KullaniciRepository extends JpaRepository<Kullanici, Long> {
    Optional<Kullanici> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Kullanici> findByRol(Role rol);
    List<Kullanici> findByRolAndSubeId(Role rol, Long subeId);
    Optional<Kullanici> findByTelefon(String telefon);
    List<Kullanici> findByTenantId(Long tenantId);

    @Query("SELECT COUNT(k) FROM Kullanici k WHERE k.tenantId = :tenantId AND (k.rol = 'STAFF' OR k.rol = 'BRANCH_MANAGER')")
    long countStaffByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(k) FROM Kullanici k WHERE k.sube.id = :subeId AND k.aktif = true")
    long countBySubeIdAndAktifTrue(@Param("subeId") Long subeId);

    @Query("SELECT k FROM Kullanici k WHERE k.tenantId = :tenantId AND k.rol = 'OWNER' AND k.aktif = true")
    Optional<Kullanici> findOwnerByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(k) FROM Kullanici k WHERE k.tenantId = :tenantId AND k.rol = 'OWNER' AND k.aktif = true")
    long countOwnersByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT k FROM Kullanici k WHERE k.tenantId = :tenantId AND (k.rol = 'OWNER' OR k.rol = 'ADMIN') AND k.aktif = true")
    List<Kullanici> findAdminsByTenantId(@Param("tenantId") Long tenantId);
}
