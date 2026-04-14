package com.tuna.ecommerce.domain.request.tracking;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqTrackingDTO {
    private String actionType;
    private String metadata;
}
