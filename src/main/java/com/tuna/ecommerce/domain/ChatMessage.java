package com.tuna.ecommerce.domain;

import java.time.Instant;

import com.tuna.ecommerce.ultil.SecurityUtil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_messages", indexes = {
    @jakarta.persistence.Index(name = "idx_sender", columnList = "senderEmail"),
    @jakarta.persistence.Index(name = "idx_receiver", columnList = "receiverEmail"),
    @jakarta.persistence.Index(name = "idx_chat_pair", columnList = "senderEmail, receiverEmail")
})
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String senderEmail;

    private String receiverEmail;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    private Instant timestamp;

    private boolean isRead = false;

    @PrePersist
    public void handleBeforeCreate() {
        this.timestamp = Instant.now();
    }
}
