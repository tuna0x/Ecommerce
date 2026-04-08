package com.tuna.ecommerce.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Inventory;
import com.tuna.ecommerce.domain.ProductVariant;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductVariant(ProductVariant productVariant);

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
}
