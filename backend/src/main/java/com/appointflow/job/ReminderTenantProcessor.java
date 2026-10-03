package com.appointflow.job;

import com.appointflow.entity.Customer;
import com.appointflow.entity.Randevu;
import com.appointflow.entity.RandevuDurumu;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.tenant.TenantContext;
import com.appointflow.whatsapp.service.WhatsappMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Hatirlatma job'larinin tenant basina ASYNC ve PARALEL calismasi icin
 * ayri bir bean. @Async sadece farkli sinifa cagirildiginda proxy
 * uzerinden gercek thread'a giriyor — bu yuzden bu ayri sinif.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTenantProcessor {

    private final RandevuRepository randevuRepository;
    private final WhatsappMessageService whatsappMessageService;

    @Async("notificationExecutor")
    public CompletableFuture<Integer> sendRemindersForTenantBetween(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mesajSablonu) {
        int sent = 0;
        try {
            TenantContext.set(tenantId);
            List<Randevu> randevular = randevuRepository
                    .findByTenantIdAndTarihSaatBetweenAndDurum(tenantId, from, to, RandevuDurumu.BEKLIYOR);

            for (Randevu randevu : randevular) {
                Customer customer = randevu.getCustomer();
                if (customer == null || customer.getTelefon() == null) {
                    continue;
                }
                try {
                    String mesaj = String.format(mesajSablonu,
                            customer.getAd(),
                            randevu.getTarihSaat().toLocalTime());
                    whatsappMessageService.sendTextMessage(tenantId, customer.getTelefon(), mesaj);
                    sent++;
                } catch (Exception e) {
                    log.warn("Hatirlatma gonderilemedi: tenantId={}, customerId={}, hata={}",
                            tenantId, customer.getId(), e.getMessage());
                }
            }
            log.debug("Hatirlatma: tenantId={}, gonderilen={}/{}", tenantId, sent, randevular.size());
        } catch (Exception e) {
            log.error("Tenant hatirlatma hatasi: tenantId={}, hata={}", tenantId, e.getMessage());
        } finally {
            TenantContext.clear();
        }
        return CompletableFuture.completedFuture(sent);
    }
}
