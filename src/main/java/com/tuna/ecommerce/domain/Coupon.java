package com.tuna.ecommerce.domain;

import java.time.Instant;

import com.tuna.ecommerce.ultil.constant.CouponStatus;
import com.tuna.ecommerce.ultil.constant.CouponTypeEnum;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class Coupon {
    private Long id;
    private String code;
    private CouponTypeEnum type;
    private Double discountValue;
    private Instant startDate;
    private Instant endDate;
    private Double minSpend;
    private Double maxSpend;
    private int maxUsesPerUser;
    private CouponStatus status;
    private Instant createdAt;
    private Instant updatedAt;

        @PrePersist
    public void handleBeforeCreate(){
        this.createdAt = Instant.now();
    }

        @PreUpdate
    public void handleBeforeUpdate(){
        this.updatedAt = Instant.now();
    }
}
