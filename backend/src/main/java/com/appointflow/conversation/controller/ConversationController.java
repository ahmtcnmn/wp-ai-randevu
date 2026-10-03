package com.appointflow.conversation.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.conversation.dto.AssignConversationRequest;
import com.appointflow.conversation.dto.ConversationResponse;
import com.appointflow.conversation.dto.MessageResponse;
import com.appointflow.conversation.dto.SendMessageRequest;
import com.appointflow.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getActiveConversations() {
        return ResponseEntity.ok(ApiResponse.success(conversationService.getActiveConversations()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversationHistory() {
        return ResponseEntity.ok(ApiResponse.success(conversationService.getConversationHistory()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ConversationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.getById(id)));
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.getMessages(id)));
    }

    @PostMapping("/{id}/takeover")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ConversationResponse>> takeover(
            @PathVariable Long id,
            Authentication auth) {
        // Auth'dan user bilgisi al
        String userName = auth.getName();
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                conversationService.takeover(id, userId, userName)));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ConversationResponse>> releaseToAi(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.releaseToAi(id)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<ConversationResponse>> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignConversationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                conversationService.assignToUser(id, request.getUserId())));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request,
            Authentication auth) {
        if (request.getTur() != null && !"OUTBOUND".equalsIgnoreCase(request.getTur())) {
            throw com.appointflow.common.ApiException.badRequest(
                    "Gecersiz mesaj turu: " + request.getTur() + ". Yalniz OUTBOUND destekleniyor.");
        }
        String userName = auth.getName();
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                conversationService.sendMessage(id, request.getIcerik(), userId, userName)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<ConversationResponse>> closeConversation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(conversationService.closeConversation(id)));
    }

    private Long extractUserId(Authentication auth) {
        // JWT claims'den user_id al — basit yaklasim
        // Gercek implementasyonda JwtService'den alinacak
        try {
            return Long.valueOf(auth.getDetails().toString());
        } catch (Exception e) {
            return null;
        }
    }
}
