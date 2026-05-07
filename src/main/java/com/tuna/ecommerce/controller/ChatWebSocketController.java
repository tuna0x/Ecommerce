package com.tuna.ecommerce.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.tuna.ecommerce.domain.ChatMessage;
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
            return;
        }

        String senderEmail = principal.getName().toLowerCase();
        String receiverEmail = chatMessageDTO.getReceiverEmail().toLowerCase();
        chatMessageDTO.setSenderEmail(senderEmail);
        chatMessageDTO.setReceiverEmail(receiverEmail);

        // Lưu vào database
        chatMessageService.saveMessage(
                senderEmail,
                receiverEmail,
                chatMessageDTO.getContent()
        );

        // Gửi tới người nhận qua queue: /user/{receiverEmail}/queue/messages
        messagingTemplate.convertAndSendToUser(
                receiverEmail,
                "/queue/messages",
                chatMessageDTO
        );
        
        // Gửi ngược lại cho chính người gửi để cập nhật UI (nếu cần)
        messagingTemplate.convertAndSendToUser(
                senderEmail,
                "/queue/messages",
                chatMessageDTO
        );

        // Kiểm tra và tạo tin nhắn tự động chăm sóc khách hàng
        ChatMessage autoReply = chatMessageService.checkAndGenerateAutoReply(senderEmail, receiverEmail);
        if (autoReply != null) {
            ResChatMessageDTO autoReplyDTO = new ResChatMessageDTO(
                    autoReply.getSenderEmail(),
                    autoReply.getReceiverEmail(),
                    autoReply.getContent(),
                    autoReply.getTimestamp()
            );

            // Gửi tin nhắn tự động tới khách hàng (người nhận tin tự động)
            messagingTemplate.convertAndSendToUser(
                    autoReply.getReceiverEmail(),
                    "/queue/messages",
                    autoReplyDTO
            );

            // Gửi tin nhắn tự động tới Admin (người gửi tin tự động) để cập nhật giao diện Admin
            messagingTemplate.convertAndSendToUser(
                    autoReply.getSenderEmail(),
                    "/queue/messages",
                    autoReplyDTO
            );
        }
    }
}
