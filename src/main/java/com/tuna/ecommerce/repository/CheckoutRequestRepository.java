package com.tuna.ecommerce.repository;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.CheckoutRequest;
import com.tuna.ecommerce.ultil.constant.CheckoutStatusEnum;

@Repository
public interface CheckoutRequestRepository extends JpaRepository<CheckoutRequest, Long> {
    @EntityGraph(attributePaths = {"user"})
    Optional<CheckoutRequest> findByRequestId(String requestId);

    Optional<CheckoutRequest> findByRequestIdAndUserId(String requestId, Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<CheckoutRequest> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            CheckoutStatusEnum status,
            Instant updatedAt);
}
