package com.appointflow.whatsapp.repository;

import com.appointflow.whatsapp.entity.WhatsappTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsappTemplateRepository extends JpaRepository<WhatsappTemplate, Long> {
    List<WhatsappTemplate> findByTenantId(Long tenantId);
    Optional<WhatsappTemplate> findByTenantIdAndTemplateKey(Long tenantId, String templateKey);
    List<WhatsappTemplate> findByTenantIdAndDurum(Long tenantId, WhatsappTemplate.TemplateDurum durum);
}
