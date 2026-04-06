package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.ProductPromotion;
import com.tuna.ecommerce.domain.Promotion;

@Repository
public interface ProductPromotionRepository extends JpaRepository<ProductPromotion, Long> {
    boolean existsByProductIdAndPromotionId(Long productId, Long promotionId);

    List<ProductPromotion> findByPromotionId(Long promotionId);

    void deleteByPromotionId(Long promotionId);

 @Query("""
        SELECT pp
        FROM ProductPromotion pp
        JOIN FETCH pp.promotion p
        WHERE pp.product.id = :productId
          AND p.active = true
          AND (p.startAt IS NULL OR p.startAt <= :now)
          AND (p.endAt IS NULL OR p.endAt >= :now)
    """)
    List<ProductPromotion> findActiveByProductId(Long productId, java.time.LocalDateTime now);
}
