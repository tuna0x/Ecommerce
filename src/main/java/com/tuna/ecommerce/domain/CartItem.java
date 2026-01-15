package com.tuna.ecommerce.domain;

import java.time.Instant;

import com.tuna.ecommerce.ultil.SecurityUtil;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cartItems")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne
    @JoinColumn (name = "product_id")
    private Product product;

    private int quantity;
    private double unitPrice;
    private double totalPrice;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

            @PrePersist
    public void handleBeforeCreate(){
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() ==true ?
        SecurityUtil.getCurrentUserLogin().get() : "";
        this.createdAt = Instant.now();

    }

        @PreUpdate
    public void handleBeforeUpdate(){
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() ==true ?
        SecurityUtil.getCurrentUserLogin().get() : "";
        this.updatedAt = Instant.now();

    }
}
