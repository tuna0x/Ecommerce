package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long>{
    
}
