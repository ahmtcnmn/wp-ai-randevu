package com.appointflow.device.repository;

import com.appointflow.device.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserIdAndExpoPushToken(Long userId, String expoPushToken);

    List<UserDevice> findByUserId(Long userId);
}
