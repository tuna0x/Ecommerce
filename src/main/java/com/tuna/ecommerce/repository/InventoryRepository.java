package com.tuna.ecommerce.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Inventory;
import com.tuna.ecommerce.domain.ProductVariant;


@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"productVariant", "productVariant.product", "productVariant.product.category", "productVariant.product.images"})
    Optional<Inventory> findByProductVariant(ProductVariant productVariant);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"productVariant", "productVariant.product", "productVariant.product.category", "productVariant.product.images"})
    org.springframework.data.domain.Page<Inventory> findAll(org.springframework.data.jpa.domain.Specification<Inventory> spec, org.springframework.data.domain.Pageable pageable);
    
    List<Inventory> findByProductVariantProduct(com.tuna.ecommerce.domain.Product product);

    @Query("SELECT SUM(i.stock * pv.price) FROM Inventory i JOIN i.productVariant pv")
    BigDecimal calculateTotalCapitalValue();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.stock = 0")
    long countOutOfStock();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.stock > 0 AND i.stock < i.minStockThreshold")
    long countLowStock();

    @Query("SELECT p.name as name, SUM(i.stock * pv.price) as totalValue " +
           "FROM Inventory i JOIN i.productVariant pv JOIN pv.product p " +
           "GROUP BY p.id, p.name ORDER BY totalValue DESC")
    List<Object[]> findTopProductsByValue();

    @Query("SELECT p.id as id, p.name as name, pi.imageUrl as image, i.stock as stock " +
           "FROM Inventory i JOIN i.productVariant pv JOIN pv.product p LEFT JOIN p.images pi " +
           "WHERE i.stock > 0 AND i.stock < i.minStockThreshold AND (pi.main = true OR pi.id = (SELECT MIN(pi2.id) FROM ProductImage pi2 WHERE pi2.product.id = p.id))")
    List<Object[]> findLowStockItems();
}
