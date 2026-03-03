package com.tuna.ecommerce.domain.request.productDetail;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqCreateProductDetailDTO {
    private String description;
    private String ingredient;
    private String usageGuide;
    private String specification;
    private Long productId;
}
