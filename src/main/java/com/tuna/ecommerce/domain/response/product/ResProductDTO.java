package com.tuna.ecommerce.domain.response.product;

import java.math.BigDecimal;
import java.util.List;

import org.w3c.dom.Attr;

import com.tuna.ecommerce.domain.Category;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal discountPrice;
    private BigDecimal finalPrice;
    private int stock;
    private int reservedStock;
    private int maxStock;
    private int weight;
    private String thumbnail;
    private List<String> image;
    private List<ProductImageInner> productImages;
    private CategoryInner category;
    private BrandInner brand;
    private List<ValueInner> attributeValue;
    private Double averageRating;
    private Long reviewCount;
    private int soldCount;
    private List<ProductVariantInner> variants;
    private FlashSaleInner flashSale;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FlashSaleInner {
        private BigDecimal price;
        private int limitQuantity;
        private int soldQuantity;
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private java.time.LocalDateTime endAt;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductVariantInner {
        private Long id;
        private String sku;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private BigDecimal finalPrice;
        private int stock;
        private int reservedStock;
        private int maxStock;
        private double weight;
        private String image;
        private Long productImageId;
        private List<VariantAttributeInner> variantAttributes;
        private FlashSaleInner flashSale;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VariantAttributeInner {
        private String name;
        private String attributeValue;
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
        private String attributeValue;
        private Long attributeId;
        private String attributeName;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductImageInner {
        private Long id;
        private String imageUrl;
    }
}
