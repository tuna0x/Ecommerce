package com.tuna.ecommerce.domain.response.inventory;

import java.time.Instant;

import com.tuna.ecommerce.ultil.constant.InventoryLogType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResInventoryLogDTO {
    private Long id;
    private int quantityChange;
    private InventoryLogType type;
    private String note;
    private Instant createdAt;
    private String createdBy;
    private InventoryDTO inventory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryDTO {
        private Long id;
        private ProductVariantDTO productVariant;
    }

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
    }
}
