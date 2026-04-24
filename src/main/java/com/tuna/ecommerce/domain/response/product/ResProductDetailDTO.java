package com.tuna.ecommerce.domain.response.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResProductDetailDTO {
    private Long id;
    private String description;
    private String ingredient;
    private String usageGuide;
    private String specification;
    private ProductInner product;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductInner {
        private Long id;
        private String name;
        private Object image;
        private Object brand;
    }
}
