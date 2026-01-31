package com.tuna.ecommerce.domain.request.order;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateOrderDTO {
    private String shippingAdress;
    private List<Long> cartItemId;
    private String couponCode;
}
