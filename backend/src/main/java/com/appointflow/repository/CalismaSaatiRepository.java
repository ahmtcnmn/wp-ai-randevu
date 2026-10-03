package com.appointflow.repository;

import com.appointflow.entity.CalismaSaati;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CalismaSaatiRepository extends JpaRepository<CalismaSaati, Long> {
    List<CalismaSaati> findByUzmanId(Long uzmanId);
    Optional<CalismaSaati> findByUzmanIdAndGunOfWeek(Long uzmanId, Integer gunOfWeek);
}
