package com.tuna.ecommerce.domain.request.tracking;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqBehaviorDTO {
    private String actionType;
    private String metadata;
    private String sessionId;
    private String deviceType;
    private String referrer;
    private String pageUrl;
}
