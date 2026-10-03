package com.appointflow.repository;

import com.appointflow.entity.BackgroundJob;
import com.appointflow.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {

    List<BackgroundJob> findByTenantId(Long tenantId);

    List<BackgroundJob> findByTenantIdAndStatus(Long tenantId, JobStatus status);

    Optional<BackgroundJob> findByIdempotencyKey(String key);

    List<BackgroundJob> findByStatusOrderByCreatedAtAsc(JobStatus status);

    @Query("SELECT j FROM BackgroundJob j WHERE j.status = 'FAILED' " +
           "AND j.nextRetryAt <= :now AND j.retryCount < j.maxRetries")
    List<BackgroundJob> findReadyForRetry(@Param("now") LocalDateTime now);
}
