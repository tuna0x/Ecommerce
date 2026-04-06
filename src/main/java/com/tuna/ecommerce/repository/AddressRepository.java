package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address,Long>,JpaSpecificationExecutor<Address>{
    Address findByUserIdAndIsDefaultTrue(Long userId);
    List<Address> findByUserId(Long userId);
}
