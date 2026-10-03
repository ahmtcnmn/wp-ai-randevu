package com.appointflow.repository;

import com.appointflow.entity.AppointmentReminder;
import com.appointflow.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, Long> {

    List<AppointmentReminder> findByTenantId(Long tenantId);

    List<AppointmentReminder> findByRandevuId(Long randevuId);

    List<AppointmentReminder> findByTenantIdAndStatus(Long tenantId, ReminderStatus status);

    // Bugün gönderilmesi gereken PENDING hatırlatmalar (tüm tenant'lar için job)
    @Query("SELECT r FROM AppointmentReminder r WHERE r.status = 'PENDING' " +
           "AND r.gonderimTarihi <= :bugun")
    List<AppointmentReminder> findDueReminders(@Param("bugun") LocalDate bugun);

    // SNOOZED ama tarihi gelmiş
    @Query("SELECT r FROM AppointmentReminder r WHERE r.status = 'SNOOZED' " +
           "AND r.snoozedUntil <= :bugun")
    List<AppointmentReminder> findDueSnoozedReminders(@Param("bugun") LocalDate bugun);

    List<AppointmentReminder> findByCustomerId(Long customerId);

    long countByTenantIdAndStatus(Long tenantId, ReminderStatus status);

    long countByTenantId(Long tenantId);
}
