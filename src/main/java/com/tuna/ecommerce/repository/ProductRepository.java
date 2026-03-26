package com.tuna.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByName(String name);

    List<Product> findTop8ByCategoryIdAndIdNot(Long categoryId, Long id);

    @Query("SELECT p.originalPrice FROM Product p WHERE p.id = :id")
    Optional<Double> findOriginalPriceById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = { "category", "brand" })
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "category", "brand", "images", "productAttributeValues" })
    Optional<Product> findById(Long id);
}
