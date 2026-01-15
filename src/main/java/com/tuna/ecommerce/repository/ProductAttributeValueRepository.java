package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tuna.ecommerce.domain.ProductAttributeValue;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {
    
}
