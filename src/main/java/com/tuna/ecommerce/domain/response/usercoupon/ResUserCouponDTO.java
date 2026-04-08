package com.tuna.ecommerce.domain.response.usercoupon;

import java.time.Instant;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuna.ecommerce.ultil.constant.CouponTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResUserCouponDTO {
    private Long id;

    @JsonProperty("isUsed")
    private boolean isUsed;
    private Instant collectedAt;
    private Instant usedAt;
    private CouponInner coupon;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CouponInner {
        private Long id;
        private String code;
        private String name;
        private String description;
        private CouponTypeEnum type;
        private BigDecimal discountValue;
        private java.time.LocalDateTime startDate;
        private java.time.LocalDateTime endDate;
        private BigDecimal minOrderValue;
        private BigDecimal maxDiscountValue;
        private Integer usageLimit;
        private Integer usedCount;
    }
}
