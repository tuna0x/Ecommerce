package com.tuna.ecommerce.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import com.tuna.ecommerce.domain.FlashSaleItem;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM FlashSaleItem i WHERE i.id = :id")
    Optional<FlashSaleItem> findByIdWithWriteLock(@Param("id") Long id);
    
    @Query("SELECT i FROM FlashSaleItem i " +
           "JOIN FETCH i.campaign c " +
           "WHERE c.active = true AND c.startAt <= :now AND c.endAt >= :now " +
           "AND i.product.id = :productId")
    List<FlashSaleItem> findActiveFlashSaleItem(@Param("productId") Long productId, @Param("now") LocalDateTime now);
    
    @Query("SELECT i FROM FlashSaleItem i " +
           "JOIN FETCH i.campaign c " +
           "WHERE c.active = true AND c.startAt <= :now AND c.endAt >= :now " +
           "AND i.variant.id = :variantId")
    List<FlashSaleItem> findActiveFlashSaleItemByVariant(@Param("variantId") Long variantId, @Param("now") LocalDateTime now);

    List<FlashSaleItem> findByCampaignId(Long campaignId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByCampaignId(Long campaignId);
}
