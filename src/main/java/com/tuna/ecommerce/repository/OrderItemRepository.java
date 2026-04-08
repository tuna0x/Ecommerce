package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.OrderItem;
import org.springframework.data.repository.query.Param;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT i.product.name, SUM(i.quantity) as totalQty FROM OrderItem i WHERE i.order.createdAt BETWEEN :startDate AND :endDate GROUP BY i.product.name ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts(@Param("startDate") java.time.Instant startDate,
            @Param("endDate") java.time.Instant endDate, Pageable pageable);
}