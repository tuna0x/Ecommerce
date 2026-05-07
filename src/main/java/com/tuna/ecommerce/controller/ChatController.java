package com.tuna.ecommerce.controller;

import com.tuna.ecommerce.domain.request.chat.ReqChatDTO;
import com.tuna.ecommerce.domain.response.chat.ResChatDTO;
import com.tuna.ecommerce.domain.response.chat.ResChatMessageDTO;
import com.tuna.ecommerce.service.ChatMessageService;
import com.tuna.ecommerce.service.GeminiService;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.anotation.APIMessage;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final GeminiService geminiService;
    private final ChatMessageService chatMessageService;

    @PostMapping("/chat")
    @APIMessage("Nhận phản hồi từ Chatbot thành công")
    public ResponseEntity<ResChatDTO> chat(@RequestBody ReqChatDTO request) {
        String response = geminiService.getChatResponse(
            request.getMessage(), 
            request.getHistory(),
            request.getSessionId(),
            request.getDeviceType(),
            request.getPageUrl()
        );
        return ResponseEntity.ok().body(new ResChatDTO(response));
    }

    @GetMapping("/chat/history")
    @APIMessage("Lấy lịch sử chat thành công")
    public ResponseEntity<List<ResChatMessageDTO>> getHistory(
            @RequestParam String participant,
            Pageable pageable) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("");
        List<ResChatMessageDTO> history = chatMessageService.getChatHistory(currentUser, participant, pageable);
        return ResponseEntity.ok().body(history);
    }
 
    @GetMapping("/chat/conversations")
    @APIMessage("Lấy danh sách hội thoại thành công")
    public ResponseEntity<List<ResChatMessageDTO>> getConversations() {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("");
        List<ResChatMessageDTO> conversations = chatMessageService.getRecentConversations(currentUser);
        return ResponseEntity.ok().body(conversations);
    }

    @PostMapping("/chat/read")
    @APIMessage("Đánh dấu tin nhắn đã đọc thành công")
    public ResponseEntity<Void> markAsRead(@RequestParam String participant) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("");
        chatMessageService.markAsRead(participant, currentUser);
        return ResponseEntity.ok().build();
    }
}
