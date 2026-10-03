package com.appointflow.repository;

import com.appointflow.entity.CustomerSegment;
import com.appointflow.entity.SegmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, Long> {

    List<CustomerSegment> findByTenantId(Long tenantId);

    List<CustomerSegment> findByTenantIdAndSegmentType(Long tenantId, SegmentType segmentType);

    Optional<CustomerSegment> findByTenantIdAndCustomerId(Long tenantId, Long customerId);

    @Query("SELECT cs.segmentType, COUNT(cs) FROM CustomerSegment cs WHERE cs.tenantId = :tenantId GROUP BY cs.segmentType")
    List<Object[]> countBySegmentType(@Param("tenantId") Long tenantId);

    void deleteByTenantId(Long tenantId);
}
