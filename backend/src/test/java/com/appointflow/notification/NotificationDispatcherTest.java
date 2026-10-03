package com.appointflow.notification;

import com.appointflow.device.service.ExpoPushService;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.Role;
import com.appointflow.notification.dispatcher.NotificationDispatcher;
import com.appointflow.notification.entity.Notification;
import com.appointflow.notification.service.NotificationService;
import com.appointflow.repository.KullaniciRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock NotificationService notificationService;
    @Mock ExpoPushService expoPushService;
    @Mock KullaniciRepository kullaniciRepository;

    @InjectMocks NotificationDispatcher dispatcher;

    @Test
    void notifyUser_createsDbRecord_andTriggersPush() {
        when(notificationService.create(eq(1L), eq(2L), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new Notification());

        dispatcher.notifyUser(1L, 2L, "APPOINTMENT_CREATED", "Yeni randevu", "Ali Veli", "/randevular/5");

        verify(notificationService).create(1L, 2L, "APPOINTMENT_CREATED", "Yeni randevu", "Ali Veli", "/randevular/5");
        verify(expoPushService).pushToUser(2L, "Yeni randevu", "Ali Veli", "/randevular/5");
    }

    @Test
    void notifyOwner_noOwnerExists_silentlySkips() {
        when(kullaniciRepository.findOwnerByTenantId(1L)).thenReturn(Optional.empty());

        dispatcher.notifyOwner(1L, "PAYMENT_SUCCESS", "OK", "Ok", "/billing");

        verify(notificationService, never()).create(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString());
        verify(expoPushService, never()).pushToUser(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void notifyOwner_ownerExists_dispatches() {
        Kullanici owner = Kullanici.builder().id(99L).rol(Role.OWNER).build();
        when(kullaniciRepository.findOwnerByTenantId(1L)).thenReturn(Optional.of(owner));
        when(notificationService.create(anyLong(), eq(99L), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new Notification());

        dispatcher.notifyOwner(1L, "PAYMENT_SUCCESS", "OK", "Ok", "/billing");

        verify(notificationService).create(1L, 99L, "PAYMENT_SUCCESS", "OK", "Ok", "/billing");
        verify(expoPushService).pushToUser(99L, "OK", "Ok", "/billing");
    }

    @Test
    void notifyAdmins_multipleAdmins_dispatchesToEach() {
        Kullanici owner = Kullanici.builder().id(99L).rol(Role.OWNER).build();
        Kullanici admin = Kullanici.builder().id(100L).rol(Role.ADMIN).build();
        when(kullaniciRepository.findAdminsByTenantId(1L)).thenReturn(List.of(owner, admin));
        when(notificationService.create(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new Notification());

        dispatcher.notifyAdmins(1L, "APPOINTMENT_CREATED", "Yeni", "Ali", "/r/1");

        verify(notificationService, times(2)).create(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString());
        verify(expoPushService).pushToUser(99L, "Yeni", "Ali", "/r/1");
        verify(expoPushService).pushToUser(100L, "Yeni", "Ali", "/r/1");
    }

    @Test
    void notifyUser_serviceThrows_swallowsExceptionToProtectMainFlow() {
        when(notificationService.create(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DB down"));

        // Should NOT throw — dispatcher swallow eder
        dispatcher.notifyUser(1L, 2L, "X", "t", "b", "/l");
    }
}
