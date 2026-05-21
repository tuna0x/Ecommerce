package com.tuna.ecommerce.domain;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.tuna.ecommerce.ultil.SecurityUtil;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory", indexes = {
        @Index(name = "idx_inventory_product_variant", columnList = "product_variant_id")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @jakarta.persistence.OneToMany(mappedBy = "inventory", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<InventoryLog> inventoryLogs = new java.util.ArrayList<>();

    private int stock;
    private int reservedStock = 0; // Hàng đang được giữ
    private double costPrice = 0; // Giá vốn
    private int minStockThreshold = 10; // Ngưỡng báo hết hàng
    private int maxStock = 100; // Ngưỡng báo tồn kho quá nhiều (Overstock)

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.updatedAt = Instant.now();
    }
}
