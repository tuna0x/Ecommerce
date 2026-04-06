package com.tuna.ecommerce.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {
    boolean existsById(Long id);

    @Query("SELECT p FROM Promotion p WHERE p.global = true " +
            "AND p.active = true " +
            "AND (p.startAt IS NULL OR p.startAt <= :now) " +
            "AND (p.endAt IS NULL OR p.endAt >= :now)")
    List<Promotion> findActiveGlobal(LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.category.id = :categoryId " +
            "AND p.active = true " +
            "AND (p.startAt IS NULL OR p.startAt <= :now) " +
            "AND (p.endAt IS NULL OR p.endAt >= :now)")
    List<Promotion> findActiveByCategoryId(Long categoryId, LocalDateTime now);
}
