package com.tuna.ecommerce.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variants")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    private String sku;
    private BigDecimal price; // Price override for this variant
    private double weight;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_variant_attribute_values",
        joinColumns = @JoinColumn(name = "product_variant_id"),
        inverseJoinColumns = @JoinColumn(name = "attribute_value_id")
    )
    private List<AttributeValue> attributeValues = new ArrayList<>();

    @OneToOne(mappedBy = "productVariant", cascade = CascadeType.ALL)
    @JsonIgnore
    private Inventory inventory;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedAt = Instant.now();
    }
}
