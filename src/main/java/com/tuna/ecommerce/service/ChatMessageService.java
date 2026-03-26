package com.tuna.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.ChatMessage;
import com.tuna.ecommerce.domain.response.chat.ResChatMessageDTO;
import com.tuna.ecommerce.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(String sender, String receiver, String content) {
        ChatMessage message = new ChatMessage();
        message.setSenderEmail(sender);
        message.setReceiverEmail(receiver);
        message.setContent(content);
        return chatMessageRepository.save(message);
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
}
