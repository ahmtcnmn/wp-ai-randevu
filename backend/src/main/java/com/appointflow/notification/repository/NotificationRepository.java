package com.appointflow.notification.repository;

import com.appointflow.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndOkunduFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.okundu = true, n.readAt = :now WHERE n.userId = :userId AND n.okundu = false")
    int markAllReadByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
