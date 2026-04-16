package com.tuna.ecommerce.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "flash_sale_items")
@Entity
public class FlashSaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    @JsonIgnore
    private FlashSaleCampaign campaign;

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProductVariant variant;

    private BigDecimal flashSalePrice;

    private Integer limitQuantity; // Số lượng mở bán Flash Sale

    private Integer soldQuantity = 0; // Số lượng đã bán trong đợt Sale
}
