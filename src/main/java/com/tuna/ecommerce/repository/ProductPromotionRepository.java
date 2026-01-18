package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.ProductPromotion;

@Repository
public interface ProductPromotionRepository extends JpaRepository<ProductPromotion, Long> {
    boolean existsByProductIdAndPromotionId(Long productId, Long promotionId);
}
