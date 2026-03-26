package com.tuna.ecommerce.domain.response.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResPaymentVNPAYDTO {
    private String code;
    private String message;
    private String paymentUrl;
}
