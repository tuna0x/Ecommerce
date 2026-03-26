package com.tuna.ecommerce.domain.request.notification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqSendNotificationDTO {
    private Long userId;
    private String title;
    private String message;
    private String type;
}
