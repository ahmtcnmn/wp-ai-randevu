package com.appointflow.conversation.repository;

import com.appointflow.conversation.entity.ConversationHandoff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationHandoffRepository extends JpaRepository<ConversationHandoff, Long> {
    List<ConversationHandoff> findByConversationIdOrderByDevralmaZamaniDesc(Long conversationId);
}
