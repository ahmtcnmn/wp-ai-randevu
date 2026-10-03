package com.appointflow.repository;

import com.appointflow.entity.Hizmet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HizmetRepository extends JpaRepository<Hizmet, Long> {
    List<Hizmet> findByTenantId(Long tenantId);
    List<Hizmet> findByTenantIdAndAktifTrue(Long tenantId);
    List<Hizmet> findByKategoriId(Long kategoriId);

    long countByKategoriIdAndAktifTrue(Long kategoriId);
}
