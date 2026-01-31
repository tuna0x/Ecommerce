package com.tuna.ecommerce.domain.request.promotion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqAssignPromotionDTO {
    private Long productId;
    private Long promotionId;
}
