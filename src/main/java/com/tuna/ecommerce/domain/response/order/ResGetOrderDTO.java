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
    private BigDecimal subTotal;
    private Integer shippingFee;
    private String receiverName;
    private String phone;
    private String province;
    private String district;
    private String ward;

    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status;

    @Enumerated(EnumType.STRING)
    private PaymentStatusEnum paymentStatus;
    private String shippingAddress;
    private String paymentMethod;
    private String transactionID;
    private String paymentUrl;
    private String shippingCode;
    private java.time.Instant deliveredAt;
    private java.time.Instant confirmedAt;
    private java.time.Instant createdAt;
    private java.util.List<OrderItemInner> items;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemInner {
        private Long productId;
        private String productName;
        private String productImage;
        private int quantity;
        private java.math.BigDecimal price;
    }

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
