package com.appointflow.repository;

import com.appointflow.entity.CustomerTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerTagRepository extends JpaRepository<CustomerTag, Long> {
    List<CustomerTag> findByCustomerId(Long customerId);
}
