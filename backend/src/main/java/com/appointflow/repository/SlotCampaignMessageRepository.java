package com.appointflow.repository;

import com.appointflow.entity.SlotCampaignMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotCampaignMessageRepository extends JpaRepository<SlotCampaignMessage, Long> {

    List<SlotCampaignMessage> findByCampaignId(Long campaignId);

    Optional<SlotCampaignMessage> findByCampaignIdAndCustomerId(Long campaignId, Long customerId);

    // Aynı müşteriye bugün kampanya gönderilmiş mi? (spam koruması)
    @Query("SELECT COUNT(m) > 0 FROM SlotCampaignMessage m " +
           "WHERE m.customer.id = :customerId AND m.sentAt >= :since")
    boolean existsByCustomerIdSince(@Param("customerId") Long customerId, @Param("since") LocalDateTime since);
}
