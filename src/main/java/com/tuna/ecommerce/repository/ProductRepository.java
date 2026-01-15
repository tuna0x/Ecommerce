package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Product;



@Repository
public interface ProductRepository extends JpaRepository<Product,Long>,JpaSpecificationExecutor<Product>{

    boolean existsByName(String name);
    Double findByOriginalPrice(long id);
}
