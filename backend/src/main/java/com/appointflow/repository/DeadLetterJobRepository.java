package com.appointflow.repository;

import com.appointflow.entity.DeadLetterJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeadLetterJobRepository extends JpaRepository<DeadLetterJob, Long> {

    List<DeadLetterJob> findByTenantId(Long tenantId);

    List<DeadLetterJob> findByTenantIdAndResolvedAtIsNull(Long tenantId);
}
