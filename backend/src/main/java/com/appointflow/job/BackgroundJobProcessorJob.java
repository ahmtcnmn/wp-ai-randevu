package com.appointflow.job;

import com.appointflow.entity.BackgroundJob;
import com.appointflow.entity.JobStatus;
import com.appointflow.repository.BackgroundJobRepository;
import com.appointflow.service.BackgroundJobService;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackgroundJobProcessorJob {

    private final BackgroundJobRepository jobRepository;
    private final BackgroundJobService jobService;

    @Scheduled(cron = "0 */2 * * * *")
    public void processJobs() {
        List<BackgroundJob> pending = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PENDING);
        List<BackgroundJob> retryReady = jobRepository.findReadyForRetry(LocalDateTime.now());

        List<BackgroundJob> toProcess = new ArrayList<>();
        toProcess.addAll(pending);
        toProcess.addAll(retryReady);

        if (toProcess.isEmpty()) {
            return;
        }

        log.info("BackgroundJobProcessor: {} iş işlenecek", toProcess.size());

        for (BackgroundJob job : toProcess) {
            try {
                jobService.markRunning(job.getId());
                TenantContext.set(job.getTenantId());
                dispatch(job);
                jobService.markSuccess(job.getId());
            } catch (Exception e) {
                log.error("Job başarısız: jobId={}, type={}, hata={}", job.getId(), job.getJobType(), e.getMessage());
                jobService.markFailed(job.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void dispatch(BackgroundJob job) {
        switch (job.getJobType()) {
            case GENERIC -> log.info("GENERIC job işlendi: jobId={}, payload={}", job.getId(), job.getPayload());
            default -> log.warn("Bilinmeyen job tipi: {}, jobId={}", job.getJobType(), job.getId());
        }
    }
}
