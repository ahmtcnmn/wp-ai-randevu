package com.appointflow.contact.repository;

import com.appointflow.contact.entity.ContactRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {

    Page<ContactRequest> findByDurumOrderByCreatedAtDesc(String durum, Pageable pageable);

    long countByDurum(String durum);
}
