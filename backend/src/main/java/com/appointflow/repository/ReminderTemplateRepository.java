package com.appointflow.repository;

import com.appointflow.entity.ReminderTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderTemplateRepository extends JpaRepository<ReminderTemplate, Long> {
    List<ReminderTemplate> findByTenantId(Long tenantId);
    List<ReminderTemplate> findByTenantIdAndAktifTrue(Long tenantId);
    java.util.Optional<ReminderTemplate> findByTenantIdAndHizmetIdAndAktifTrue(Long tenantId, Long hizmetId);
}
