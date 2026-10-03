package com.appointflow.repository;

import com.appointflow.entity.Earning;
import com.appointflow.entity.EarningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EarningRepository extends JpaRepository<Earning, Long> {

    List<Earning> findByTenantIdAndStaffId(Long tenantId, Long staffId);

    List<Earning> findByTenantIdAndStaffIdAndStatus(Long tenantId, Long staffId, EarningStatus status);

    List<Earning> findByEarningPeriodId(Long periodId);

    @Query("SELECT e FROM Earning e WHERE e.tenantId = :tenantId AND e.staffId = :staffId " +
           "AND e.status = 'PENDING' " +
           "AND CAST(e.randevu.tarihSaat AS LocalDate) >= :periodStart " +
           "AND CAST(e.randevu.tarihSaat AS LocalDate) <= :periodEnd")
    List<Earning> findPendingForPeriod(@Param("tenantId") Long tenantId,
                                       @Param("staffId") Long staffId,
                                       @Param("periodStart") LocalDate periodStart,
                                       @Param("periodEnd") LocalDate periodEnd);

    @Query("SELECT COALESCE(SUM(e.commissionAmount), 0) FROM Earning e " +
           "WHERE e.tenantId = :tenantId AND e.staffId = :staffId AND e.status = :status")
    BigDecimal sumCommissionByStaffAndStatus(@Param("tenantId") Long tenantId,
                                              @Param("staffId") Long staffId,
                                              @Param("status") EarningStatus status);

    boolean existsByRandevuId(Long randevuId);
}
