package com.tuna.ecommerce.domain.request.order;

import java.util.List;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqBulkUpdateStatusDTO {
    private List<Long> ids;
    private OrderStatusEnum status;
    private String reason;
}
