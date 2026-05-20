package com.tuna.ecommerce.domain.response.order;

import com.tuna.ecommerce.ultil.constant.CheckoutStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResCheckoutAsyncDTO {
    private String checkoutId;
    private CheckoutStatusEnum status;
    private Long orderId;
    private String paymentUrl;
    private String message;
}
