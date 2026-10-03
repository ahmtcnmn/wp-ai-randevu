package com.appointflow.repository;

import com.appointflow.entity.AppointmentServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentServiceRepository extends JpaRepository<AppointmentServiceEntity, Long> {
    List<AppointmentServiceEntity> findByRandevuId(Long randevuId);
}
