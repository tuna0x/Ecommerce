package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tuna.ecommerce.domain.ProductAttributeValue;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {
    boolean existsByProductIdAndAttributeValueId(Long productId, Long attributeValueId);
    List<ProductAttributeValue> findByProductId(Long productId);
}
