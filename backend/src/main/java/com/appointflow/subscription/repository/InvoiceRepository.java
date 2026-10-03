package com.appointflow.subscription.repository;

import com.appointflow.subscription.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    Optional<Invoice> findByIyzicoPaymentId(String iyzicoPaymentId);
    Optional<Invoice> findByIyzicoToken(String iyzicoToken);
}
