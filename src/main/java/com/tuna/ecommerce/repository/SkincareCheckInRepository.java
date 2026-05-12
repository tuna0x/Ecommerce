package com.tuna.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.SkincareCheckIn;
import com.tuna.ecommerce.domain.User;

@Repository
public interface SkincareCheckInRepository extends JpaRepository<SkincareCheckIn, Long> {
    Optional<SkincareCheckIn> findByUser(User user);
    Optional<SkincareCheckIn> findByUserEmail(String email);
}
