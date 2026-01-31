package com.tuna.ecommerce.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table  (name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(mappedBy = "payment")
    @JsonIgnore
    private Order order;
    private BigDecimal amount;// số tiền thanh toán

    @Enumerated(EnumType.STRING)
    private PaymentMethodEnum method;// phương thức thanh toán
    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status;// trạng thái thanh toán
    private String transactionId;// mã giao dịch

    private Instant createdAt;
    private String createdBy;

    @PrePersist
    public void handleBeforeCreate(){
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() ==true ?
        SecurityUtil.getCurrentUserLogin().get() : "";
        this.createdAt = Instant.now();
        this.transactionId="PAY-"+RandomStringUtils.randomAlphanumeric(10).toUpperCase();
    }
}
