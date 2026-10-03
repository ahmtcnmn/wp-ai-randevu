package com.appointflow.notification.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.notification.dto.NotificationResponse;
import com.appointflow.notification.dto.UnreadCountResponse;
import com.appointflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> list(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getMyNotifications(auth.getName(), page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount(Authentication auth) {
        long count = notificationService.getUnreadCount(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountResponse(count)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markRead(auth.getName(), id)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllRead(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAllRead(auth.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication auth, @PathVariable Long id) {
        notificationService.delete(auth.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
