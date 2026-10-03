package com.appointflow.repository;

import com.appointflow.entity.CommissionRule;
import com.appointflow.entity.CommissionScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Long> {

    List<CommissionRule> findByTenantIdAndAktifTrue(Long tenantId);

    List<CommissionRule> findByTenantIdAndScopeAndAktifTrue(Long tenantId, CommissionScope scope);

    // ─── SERVICE scope lookups (urune bagli degil) ─────────────────────────────

    @Query("SELECT r FROM CommissionRule r WHERE r.tenantId = :tenantId AND r.staffId = :staffId " +
           "AND r.scope = :scope AND r.productId IS NULL AND r.productKategori IS NULL AND r.aktif = true")
    Optional<CommissionRule> findStaffGeneralRule(@Param("tenantId") Long tenantId,
                                                   @Param("staffId") Long staffId,
                                                   @Param("scope") CommissionScope scope);

    @Query("SELECT r FROM CommissionRule r WHERE r.tenantId = :tenantId AND r.staffId IS NULL " +
           "AND r.scope = :scope AND r.productId IS NULL AND r.productKategori IS NULL AND r.aktif = true")
    Optional<CommissionRule> findTenantDefaultRule(@Param("tenantId") Long tenantId,
                                                    @Param("scope") CommissionScope scope);

    // ─── PRODUCT scope — 4 seviye lookup ───────────────────────────────────────

    /** 1. Cesit: Calisan + Belirli urun */
    @Query("SELECT r FROM CommissionRule r WHERE r.tenantId = :tenantId AND r.staffId = :staffId " +
           "AND r.scope = 'PRODUCT' AND r.productId = :productId AND r.aktif = true")
    Optional<CommissionRule> findStaffProductRule(@Param("tenantId") Long tenantId,
                                                   @Param("staffId") Long staffId,
                                                   @Param("productId") Long productId);

    /** 2. Cesit: Calisan + Kategori */
    @Query("SELECT r FROM CommissionRule r WHERE r.tenantId = :tenantId AND r.staffId = :staffId " +
           "AND r.scope = 'PRODUCT' AND r.productKategori = :kategori " +
           "AND r.productId IS NULL AND r.aktif = true")
    Optional<CommissionRule> findStaffKategoriRule(@Param("tenantId") Long tenantId,
                                                    @Param("staffId") Long staffId,
                                                    @Param("kategori") String kategori);

    /** 3. Cesit: Tenant default + Belirli urun (her calisan icin) */
    @Query("SELECT r FROM CommissionRule r WHERE r.tenantId = :tenantId AND r.staffId IS NULL " +
           "AND r.scope = 'PRODUCT' AND r.productId = :productId AND r.aktif = true")
    Optional<CommissionRule> findTenantProductRule(@Param("tenantId") Long tenantId,
                                                    @Param("productId") Long productId);

    /** 4. Cesit: Tenant default + Kategori */
    @Query("SELECT r FROM CommissionRule r WHERE r.tenantId = :tenantId AND r.staffId IS NULL " +
           "AND r.scope = 'PRODUCT' AND r.productKategori = :kategori " +
           "AND r.productId IS NULL AND r.aktif = true")
    Optional<CommissionRule> findTenantKategoriRule(@Param("tenantId") Long tenantId,
                                                     @Param("kategori") String kategori);

    // ─── Duplicate kontrolu — exact key match ──────────────────────────────────

    @Query("SELECT COUNT(r) > 0 FROM CommissionRule r WHERE r.tenantId = :tenantId " +
           "AND r.scope = :scope AND r.aktif = true " +
           "AND ((:staffId IS NULL AND r.staffId IS NULL) OR r.staffId = :staffId) " +
           "AND ((:productId IS NULL AND r.productId IS NULL) OR r.productId = :productId) " +
           "AND ((:kategori IS NULL AND r.productKategori IS NULL) OR r.productKategori = :kategori)")
    boolean existsActiveRuleForKey(@Param("tenantId") Long tenantId,
                                    @Param("scope") CommissionScope scope,
                                    @Param("staffId") Long staffId,
                                    @Param("productId") Long productId,
                                    @Param("kategori") String kategori);
}
