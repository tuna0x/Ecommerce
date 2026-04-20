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
    private BigDecimal originalPrice;
    private double costPrice;
    private int stock;
    private List<Long> attributeValue;
    private Long categoryId;
    private Long brandId;
    private List<VariantDTO> variants;

    @Getter
    @Setter
    public static class VariantDTO {
        private String sku;
        private BigDecimal price;
        private double costPrice;
        private int stock;
        private double weight;
        private Long productImageId;
        private Integer productImageIndex;
        private List<Long> attributeValues;
    }
}
