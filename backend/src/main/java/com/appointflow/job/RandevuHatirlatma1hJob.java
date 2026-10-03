package com.appointflow.job;

import com.appointflow.entity.Tenant;
import com.appointflow.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandevuHatirlatma1hJob {

    private static final String MESAJ_SABLONU =
            "Merhaba %s, 1 saat sonra %s saatindeki randevunuz var. Sizi bekliyoruz!";

    private final TenantRepository tenantRepository;
    private final ReminderTenantProcessor reminderProcessor;

    @Scheduled(cron = "0 0 * * * *")
    public void sendReminders() {
        log.info("1h randevu hatirlatma job'i basliyor (async)...");
        LocalDateTime from = LocalDateTime.now().plusMinutes(50);
        LocalDateTime to = LocalDateTime.now().plusMinutes(70);

        List<Tenant> tenants = tenantRepository.findAll();
        List<CompletableFuture<Integer>> futures = new ArrayList<>(tenants.size());

        for (Tenant tenant : tenants) {
            futures.add(reminderProcessor.sendRemindersForTenantBetween(
                    tenant.getId(), from, to, MESAJ_SABLONU));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> {
                    int total = futures.stream().mapToInt(f -> {
                        try { return f.get(); } catch (Exception e) { return 0; }
                    }).sum();
                    log.info("1h hatirlatma tamamlandi: tenant={}, toplam_mesaj={}",
                            tenants.size(), total);
                });
    }
}
