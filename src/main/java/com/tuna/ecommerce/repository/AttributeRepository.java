package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Attribute;
import java.util.List;


@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long>, JpaSpecificationExecutor<Attribute> {
    boolean existsByName(String name);
    Attribute findByName(String name);
}
