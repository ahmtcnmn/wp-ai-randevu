package com.appointflow.repository;

import com.appointflow.entity.EarningPeriod;
import com.appointflow.entity.EarningStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarningPeriodRepository extends JpaRepository<EarningPeriod, Long> {

    List<EarningPeriod> findByTenantId(Long tenantId);

    List<EarningPeriod> findByTenantIdAndStaffId(Long tenantId, Long staffId);

    Optional<EarningPeriod> findByTenantIdAndStaffIdAndPeriodStartAndPeriodEnd(
            Long tenantId, Long staffId, LocalDate periodStart, LocalDate periodEnd);

    List<EarningPeriod> findByTenantIdAndStatus(Long tenantId, EarningStatus status);
}
