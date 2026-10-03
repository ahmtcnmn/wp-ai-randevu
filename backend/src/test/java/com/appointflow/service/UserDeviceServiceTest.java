package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.device.dto.UserDeviceRequest;
import com.appointflow.device.dto.UserDeviceResponse;
import com.appointflow.device.entity.UserDevice;
import com.appointflow.device.repository.UserDeviceRepository;
import com.appointflow.device.service.UserDeviceService;
import com.appointflow.entity.Kullanici;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock UserDeviceRepository repository;
    @Mock KullaniciRepository kullaniciRepository;

    @InjectMocks UserDeviceService service;

    @Test
    void register_newDevice_creates() {
        Kullanici me = Kullanici.builder().id(1L).email("u@test.com").build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(me));
        when(repository.findByUserIdAndExpoPushToken(1L, "ExponentPushToken[AAA]"))
                .thenReturn(Optional.empty());
        when(repository.save(any(UserDevice.class))).thenAnswer(inv -> {
            UserDevice d = inv.getArgument(0);
            d.setId(99L);
            return d;
        });

        UserDeviceRequest req = new UserDeviceRequest();
        req.setExpoPushToken("ExponentPushToken[AAA]");
        req.setPlatform("ios");

        UserDeviceResponse resp = service.register("u@test.com", req);

        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getPlatform()).isEqualTo("ios");
        assertThat(resp.getSonKullanim()).isNotNull();
    }

    @Test
    void register_existingDevice_updatesInPlace() {
        Kullanici me = Kullanici.builder().id(1L).email("u@test.com").build();
        UserDevice existing = UserDevice.builder()
                .id(42L).userId(1L).expoPushToken("ExponentPushToken[AAA]")
                .platform("android").build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(me));
        when(repository.findByUserIdAndExpoPushToken(1L, "ExponentPushToken[AAA]"))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDeviceRequest req = new UserDeviceRequest();
        req.setExpoPushToken("ExponentPushToken[AAA]");
        req.setPlatform("ios"); // platform değişti

        UserDeviceResponse resp = service.register("u@test.com", req);

        assertThat(resp.getId()).isEqualTo(42L); // aynı kayıt
        assertThat(existing.getPlatform()).isEqualTo("ios"); // güncellendi
        assertThat(existing.getSonKullanim()).isNotNull();
    }

    @Test
    void delete_otherUserDevice_throws403() {
        Kullanici me = Kullanici.builder().id(1L).email("u@test.com").build();
        UserDevice otherUserDevice = UserDevice.builder()
                .id(42L).userId(999L).expoPushToken("X").build();
        when(kullaniciRepository.findByEmail("u@test.com")).thenReturn(Optional.of(me));
        when(repository.findById(42L)).thenReturn(Optional.of(otherUserDevice));

        assertThatThrownBy(() -> service.delete("u@test.com", 42L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("erisim yetkiniz yok");

        verify(repository, never()).delete(any());
    }
}
