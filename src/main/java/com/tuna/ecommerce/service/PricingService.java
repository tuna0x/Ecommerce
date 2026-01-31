package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductPromotion;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.response.promotion.ResPriceResultDTO;
import com.tuna.ecommerce.repository.ProductPromotionRepository;
import com.tuna.ecommerce.ultil.constant.PromotionTypeEnum;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PricingService {
    private final ProductPromotionRepository productPromotionRepository;

    public BigDecimal calculateBestDiscount(BigDecimal price,List<Promotion> promotions) {
        BigDecimal max=BigDecimal.ZERO;
        BigDecimal discount=BigDecimal.ZERO;
        for (Promotion prom : promotions){
            switch (prom.getType()) {
                case PERCENT:
                    discount=price.multiply(prom.getValue()).divide(BigDecimal.valueOf(100));
                    break;
                case FIXED:
                    discount=prom.getValue();
                    break;
                default:
                    break;
            }

            if (discount.compareTo(max)>0) {
                max=discount;
            }
        }
        return max;
    }

    public ResPriceResultDTO calculatePrice(Product product) {
        BigDecimal originalPrice=product.getOriginalPrice();
        List<ProductPromotion> list=this.productPromotionRepository.findActiveByProductId(product.getId());
        List<Promotion> promotions= new ArrayList<>();
        for(ProductPromotion p:list){
            promotions.add(p.getPromotion());
        }
        BigDecimal discount = this.calculateBestDiscount(originalPrice, promotions);
        BigDecimal finalPrice = originalPrice.subtract(discount);
        return new ResPriceResultDTO(originalPrice, discount, finalPrice);

    }
}
