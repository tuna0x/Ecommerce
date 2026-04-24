package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User,Long>,JpaSpecificationExecutor<User>{

    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"role", "role.permissions"})
    User findByEmail(String email);

    User findByRefreshTokenAndEmail(String refreshToken, String email);
    
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.role.name IN :roleNames")
    java.util.List<User> findByRoleNameIn(@org.springframework.data.repository.query.Param("roleNames") java.util.List<String> roleNames);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countNewUsers(@org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate, @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate);
}
