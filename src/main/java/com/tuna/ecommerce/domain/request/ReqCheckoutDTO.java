package com.tuna.ecommerce.domain.request;

import java.util.List;

import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReqCheckoutDTO {
        private String shippingAddress;
    private List<Long> cartItemIDs;
    private PaymentMethodEnum paymentMethodEnum;
}
