package com.appointflow.repository;

import com.appointflow.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTenantId(Long tenantId);
    List<Product> findByTenantIdAndAktifTrue(Long tenantId);
}
