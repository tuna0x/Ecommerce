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

    List<Product> findTop8ByDeletedFalseAndActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(Long categoryId,
            Long id);

    // Smart Recommendations: Same Brand + Same Category (Top 4)
    List<Product> findTop4ByDeletedFalseAndActiveTrueAndCategoryIdAndBrandIdAndIdNotOrderBySoldCountDesc(
            Long categoryId, Long brandId, Long id);

    // Fallback: Best Sellers in Category (Top 8)
    List<Product> findTop8ByDeletedFalseAndActiveTrueAndCategoryIdAndIdNotOrderBySoldCountDesc(Long categoryId,
            Long id);

    @EntityGraph(attributePaths = { "category", "brand" })
    Page<Product> findByDeletedFalseAndActiveTrueAndIdIn(List<Long> productIds, Pageable pageable);

    @EntityGraph(attributePaths = { "category", "brand" })
    Page<Product> findByCategoryIdInOrIdIn(List<Long> categoryIds, List<Long> productIds, Pageable pageable);

    @Query("SELECT p.originalPrice FROM Product p WHERE p.id = :id")
    Optional<Double> findOriginalPriceById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = { "category", "brand" })
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "category", "brand", "images", "productAttributeValues" })
    Optional<Product> findById(Long id);

    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findPlainById(@Param("id") Long id);

    @Query("SELECT c.name as category, COUNT(p.id) as count, SUM(pv.price * i.stock) as value " +
            "FROM Product p " +
            "JOIN p.category c " +
            "JOIN p.variants pv " +
            "JOIN pv.inventory i " +
            "GROUP BY c.id, c.name")
    List<Object[]> findCategoryDistribution();

    @Query(value = "SELECT * FROM products WHERE (LOWER(name) LIKE LOWER(:query) OR LOWER(name_unsigned) LIKE LOWER(:query)) AND active = true AND deleted = false LIMIT 15", nativeQuery = true)
    List<Product> searchByNameNative(@Param("query") String query);
}
