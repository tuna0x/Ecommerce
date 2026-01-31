package com.tuna.ecommerce.domain.response.promotion;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResPriceResultDTO {
    private BigDecimal originalPrice;
    private BigDecimal discountPrice;
    private BigDecimal finalPrice;
}
