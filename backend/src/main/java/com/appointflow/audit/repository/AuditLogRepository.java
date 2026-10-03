package com.appointflow.audit.repository;

import com.appointflow.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.createdAt BETWEEN :from AND :to " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findByTenantIdAndPeriod(@Param("tenantId") Long tenantId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntity(@Param("tenantId") Long tenantId,
                                 @Param("entityType") String entityType,
                                 @Param("entityId") Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.action = :action " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantIdAndAction(@Param("tenantId") Long tenantId,
                                            @Param("action") String action,
                                            Pageable pageable);
}
