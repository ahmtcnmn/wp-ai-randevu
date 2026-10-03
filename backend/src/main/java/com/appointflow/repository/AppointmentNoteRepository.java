package com.appointflow.repository;

import com.appointflow.entity.AppointmentNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentNoteRepository extends JpaRepository<AppointmentNote, Long> {
    List<AppointmentNote> findByRandevuIdOrderByCreatedAtAsc(Long randevuId);
}
