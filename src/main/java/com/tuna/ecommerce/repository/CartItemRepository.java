package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.CartItem;



@Repository
public interface CartItemRepository extends JpaRepository<CartItem,Long>,JpaSpecificationExecutor<CartItem>{
    CartItem findByCartIdAndProductId(long cartId, long productId);
    List<CartItem> findByIdIn(List<Long> list);
}
