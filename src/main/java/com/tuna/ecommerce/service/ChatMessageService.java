package com.tuna.ecommerce.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.ChatMessage;
import com.tuna.ecommerce.domain.response.chat.ResChatMessageDTO;
import com.tuna.ecommerce.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final long SESSION_COOLDOWN_MINUTES = 30;
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String AUTO_REPLY_CONTENT = "Cảm ơn bạn đã liên hệ với Bông Cosmetic! 💖 Chúng tôi đã nhận được tin nhắn của bạn và sẽ phản hồi sớm nhất có thể. Trong lúc chờ đợi, bạn có thể tham khảo các sản phẩm hoặc hỏi trợ lý Bông AI ở tab bên cạnh nhé! ✨";

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(String sender, String receiver, String content) {
        ChatMessage message = new ChatMessage();
        message.setSenderEmail(sender);
        message.setReceiverEmail(receiver);
        message.setContent(content);
        return chatMessageRepository.save(message);
    }

    public ChatMessage checkAndGenerateAutoReply(String sender, String receiver) {
        // Chỉ tự động trả lời khi khách hàng (không phải admin) nhắn tin cho admin
        if (receiver.equalsIgnoreCase(ADMIN_EMAIL) && !sender.equalsIgnoreCase(ADMIN_EMAIL)) {
            // Lấy 2 tin nhắn gần nhất trong lịch sử chat
            Pageable limitTwo = PageRequest.of(0, 2);
            Page<ChatMessage> lastMessages = chatMessageRepository.findChatHistory(sender, receiver, limitTwo);
            
            boolean needAutoReply = false;
            List<ChatMessage> contentList = lastMessages.getContent();
            
            if (contentList.size() <= 1) {
                // Chỉ có tin nhắn vừa gửi (hoặc không có gì), tức là phiên mới tinh
                needAutoReply = true;
            } else {
                // Tin nhắn thứ hai trong danh sách chính là tin nhắn trước đó của phiên cũ
                ChatMessage prevMsg = contentList.get(1);
                Instant prevTimestamp = prevMsg.getTimestamp();
                if (prevTimestamp == null) {
                    needAutoReply = true;
                } else {
                    long minutesElapsed = Duration.between(prevTimestamp, Instant.now()).toMinutes();
                    if (minutesElapsed >= SESSION_COOLDOWN_MINUTES) {
                        needAutoReply = true;
                    }
                }
            }
            
            if (needAutoReply) {
                // Tạo và lưu tin nhắn tự động chăm sóc khách hàng
                ChatMessage autoReply = new ChatMessage();
                autoReply.setSenderEmail(ADMIN_EMAIL);
                autoReply.setReceiverEmail(sender);
                autoReply.setContent(AUTO_REPLY_CONTENT);
                return chatMessageRepository.save(autoReply);
            }
        }
        return null;
    }

    public List<ResChatMessageDTO> getChatHistory(String email1, String email2, Pageable pageable) {
        Page<ChatMessage> messages = chatMessageRepository.findChatHistory(email1, email2, pageable);
        return messages.getContent().stream().map(m -> new ResChatMessageDTO(
                m.getSenderEmail(),
                m.getReceiverEmail(),
                m.getContent(),
                m.getTimestamp()
        )).collect(Collectors.toList());
    }

    public List<ResChatMessageDTO> getRecentConversations(String email) {
        List<ChatMessage> messages = chatMessageRepository.findRecentConversations(email);
        return messages.stream().map(m -> {
            String partner = m.getSenderEmail().equalsIgnoreCase(email) ? m.getReceiverEmail() : m.getSenderEmail();
            long unreadCount = chatMessageRepository.countUnreadMessages(partner, email);
            return new ResChatMessageDTO(
                    m.getSenderEmail(),
                    m.getReceiverEmail(),
                    m.getContent(),
                    m.getTimestamp(),
                    unreadCount
            );
        }).collect(Collectors.toList());
    }

    public void markAsRead(String sender, String receiver) {
        chatMessageRepository.markMessagesAsRead(sender, receiver);
    }
}
