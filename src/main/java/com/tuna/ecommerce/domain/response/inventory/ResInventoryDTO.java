package com.tuna.ecommerce.domain.response.inventory;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResInventoryDTO {
    private Long id;
    private int stock;
    private int reservedStock;
    private double costPrice;
    private int minStockThreshold;
    private int maxStock;
    private Instant updatedAt;

    private ProductVariantDTO productVariant;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariantDTO {
        private Long id;
        private String sku;
        private ProductDTO product;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO {
        private Long id;
        private String name;
        private String thumbnail;
        private String categoryName;
    }
}
