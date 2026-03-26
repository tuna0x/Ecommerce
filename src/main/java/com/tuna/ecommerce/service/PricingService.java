package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductPromotion;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.response.promotion.ResPriceResultDTO;
import com.tuna.ecommerce.repository.ProductPromotionRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PricingService {
    private final ProductPromotionRepository productPromotionRepository;

    /**
     * Calculates the best discount (highest value) among all applicable promotions.
     * Picks only one "best" discount, they don't stack by default in this logic.
     */
    public BigDecimal calculateBestDiscount(BigDecimal price, List<Promotion> promotions) {
        if (price == null || promotions == null || promotions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal bestDiscountAmount = BigDecimal.ZERO;

        for (Promotion prom : promotions) {
            BigDecimal currentDiscount = BigDecimal.ZERO;
            if (prom.getType() == null || prom.getValue() == null) continue;

            switch (prom.getType()) {
                case PERCENT:
                    // Example: 10% discount on 100k = 10k
                    currentDiscount = price.multiply(prom.getValue())
                            .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                    break;
                case FIXED:
                    currentDiscount = prom.getValue();
                    break;
                default:
                    break;
            }

            if (currentDiscount.compareTo(bestDiscountAmount) > 0) {
                bestDiscountAmount = currentDiscount;
            }
        }
        return bestDiscountAmount;
    }

    /**
     * Calculates current pricing for a product including its best active promotion.
     */
    public ResPriceResultDTO calculatePrice(Product product) {
        if (product == null || product.getOriginalPrice() == null) {
            return new ResPriceResultDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal originalPrice = product.getOriginalPrice();
        List<ProductPromotion> productPromos = this.productPromotionRepository.findActiveByProductId(product.getId());
        
        List<Promotion> promotions = new ArrayList<>();
        if (productPromos != null) {
            for (ProductPromotion pp : productPromos) {
                if (pp.getPromotion() != null) {
                    promotions.add(pp.getPromotion());
                }
            }
        }

        BigDecimal discount = this.calculateBestDiscount(originalPrice, promotions);
        
        // Final price cannot be less than zero
        BigDecimal finalPrice = originalPrice.subtract(discount);
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
        }

        return new ResPriceResultDTO(originalPrice, discount, finalPrice);
    }
}
