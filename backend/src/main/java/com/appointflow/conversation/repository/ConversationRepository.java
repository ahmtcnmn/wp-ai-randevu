package com.appointflow.conversation.repository;

import com.appointflow.conversation.entity.Conversation;
import com.appointflow.conversation.entity.Conversation.ConversationDurum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByTenantIdAndDurumIn(Long tenantId, List<ConversationDurum> durumlar);
    List<Conversation> findByTenantIdOrderByOlusturmaTarihiDesc(Long tenantId);
    Optional<Conversation> findByTenantIdAndCustomerPhoneAndDurumNot(Long tenantId, String phone, ConversationDurum durum);
    List<Conversation> findByTenantIdAndDurum(Long tenantId, ConversationDurum durum);
}
