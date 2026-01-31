package com.tuna.ecommerce.domain.request.coupon;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import com.tuna.ecommerce.ultil.constant.CouponStatus;
import com.tuna.ecommerce.ultil.constant.CouponTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqUpdateCouponDTO {
    private Long id;
    private CouponTypeEnum type;
    private BigDecimal value;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountValue;
    private Integer usageLimit;
    private Integer usedCount;
    private CouponStatus status;
    private boolean isPublic;
}
