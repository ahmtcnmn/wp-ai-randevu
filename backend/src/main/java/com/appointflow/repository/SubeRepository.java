package com.appointflow.repository;

import com.appointflow.entity.Sube;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubeRepository extends JpaRepository<Sube, Long> {
    List<Sube> findByTenantId(Long tenantId);
    long countByTenantId(Long tenantId);
}
