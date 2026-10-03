package com.appointflow.repository;

import com.appointflow.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findByTenantIdOrderBySiraAsc(Long tenantId);
}
