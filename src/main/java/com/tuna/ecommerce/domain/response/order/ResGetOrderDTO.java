package com.tuna.ecommerce.domain.response.order;

import java.math.BigDecimal;

import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResGetOrderDTO {
    private long id;
    private UserInner user;
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status;

    @Enumerated(EnumType.STRING)
    private PaymentStatusEnum paymentStatus;
    private String ShippingAddress;
    private String transactionID;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInner {
    private Long id;
    private String name;
    private String email;
    }
}
