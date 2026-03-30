package com.tuna.ecommerce.domain.request.coupon;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private String code;
    private String name;
    private String description;
    private CouponTypeEnum type;
    private BigDecimal value;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountValue;
    private Integer usageLimit;
    private Integer usedCount;
    private CouponStatus status;
    private boolean isPublic;
}
