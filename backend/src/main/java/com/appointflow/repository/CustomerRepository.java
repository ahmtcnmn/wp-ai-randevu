package com.appointflow.repository;

import com.appointflow.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByTenantId(Long tenantId);
    Optional<Customer> findByTenantIdAndTelefon(Long tenantId, String telefon);
    boolean existsByTenantIdAndTelefon(Long tenantId, String telefon);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.tenantId = :tenantId " +
           "AND c.createdAt >= :from AND c.createdAt <= :to")
    Long countNewByPeriod(@Param("tenantId") Long tenantId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.tenantId = :tenantId " +
           "AND (c.sonZiyaret IS NULL OR c.sonZiyaret < :cutoff) AND c.karaListedeMi = false")
    Long countChurnRisk(@Param("tenantId") Long tenantId,
                         @Param("cutoff") LocalDateTime cutoff);

    Long countByTenantId(Long tenantId);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.tenantId = :tenantId AND c.karaListedeMi = true")
    Long countBlacklisted(@Param("tenantId") Long tenantId);
}
