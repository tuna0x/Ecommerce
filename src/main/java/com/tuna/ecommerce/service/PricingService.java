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
    private final com.tuna.ecommerce.repository.PromotionRepository promotionRepository;

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
            if (prom.getType() == null || prom.getDiscountValue() == null)
                continue;

            switch (prom.getType()) {
                case PERCENT:
                    // Example: 10% discount on 100k = 10k
                    currentDiscount = price.multiply(prom.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                    break;
                case FIXED:
                    currentDiscount = prom.getDiscountValue();
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
     * Gathers all applicable promotions for a product (Specific, Global, and Category-based).
     */
    public List<Promotion> getApplicablePromotions(Product product) {
        List<Promotion> promotions = new ArrayList<>();
        if (product == null) {
            return promotions;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // 1. Specific Product Promotions
        List<ProductPromotion> productPromos = this.productPromotionRepository.findActiveByProductId(product.getId(), now);
        if (productPromos != null) {
            for (ProductPromotion pp : productPromos) {
                if (pp.getPromotion() != null) {
                    promotions.add(pp.getPromotion());
                }
            }
        }

        // 2. Global Promotions
        List<Promotion> globalPromos = this.promotionRepository.findActiveGlobal(now);
        if (globalPromos != null) {
            promotions.addAll(globalPromos);
        }

        // 3. Category Promotions
        if (product.getCategory() != null) {
            List<Promotion> categoryPromos = this.promotionRepository.findActiveByCategoryId(product.getCategory().getId(), now);
            if (categoryPromos != null) {
                promotions.addAll(categoryPromos);
            }
        }

        return promotions;
    }

    /**
     * Calculates current pricing for a product including its best active promotion.
     */
    public ResPriceResultDTO calculatePrice(Product product) {
        if (product == null || product.getOriginalPrice() == null) {
            return new ResPriceResultDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal originalPrice = product.getOriginalPrice();
        List<Promotion> promotions = this.getApplicablePromotions(product);

        return this.calculatePriceWithPromotions(originalPrice, promotions);
    }

    /**
     * Helper to calculate final price given an original price and a list of promotions.
     */
    public ResPriceResultDTO calculatePriceWithPromotions(BigDecimal originalPrice, List<Promotion> promotions) {
        if (originalPrice == null) {
            return new ResPriceResultDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
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
