package com.appointflow.repository;

import com.appointflow.entity.StaffService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffServiceRepository extends JpaRepository<StaffService, Long> {
    List<StaffService> findByKullaniciId(Long kullaniciId);
    List<StaffService> findByHizmetId(Long hizmetId);
    void deleteByKullaniciIdAndHizmetId(Long kullaniciId, Long hizmetId);
    boolean existsByKullaniciIdAndHizmetId(Long kullaniciId, Long hizmetId);
    void deleteByKullaniciId(Long kullaniciId);
}
