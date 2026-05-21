package com.tuna.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.User;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long>, JpaSpecificationExecutor<Cart> {

    @EntityGraph(attributePaths = { "items", "items.product" })
    Cart findByUser(User user);

    @Query("SELECT c FROM Cart c JOIN c.user u WHERE u.email = :email")
    Optional<Cart> findPlainByUserEmail(@Param("email") String email);

    @Override
    @EntityGraph(attributePaths = { "items", "items.product" })
    Optional<Cart> findById(Long id);
}
