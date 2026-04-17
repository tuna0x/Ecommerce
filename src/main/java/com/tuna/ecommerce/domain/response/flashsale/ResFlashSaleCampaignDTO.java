package com.tuna.ecommerce.domain.response.flashsale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResFlashSaleCampaignDTO {
    private Long id;
    private String name;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    private Boolean active;
    private List<ResFlashSaleItemDTO> items;

    @Getter
    @Setter
    public static class ResFlashSaleItemDTO {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private Long variantId;
        private String variantSku;
        private BigDecimal flashSalePrice;
        private Integer limitQuantity;
        private Integer soldQuantity;
    }
}
