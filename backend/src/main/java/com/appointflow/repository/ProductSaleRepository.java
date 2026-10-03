package com.appointflow.repository;

import com.appointflow.entity.ProductSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductSaleRepository extends JpaRepository<ProductSale, Long> {

    List<ProductSale> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @Query("SELECT ps FROM ProductSale ps WHERE ps.randevu.id = :randevuId")
    List<ProductSale> findByRandevuId(@Param("randevuId") Long randevuId);

    @Query("SELECT ps FROM ProductSale ps WHERE ps.product.id = :productId AND ps.tenantId = :tenantId ORDER BY ps.createdAt DESC")
    List<ProductSale> findByProductId(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    @Query("SELECT ps FROM ProductSale ps WHERE ps.tenantId = :tenantId AND ps.createdAt BETWEEN :from AND :to")
    List<ProductSale> findByTenantIdAndPeriod(@Param("tenantId") Long tenantId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
