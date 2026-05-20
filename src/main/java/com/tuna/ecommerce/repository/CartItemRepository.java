package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.CartItem;

import jakarta.persistence.LockModeType;



import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem,Long>,JpaSpecificationExecutor<CartItem>{
    CartItem findByCartIdAndProductId(long cartId, long productId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"cart", "cart.user", "product", "productVariant"})
    List<CartItem> findByIdIn(List<Long> list);
}
