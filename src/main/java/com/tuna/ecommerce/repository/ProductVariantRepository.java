package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tuna.ecommerce.domain.ProductVariant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProduct_IdAndDeletedFalse(Long productId);

    Optional<ProductVariant> findFirstByProduct_IdAndDeletedFalseOrderByIdAsc(Long productId);
}
