package com.appointflow.whatsapp.repository;

import com.appointflow.whatsapp.entity.WhatsappConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsappConfigRepository extends JpaRepository<WhatsappConfig, Long> {
    Optional<WhatsappConfig> findByTenantId(Long tenantId);
    Optional<WhatsappConfig> findByPhoneNumberId(String phoneNumberId);
}
