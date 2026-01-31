package com.tuna.ecommerce.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomStringUtils;

import com.tuna.ecommerce.ultil.constant.CouponStatus;
import com.tuna.ecommerce.ultil.constant.CouponTypeEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "coupons")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String code;
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
    private Instant createdAt;
    private Instant updatedAt;

        @PrePersist
    public void handleBeforeCreate(){
        this.createdAt = Instant.now();
         this.code="COUPON-"+RandomStringUtils.randomAlphanumeric(6).toUpperCase();
    }

        @PreUpdate
    public void handleBeforeUpdate(){
        this.updatedAt = Instant.now();
    }
}
