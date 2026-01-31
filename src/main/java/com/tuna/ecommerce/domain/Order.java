package com.tuna.ecommerce.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal totalPrice;
    private BigDecimal discountPrice;
    private BigDecimal finalPrice;
    private String ShippingAddress;

    //n-1
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status;

    @Enumerated(EnumType.STRING)
    private PaymentStatusEnum paymentStatus;


    //1-n
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OrderItem> items;

    @OneToOne
    @JoinColumn(name="payment_id",referencedColumnName = "id")
    private Payment payment;//1-1

    private Instant createdAt;
    private String createdBy;

    @PrePersist
    public void handleBeforeCreate(){
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() ==true ?
        SecurityUtil.getCurrentUserLogin().get() : "";
        this.createdAt = Instant.now();
    }
}
