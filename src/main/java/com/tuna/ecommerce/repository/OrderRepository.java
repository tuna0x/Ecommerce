package com.tuna.ecommerce.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Override
    @EntityGraph(attributePaths = { "items", "items.product", "payment" })
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = { "items", "payment" })
    Optional<Order> findByUserIdAndId(Long userId, Long id);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(com.tuna.ecommerce.ultil.constant.OrderStatusEnum status, Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i WHERE o.user.id = :userId AND i.product.id = :productId AND o.status = 'DELIVERED'")
    boolean hasPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT SUM(o.finalPrice) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o")
    long countTotalOrders();
}