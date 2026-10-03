package com.appointflow.notification.service;

import com.appointflow.common.ApiException;
import com.appointflow.entity.Kullanici;
import com.appointflow.notification.dto.NotificationResponse;
import com.appointflow.notification.entity.Notification;
import com.appointflow.notification.repository.NotificationRepository;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final KullaniciRepository kullaniciRepository;

    @Transactional
    public Notification create(Long tenantId, Long userId, String tip, String baslik, String icerik, String link) {
        Notification n = Notification.builder()
                .tenantId(tenantId)
                .userId(userId)
                .tip(tip)
                .baslik(baslik)
                .icerik(icerik)
                .link(link)
                .build();
        return notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(String email, int page, int size) {
        Kullanici me = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(me.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        Kullanici me = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        return notificationRepository.countByUserIdAndOkunduFalse(me.getId());
    }

    @Transactional
    public NotificationResponse markRead(String email, Long notificationId) {
        Kullanici me = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Bildirim bulunamadi."));
        if (!n.getUserId().equals(me.getId())) {
            throw ApiException.forbidden("Bu bildirime erisim yetkiniz yok.");
        }
        if (Boolean.FALSE.equals(n.getOkundu())) {
            n.setOkundu(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
        return toResponse(n);
    }

    @Transactional
    public int markAllRead(String email) {
        Kullanici me = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        return notificationRepository.markAllReadByUserId(me.getId(), LocalDateTime.now());
    }

    @Transactional
    public void delete(String email, Long notificationId) {
        Kullanici me = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Bildirim bulunamadi."));
        if (!n.getUserId().equals(me.getId())) {
            throw ApiException.forbidden("Bu bildirime erisim yetkiniz yok.");
        }
        notificationRepository.delete(n);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .tip(n.getTip())
                .baslik(n.getBaslik())
                .icerik(n.getIcerik())
                .link(n.getLink())
                .okundu(n.getOkundu())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
