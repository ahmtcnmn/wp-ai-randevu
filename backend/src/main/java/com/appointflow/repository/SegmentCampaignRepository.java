package com.appointflow.repository;

import com.appointflow.entity.SegmentCampaign;
import com.appointflow.entity.SegmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SegmentCampaignRepository extends JpaRepository<SegmentCampaign, Long> {

    List<SegmentCampaign> findByTenantId(Long tenantId);

    List<SegmentCampaign> findByTenantIdAndHedefSegment(Long tenantId, SegmentType hedefSegment);

    Long countByTenantId(Long tenantId);

    // Aynı segmente bugün zaten kampanya gönderilmiş mi? (spam koruması — legacy, kullanılmıyor)
    @Query("SELECT COUNT(c) > 0 FROM SegmentCampaign c " +
           "WHERE c.tenantId = :tenantId AND c.hedefSegment = :segment AND c.createdAt >= :since")
    boolean existsByTenantIdAndSegmentSince(@Param("tenantId") Long tenantId,
                                             @Param("segment") SegmentType segment,
                                             @Param("since") LocalDateTime since);

    /** Belirli segmente belirli zamandan beri gönderilen kampanya sayısı (günlük limit kontrolü). */
    @Query("SELECT COUNT(c) FROM SegmentCampaign c " +
           "WHERE c.tenantId = :tenantId AND c.hedefSegment = :segment AND c.createdAt >= :since")
    long countByTenantIdAndSegmentSince(@Param("tenantId") Long tenantId,
                                         @Param("segment") SegmentType segment,
                                         @Param("since") LocalDateTime since);
}
