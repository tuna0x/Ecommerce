package com.tuna.ecommerce.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.tuna.ecommerce.domain.response.chat.ResChatMessageDTO;
import com.tuna.ecommerce.service.ChatMessageService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    // Client gửi tin nhắn tới: /app/chat.send
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ResChatMessageDTO chatMessageDTO, Principal principal) {
        if (principal == null) {
            System.err.println(">>> WebSocket Error: Principal is null. Ensure UserInterceptor is working.");
            return;
        }

        String senderEmail = principal.getName();
        System.out.println(">>> WebSocket Message received from: " + senderEmail + " to: " + chatMessageDTO.getReceiverEmail());
        chatMessageDTO.setSenderEmail(senderEmail);

        // Lưu vào database
        chatMessageService.saveMessage(
                senderEmail,
                chatMessageDTO.getReceiverEmail(),
                chatMessageDTO.getContent()
        );

        // Gửi tới người nhận qua queue: /user/{receiverEmail}/queue/messages
        System.out.println(">>> Sending WebSocket message to receiver: " + chatMessageDTO.getReceiverEmail());
        messagingTemplate.convertAndSendToUser(
                chatMessageDTO.getReceiverEmail(),
                "/queue/messages",
                chatMessageDTO
        );
        
        // Gửi ngược lại cho chính người gửi để cập nhật UI (nếu cần)
        messagingTemplate.convertAndSendToUser(
                senderEmail,
                "/queue/messages",
                chatMessageDTO
        );
    }
}
