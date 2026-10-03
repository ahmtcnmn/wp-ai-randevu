package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.BackgroundJobResponse;
import com.appointflow.dto.DeadLetterJobResponse;
import com.appointflow.dto.JobSubmitRequest;
import com.appointflow.entity.*;
import com.appointflow.repository.BackgroundJobRepository;
import com.appointflow.repository.DeadLetterJobRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BackgroundJobService {

    private static final String IDEM_KEY_PREFIX = "job:idem:";
    private static final Duration IDEM_TTL = Duration.ofHours(24);

    private final BackgroundJobRepository jobRepository;
    private final DeadLetterJobRepository dlqRepository;
    private final StringRedisTemplate redisTemplate;

    public BackgroundJobResponse submit(Long tenantId, JobType jobType, String payload, String idempotencyKey) {
        // Fast-path: Redis check
        String redisKey = IDEM_KEY_PREFIX + idempotencyKey;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.debug("Job idempotent skip (Redis): key={}", idempotencyKey);
            return jobRepository.findByIdempotencyKey(idempotencyKey)
                    .map(this::toResponse)
                    .orElse(null);
        }

        // DB check
        if (jobRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw ApiException.conflict("Bu idempotency key ile iş zaten mevcut: " + idempotencyKey);
        }

        BackgroundJob job = BackgroundJob.builder()
                .tenantId(tenantId)
                .jobType(jobType)
                .payload(payload)
                .idempotencyKey(idempotencyKey)
                .build();

        return toResponse(jobRepository.save(job));
    }

    public BackgroundJobResponse submitFromRequest(JobSubmitRequest request) {
        Long tenantId = TenantContext.getTenantId();
        JobType jobType;
        try {
            jobType = JobType.valueOf(request.getJobType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Geçersiz job tipi: " + request.getJobType());
        }
        return submit(tenantId, jobType, request.getPayload(), request.getIdempotencyKey());
    }

    public void markRunning(Long jobId) {
        BackgroundJob job = findJob(jobId);
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);
    }

    public void markSuccess(Long jobId) {
        BackgroundJob job = findJob(jobId);
        job.setStatus(JobStatus.SUCCESS);
        jobRepository.save(job);
        redisTemplate.opsForValue().set(
                IDEM_KEY_PREFIX + job.getIdempotencyKey(),
                "done",
                IDEM_TTL.toSeconds(),
                TimeUnit.SECONDS
        );
    }

    public void markFailed(Long jobId, String errorMessage) {
        BackgroundJob job = findJob(jobId);
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(errorMessage);

        if (job.getRetryCount() >= job.getMaxRetries()) {
            job.setStatus(JobStatus.DEAD);
            jobRepository.save(job);

            DeadLetterJob dlq = DeadLetterJob.builder()
                    .originalJobId(job.getId())
                    .tenantId(job.getTenantId())
                    .jobType(job.getJobType())
                    .payload(job.getPayload())
                    .errorMessage(errorMessage)
                    .build();
            dlqRepository.save(dlq);
            log.warn("Job DLQ'ya taşındı: jobId={}, type={}", jobId, job.getJobType());
        } else {
            job.setStatus(JobStatus.FAILED);
            long backoffMinutes = 5L * (1L << (job.getRetryCount() - 1));
            job.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes));
            jobRepository.save(job);
            log.info("Job başarısız, retry planlandı: jobId={}, nextRetryAt={}", jobId, job.getNextRetryAt());
        }
    }

    @Transactional(readOnly = true)
    public List<BackgroundJobResponse> getJobs() {
        Long tenantId = TenantContext.getTenantId();
        return jobRepository.findByTenantId(tenantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeadLetterJobResponse> getDeadLetters() {
        Long tenantId = TenantContext.getTenantId();
        return dlqRepository.findByTenantIdAndResolvedAtIsNull(tenantId)
                .stream().map(this::toDlqResponse).collect(Collectors.toList());
    }

    public BackgroundJobResponse retryDeadLetter(Long dlqId) {
        Long tenantId = TenantContext.getTenantId();
        DeadLetterJob dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> ApiException.notFound("DLQ kaydı bulunamadı."));
        if (!dlq.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu DLQ kaydına erişim yetkiniz yok.");
        }

        String newKey = "dlq-retry-" + dlqId + "-" + System.currentTimeMillis();

        BackgroundJob job = BackgroundJob.builder()
                .tenantId(dlq.getTenantId())
                .jobType(dlq.getJobType())
                .payload(dlq.getPayload())
                .idempotencyKey(newKey)
                .retryCount(0)
                .build();

        BackgroundJob saved = jobRepository.save(job);
        dlq.setResolvedAt(LocalDateTime.now());
        dlqRepository.save(dlq);
        return toResponse(saved);
    }

    public void resolveDeadLetter(Long dlqId) {
        Long tenantId = TenantContext.getTenantId();
        DeadLetterJob dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> ApiException.notFound("DLQ kaydı bulunamadı."));
        if (!dlq.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu DLQ kaydına erişim yetkiniz yok.");
        }
        dlq.setResolvedAt(LocalDateTime.now());
        dlqRepository.save(dlq);
    }

    private BackgroundJob findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("Job bulunamadı: " + jobId));
    }

    private BackgroundJobResponse toResponse(BackgroundJob j) {
        return BackgroundJobResponse.builder()
                .id(j.getId())
                .tenantId(j.getTenantId())
                .jobType(j.getJobType().name())
                .status(j.getStatus().name())
                .payload(j.getPayload())
                .idempotencyKey(j.getIdempotencyKey())
                .retryCount(j.getRetryCount())
                .maxRetries(j.getMaxRetries())
                .nextRetryAt(j.getNextRetryAt())
                .errorMessage(j.getErrorMessage())
                .createdAt(j.getCreatedAt())
                .updatedAt(j.getUpdatedAt())
                .build();
    }

    private DeadLetterJobResponse toDlqResponse(DeadLetterJob d) {
        return DeadLetterJobResponse.builder()
                .id(d.getId())
                .originalJobId(d.getOriginalJobId())
                .tenantId(d.getTenantId())
                .jobType(d.getJobType().name())
                .payload(d.getPayload())
                .errorMessage(d.getErrorMessage())
                .resolvedAt(d.getResolvedAt())
                .createdAt(d.getCreatedAt())
                .build();
    }

}
