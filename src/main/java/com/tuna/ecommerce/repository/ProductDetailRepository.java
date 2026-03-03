package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.ProductDetail;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail,Long>,JpaSpecificationExecutor<ProductDetail>{
    
}
