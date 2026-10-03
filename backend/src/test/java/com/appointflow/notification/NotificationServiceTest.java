package com.appointflow.notification;

import com.appointflow.common.ApiException;
import com.appointflow.entity.Kullanici;
import com.appointflow.notification.entity.Notification;
import com.appointflow.notification.repository.NotificationRepository;
import com.appointflow.notification.service.NotificationService;
import com.appointflow.repository.KullaniciRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock KullaniciRepository kullaniciRepository;

    @InjectMocks NotificationService service;

    @Test
    void create_persistsWithFields() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        Notification result = service.create(1L, 2L, "APPOINTMENT_CREATED", "Yeni", "Ali Veli", "/r/5");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTenantId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getTip()).isEqualTo("APPOINTMENT_CREATED");
        assertThat(result.getOkundu()).isFalse();
    }

    @Test
    void markRead_otherUser_throws403() {
        Kullanici me = Kullanici.builder().id(1L).email("u@test.com").build();
        Notification othersNotif = Notification.builder()
                .id(50L).userId(999L).okundu(false).build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(me));
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(othersNotif));

        assertThatThrownBy(() -> service.markRead("u@test.com", 50L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("erisim yetkiniz yok");
    }

    @Test
    void markRead_owned_setsReadAt() {
        Kullanici me = Kullanici.builder().id(1L).email("u@test.com").build();
        Notification mine = Notification.builder().id(50L).userId(1L).okundu(false).build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(me));
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(mine));

        service.markRead("u@test.com", 50L);

        assertThat(mine.getOkundu()).isTrue();
        assertThat(mine.getReadAt()).isNotNull();
        verify(notificationRepository).save(mine);
    }

    @Test
    void markRead_alreadyRead_doesNotResave() {
        Kullanici me = Kullanici.builder().id(1L).build();
        Notification alreadyRead = Notification.builder().id(50L).userId(1L).okundu(true).build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(me));
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(alreadyRead));

        service.markRead("u@test.com", 50L);

        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }
}
