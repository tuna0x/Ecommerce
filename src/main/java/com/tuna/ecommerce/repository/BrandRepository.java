package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Brand;
<<<<<<< HEAD
import java.util.List;


@Repository
public interface BrandRepository extends JpaRepository<Brand, Long>,JpaSpecificationExecutor<Brand>{
    boolean findByName(String name);
=======

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long>, JpaSpecificationExecutor<Brand> {

>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
}
