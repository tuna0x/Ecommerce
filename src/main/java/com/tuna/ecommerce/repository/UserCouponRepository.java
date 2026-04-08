package com.tuna.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserCoupon;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUserOrderByCollectedAtDesc(User user);
    Optional<UserCoupon> findByUserAndCoupon(User user, Coupon coupon);
    boolean existsByUserAndCoupon(User user, Coupon coupon);
}
