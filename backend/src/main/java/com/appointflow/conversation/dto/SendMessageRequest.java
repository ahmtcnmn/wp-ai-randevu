package com.appointflow.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {
    @NotBlank(message = "Mesaj içeriği boş olamaz")
    private String icerik;

    /**
     * Opsiyonel — frontend tutarliligi icin. Su an sadece OUTBOUND destekleniyor;
     * baska bir deger gelirse 400 doneriz. null veya OUTBOUND'da STAFF mesaji olarak persist edilir.
     */
    private String tur;
}
