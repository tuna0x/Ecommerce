package com.tuna.ecommerce.domain.response.product;

import java.math.BigDecimal;
import java.util.List;

import org.w3c.dom.Attr;

import com.tuna.ecommerce.domain.Category;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private int stock;
    private String image;
    private CategoryInner category;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInner {
        private Long id;
        private String name;
    }

}

