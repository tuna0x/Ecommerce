package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.OrderItem;


@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
}