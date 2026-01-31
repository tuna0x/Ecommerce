package com.tuna.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Coupon;
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>,JpaSpecificationExecutor<Coupon> {

    boolean existsByCode(String code);
    Optional<Coupon> findByCode(String code);
}
