package com.tuna.ecommerce.domain.response.chat;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResChatMessageDTO {
    private String senderEmail;
    private String receiverEmail;
    private String content;
    private Instant timestamp;
    private long unreadCount;

    public ResChatMessageDTO(String senderEmail, String receiverEmail, String content, Instant timestamp) {
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.content = content;
        this.timestamp = timestamp;
        this.unreadCount = 0;
    }

    public ResChatMessageDTO(String senderEmail, String receiverEmail, String content, Instant timestamp, long unreadCount) {
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.content = content;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
    }
}
