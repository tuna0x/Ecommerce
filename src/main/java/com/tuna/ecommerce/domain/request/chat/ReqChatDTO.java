package com.tuna.ecommerce.domain.request.chat;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqChatDTO {
    private String message;
    private List<ChatMessageDTO> history;
    private String sessionId;
    private String deviceType;
    private String pageUrl;
}
