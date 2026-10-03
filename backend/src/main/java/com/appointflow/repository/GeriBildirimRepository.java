package com.appointflow.repository;

import com.appointflow.entity.GeriBildirim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GeriBildirimRepository extends JpaRepository<GeriBildirim, Long> {
    Optional<GeriBildirim> findByRandevuId(Long randevuId);
    List<GeriBildirim> findByRandevuUzmanId(Long uzmanId);
    List<GeriBildirim> findBySikayetVarmiTrue();
}
