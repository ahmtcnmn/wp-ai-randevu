package com.appointflow.conversation.service;

import com.appointflow.common.ApiException;
import com.appointflow.conversation.dto.ConversationResponse;
import com.appointflow.conversation.dto.MessageResponse;
import com.appointflow.conversation.entity.Conversation;
import com.appointflow.conversation.entity.Conversation.ConversationDurum;
import com.appointflow.conversation.entity.ConversationHandoff;
import com.appointflow.conversation.entity.Message;
import com.appointflow.conversation.entity.Message.SenderType;
import com.appointflow.conversation.repository.ConversationHandoffRepository;
import com.appointflow.conversation.repository.ConversationRepository;
import com.appointflow.conversation.repository.MessageRepository;
import com.appointflow.notification.dispatcher.NotificationDispatcher;
import com.appointflow.tenant.TenantContext;
import com.appointflow.whatsapp.service.WhatsappMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationHandoffRepository handoffRepository;
    private final WhatsappMessageService whatsappMessageService;
    private final NotificationDispatcher notificationDispatcher;

    @Transactional(readOnly = true)
    public List<ConversationResponse> getActiveConversations() {
        Long tenantId = TenantContext.getTenantId();
        List<ConversationDurum> activeDurumlar = List.of(
                ConversationDurum.ACTIVE,
                ConversationDurum.WAITING,
                ConversationDurum.HUMAN_ACTIVE
        );
        return conversationRepository.findByTenantIdAndDurumIn(tenantId, activeDurumlar)
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationHistory() {
        Long tenantId = TenantContext.getTenantId();
        return conversationRepository.findByTenantIdOrderByOlusturmaTarihiDesc(tenantId)
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getById(Long id) {
        return toConversationResponse(findConversation(id));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId) {
        findConversation(conversationId); // Tenant kontrolu
        return messageRepository.findByConversationIdOrderByOlusturmaTarihiAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public ConversationResponse takeover(Long conversationId, Long userId, String userName) {
        Conversation conv = findConversation(conversationId);

        conv.setDurum(ConversationDurum.HUMAN_ACTIVE);
        conv.setAssignedUserId(userId);
        conversationRepository.save(conv);

        // Handoff kaydi olustur
        ConversationHandoff handoff = ConversationHandoff.builder()
                .conversationId(conversationId)
                .devralanUserId(userId)
                .devralanAd(userName)
                .neden("Manuel devralma")
                .build();
        handoffRepository.save(handoff);

        log.info("Konusma devralindi — conversation: {}, user: {}", conversationId, userId);
        return toConversationResponse(conv);
    }

    @Transactional
    public ConversationResponse releaseToAi(Long conversationId) {
        Conversation conv = findConversation(conversationId);
        conv.setDurum(ConversationDurum.ACTIVE);
        conv.setAssignedUserId(null);
        conversationRepository.save(conv);

        log.info("Konusma AI'a birakildi — conversation: {}", conversationId);
        return toConversationResponse(conv);
    }

    @Transactional
    public ConversationResponse assignToUser(Long conversationId, Long userId) {
        Conversation conv = findConversation(conversationId);
        conv.setDurum(ConversationDurum.HUMAN_ACTIVE);
        conv.setAssignedUserId(userId);
        conversationRepository.save(conv);

        log.info("Konusma atandi — conversation: {}, user: {}", conversationId, userId);
        return toConversationResponse(conv);
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, String icerik, Long userId, String userName) {
        Conversation conv = findConversation(conversationId);

        // Mesaji kaydet
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderType(SenderType.STAFF)
                .senderId(userId)
                .senderName(userName)
                .icerik(icerik)
                .build();
        Message saved = messageRepository.save(message);

        // Konusma zamanini guncelle
        conv.setSonMesajZamani(LocalDateTime.now());
        conversationRepository.save(conv);

        // WhatsApp'a gonder
        if (conv.getCustomerPhone() != null) {
            try {
                whatsappMessageService.sendTextMessage(conv.getTenantId(), conv.getCustomerPhone(), icerik);
            } catch (Exception e) {
                log.error("WhatsApp mesaj gonderilemedi: {}", e.getMessage());
            }
        }

        return toMessageResponse(saved);
    }

    @Transactional
    public ConversationResponse closeConversation(Long conversationId) {
        Conversation conv = findConversation(conversationId);
        conv.setDurum(ConversationDurum.CLOSED);
        conv.setKapatmaTarihi(LocalDateTime.now());
        conversationRepository.save(conv);

        log.info("Konusma kapatildi — conversation: {}", conversationId);
        return toConversationResponse(conv);
    }

    /**
     * WhatsApp'tan gelen mesaj icin conversation bul veya olustur.
     */
    @Transactional
    public Conversation findOrCreateConversation(Long tenantId, Long customerId, String phone, String customerName) {
        // Acik konusma varsa onu kullan
        return conversationRepository
                .findByTenantIdAndCustomerPhoneAndDurumNot(tenantId, phone, ConversationDurum.CLOSED)
                .orElseGet(() -> {
                    Conversation conv = Conversation.builder()
                            .tenantId(tenantId)
                            .customerId(customerId)
                            .customerPhone(phone)
                            .customerName(customerName)
                            .durum(ConversationDurum.ACTIVE)
                            .build();
                    Conversation saved = conversationRepository.save(conv);
                    notificationDispatcher.notifyAdmins(tenantId,
                            NotificationDispatcher.Tip.WHATSAPP_HANDOFF,
                            "Yeni WhatsApp konusmasi",
                            String.format("%s (%s) yeni mesaj gonderdi.",
                                    customerName != null ? customerName : "Bilinmeyen",
                                    phone),
                            "/whatsapp/" + saved.getId());
                    return saved;
                });
    }

    /**
     * Gelen mesaji kaydet.
     */
    @Transactional
    public void saveIncomingMessage(Long conversationId, String icerik, String whatsappMessageId) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderType(SenderType.CUSTOMER)
                .icerik(icerik)
                .whatsappMessageId(whatsappMessageId)
                .build();
        messageRepository.save(message);
    }

    /**
     * AI yanitini kaydet.
     */
    @Transactional
    public void saveAiResponse(Long conversationId, String icerik) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderType(SenderType.AI)
                .senderName("AI Asistan")
                .icerik(icerik)
                .build();
        messageRepository.save(message);
    }

    private Conversation findConversation(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return conversationRepository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> ApiException.notFound("Konuşma bulunamadı: " + id));
    }

    private ConversationResponse toConversationResponse(Conversation conv) {
        return ConversationResponse.builder()
                .id(conv.getId())
                .customerId(conv.getCustomerId())
                .customerPhone(conv.getCustomerPhone())
                .customerName(conv.getCustomerName())
                .durum(conv.getDurum().name())
                .assignedUserId(conv.getAssignedUserId())
                .kanal(conv.getKanal())
                .sonMesajZamani(conv.getSonMesajZamani())
                .olusturmaTarihi(conv.getOlusturmaTarihi())
                .kapatmaTarihi(conv.getKapatmaTarihi())
                .build();
    }

    private MessageResponse toMessageResponse(Message msg) {
        return MessageResponse.builder()
                .id(msg.getId())
                .senderType(msg.getSenderType().name())
                .senderId(msg.getSenderId())
                .senderName(msg.getSenderName())
                .mesajTipi(msg.getMesajTipi().name())
                .icerik(msg.getIcerik())
                .olusturmaTarihi(msg.getOlusturmaTarihi())
                .build();
    }
}
