package com.appointflow.conversation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignConversationRequest {
    @NotNull
    private Long userId;
    private String notlar;
}
