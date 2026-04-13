package com.tuna.ecommerce.domain.request.order;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqCancelOrderDTO {
    private Long id;
    private String reason;
}
