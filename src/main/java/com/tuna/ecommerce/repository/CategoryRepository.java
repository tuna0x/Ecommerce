package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


import com.tuna.ecommerce.domain.Category;



@Repository
public interface  CategoryRepository extends JpaRepository<Category,Long>,JpaSpecificationExecutor<Category>{
    boolean existsByName(String name);
}
