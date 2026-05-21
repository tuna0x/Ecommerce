package com.tuna.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.CartItem;

import jakarta.persistence.LockModeType;



import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem,Long>,JpaSpecificationExecutor<CartItem>{
    CartItem findByCartIdAndProductId(long cartId, long productId);

    @Query("""
            SELECT ci FROM CartItem ci
            LEFT JOIN FETCH ci.productVariant pv
            WHERE ci.cart.id = :cartId
              AND ci.product.id = :productId
              AND (
                    (:variantId IS NULL AND ci.productVariant IS NULL)
                    OR (:variantId IS NOT NULL AND pv.id = :variantId)
                  )
            """)
    Optional<CartItem> findByCartProductAndVariant(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId,
            @Param("variantId") Long variantId);

    @Query("""
            SELECT
                c.id AS cartId,
                u.id AS userId,
                COALESCE(up.name, u.email) AS userName,
                ci.id AS itemId,
                p.id AS productId,
                p.name AS productName,
                MIN(img.imageUrl) AS thumbnail,
                COALESCE(b.name, 'No Brand') AS brandName,
                p.originalPrice AS originalPrice,
                ci.unitPrice AS unitPrice,
                ci.quantity AS quantity,
                ci.totalPrice AS totalPrice,
                pv.id AS variantId,
                pv.price AS variantPrice,
                COALESCE(inv.stock, 0) AS variantStock
            FROM CartItem ci
            JOIN ci.cart c
            JOIN c.user u
            LEFT JOIN u.userProfile up
            JOIN ci.product p
            LEFT JOIN p.brand b
            LEFT JOIN p.images img
            LEFT JOIN ci.productVariant pv
            LEFT JOIN pv.inventory inv
            WHERE u.email = :email
            GROUP BY c.id, u.id, up.name, u.email, ci.id, p.id, p.name, b.name,
                     p.originalPrice, ci.unitPrice, ci.quantity, ci.totalPrice,
                     pv.id, pv.price, inv.stock
            ORDER BY ci.id ASC
            """)
    List<Object[]> findCartSummaryByUserEmail(@Param("email") String email);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"cart", "cart.user", "product", "productVariant"})
    List<CartItem> findByIdIn(List<Long> list);

    @Modifying
    @Query("""
            DELETE FROM CartItem ci
            WHERE ci.cart.id = :cartId
              AND ci.product.id = :productId
              AND (
                    (:variantId IS NULL AND ci.productVariant IS NULL)
                    OR (:variantId IS NOT NULL AND ci.productVariant.id = :variantId)
                  )
            """)
    int deleteByCartProductAndVariant(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId,
            @Param("variantId") Long variantId);
}
