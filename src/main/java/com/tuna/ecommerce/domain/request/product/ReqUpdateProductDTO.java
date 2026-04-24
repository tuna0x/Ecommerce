package com.tuna.ecommerce.domain.request.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale.Category;

import com.tuna.ecommerce.domain.AttributeValue;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqUpdateProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private double costPrice;
    private int stock;
    private List<Long> attributeValue;
    private Long categoryId;
    private Long brandId;
    private String skinType;
    private List<String> image; // List of existing image URLs to keep
    private List<VariantDTO> variants;
    private Boolean active;

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
