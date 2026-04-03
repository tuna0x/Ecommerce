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
    private BigDecimal discountPrice;
    private BigDecimal finalPrice;
    private int stock;
    private int weight;
    private String thumbnail;
    private List<String> image;
    private CategoryInner category;
    private BrandInner brand;
    private List<ValueInner> attributeValue;
    private Double averageRating;
    private Long reviewCount;
    private int soldCount;
    private List<ProductVariantInner> variants;


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductVariantInner {
        private Long id;
        private String sku;
        private BigDecimal price;
        private int stock;
        private double weight;
        private List<VariantAttributeInner> variantAttributes;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VariantAttributeInner {
        private String name;
        private String value;
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInner {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BrandInner {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValueInner {
        private Long id;
        private String value;
        private Long attributeId;
        private String attributeName;
    }


}

