package com.appointflow.repository;

import com.appointflow.entity.ProductServiceSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductServiceSuggestionRepository extends JpaRepository<ProductServiceSuggestion, Long> {
    List<ProductServiceSuggestion> findByProductId(Long productId);
    void deleteByProductId(Long productId);

    /** Bir hizmet için önerilen (aiOneriAktif=true) aktif ürünler. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT s FROM ProductServiceSuggestion s " +
            "WHERE s.hizmet.id = :hizmetId " +
            "AND s.product.aktif = true " +
            "AND s.product.aiOneriAktif = true")
    List<ProductServiceSuggestion> findByHizmetIdAndProductActive(@org.springframework.data.repository.query.Param("hizmetId") Long hizmetId);
}
