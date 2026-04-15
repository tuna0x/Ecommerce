package com.tuna.ecommerce.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuna.ecommerce.ultil.constant.CouponStatus;
import com.tuna.ecommerce.ultil.constant.CouponTypeEnum;

import jakarta.persistence.Column;
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
    private String name;
    private String description;
    private CouponTypeEnum type;
    @Column(name = "discount_value")
    @NotNull
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountValue;
    private Integer usageLimit = 0;
    private Integer usedCount = 0;
    private CouponStatus status;
    @JsonProperty("isPublic")
    private boolean isPublic;
    private Instant createdAt;
    private Instant updatedAt;

        @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
        if (this.code == null || this.code.isBlank()) {
            this.code = "COUPON-" + RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        }
    }

        @PreUpdate
    public void handleBeforeUpdate(){
        this.updatedAt = Instant.now();
    }
}
