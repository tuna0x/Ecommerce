package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User,Long>,JpaSpecificationExecutor<User>{

    boolean existsByEmail(String email);
    User findByEmail(String email);
    User findByRefreshTokenAndEmail(String refreshToken, String email);

}
