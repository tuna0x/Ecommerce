package com.tuna.ecommerce.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


import com.tuna.ecommerce.domain.Category;



@Repository
public interface  CategoryRepository extends JpaRepository<Category,Long>,JpaSpecificationExecutor<Category>{
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    Category findByName(String name);
    List<Category> findByParentCategory_Id(Long parentId);
}
