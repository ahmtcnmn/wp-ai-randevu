package com.appointflow.device.service;

import com.appointflow.common.ApiException;
import com.appointflow.device.dto.UserDeviceRequest;
import com.appointflow.device.dto.UserDeviceResponse;
import com.appointflow.device.entity.UserDevice;
import com.appointflow.device.repository.UserDeviceRepository;
import com.appointflow.entity.Kullanici;
import com.appointflow.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository repository;
    private final KullaniciRepository kullaniciRepository;

    @Transactional
    public UserDeviceResponse register(String email, UserDeviceRequest request) {
        Kullanici me = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        UserDevice device = repository
                .findByUserIdAndExpoPushToken(me.getId(), request.getExpoPushToken())
                .orElseGet(() -> UserDevice.builder()
                        .userId(me.getId())
                        .expoPushToken(request.getExpoPushToken())
                        .build());
        device.setPlatform(request.getPlatform());
        device.setSonKullanim(LocalDateTime.now());
        return toResponse(repository.save(device));
    }

    @Transactional
    public void delete(String email, Long deviceId) {
        Kullanici me = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        UserDevice device = repository.findById(deviceId)
                .orElseThrow(() -> ApiException.notFound("Cihaz bulunamadi."));
        if (!device.getUserId().equals(me.getId())) {
            throw ApiException.forbidden("Bu cihaza erisim yetkiniz yok.");
        }
        repository.delete(device);
    }

    @Transactional(readOnly = true)
    public List<UserDeviceResponse> myDevices(String email) {
        Kullanici me = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        return repository.findByUserId(me.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserDevice> findTokensForUser(Long userId) {
        return repository.findByUserId(userId);
    }

    private UserDeviceResponse toResponse(UserDevice d) {
        return UserDeviceResponse.builder()
                .id(d.getId())
                .expoPushToken(d.getExpoPushToken())
                .platform(d.getPlatform())
                .sonKullanim(d.getSonKullanim())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
