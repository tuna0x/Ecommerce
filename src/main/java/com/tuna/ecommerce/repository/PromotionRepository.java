package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>,JpaSpecificationExecutor<Promotion> {
    
}
