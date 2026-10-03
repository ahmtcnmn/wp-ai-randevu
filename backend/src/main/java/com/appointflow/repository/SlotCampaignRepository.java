package com.appointflow.repository;

import com.appointflow.entity.CampaignStatus;
import com.appointflow.entity.SlotCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotCampaignRepository extends JpaRepository<SlotCampaign, Long> {

    List<SlotCampaign> findByTenantId(Long tenantId);

    List<SlotCampaign> findByTenantIdAndStatus(Long tenantId, CampaignStatus status);

    Optional<SlotCampaign> findByTenantIdAndCancelledRandevuId(Long tenantId, Long randevuId);

    // Süresi dolmuş aktif kampanyaları bul
    List<SlotCampaign> findByStatusAndExpiresAtBefore(CampaignStatus status, LocalDateTime now);

    Long countByTenantIdAndStatus(Long tenantId, CampaignStatus status);

    @Query("SELECT c.status, COUNT(c) FROM SlotCampaign c WHERE c.tenantId = :tenantId GROUP BY c.status")
    List<Object[]> countByTenantIdGroupByStatus(@Param("tenantId") Long tenantId);
}
