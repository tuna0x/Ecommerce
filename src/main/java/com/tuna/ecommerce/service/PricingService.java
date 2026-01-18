package com.tuna.ecommerce.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.repository.ProductPromotionRepository;
import com.tuna.ecommerce.ultil.constant.PromotionTypeEnum;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PricingService {
    private final ProductPromotionRepository productPromotionRepository;

    public BigDecimal calculateDiscount(BigDecimal price, Promotion promotion) {
        if (promotion.getType()==PromotionTypeEnum.PERCENT) {
            return price.multiply(promotion.getValue()).divide(BigDecimal.valueOf(100));
        }
        if (promotion.getType()==PromotionTypeEnum.FIXED) {
            return promotion.getValue();
        }
        return BigDecimal.ZERO;
    }

    public void applyPromotionToProduct(Product product) {
        
    }
}
