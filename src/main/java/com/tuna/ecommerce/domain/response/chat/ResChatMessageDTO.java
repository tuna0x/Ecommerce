package com.tuna.ecommerce.domain.response.chat;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResChatMessageDTO {
    private String senderEmail;
    private String receiverEmail;
    private String content;
    private Instant timestamp;
}
