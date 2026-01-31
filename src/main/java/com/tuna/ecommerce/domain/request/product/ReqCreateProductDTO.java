package com.tuna.ecommerce.domain.request.product;

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
public class ReqCreateProductDTO {
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private int stock;
    private String image;
    private List<Long> attributeValue;
    private Long categoryId;
}
