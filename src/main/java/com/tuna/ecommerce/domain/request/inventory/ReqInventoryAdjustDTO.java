package com.tuna.ecommerce.domain.request.inventory;

import com.tuna.ecommerce.ultil.constant.InventoryLogType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqInventoryAdjustDTO {
    private Long productId;
    private Long variantId;
    private int quantity;
    private Double costPrice;
    private Integer minStockThreshold;
    private Integer maxStock;
    private InventoryLogType type;
    private String note;
}
