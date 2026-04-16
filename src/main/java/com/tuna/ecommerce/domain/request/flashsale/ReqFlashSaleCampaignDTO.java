package com.tuna.ecommerce.domain.request.flashsale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqFlashSaleCampaignDTO {
    private String name;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    private List<FlashSaleItemRequest> items;

    @Getter
    @Setter
    public static class FlashSaleItemRequest {
        private Long productId;
        private Long variantId;
        private BigDecimal flashSalePrice;
        private Integer limitQuantity;
    }
}
