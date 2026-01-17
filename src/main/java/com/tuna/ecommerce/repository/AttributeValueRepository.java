package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.AttributeValue;
@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long>,JpaSpecificationExecutor<AttributeValue> {
    boolean existsByAttributeIdAndValue(Long attributeId, String value);
    AttributeValue findByValue(String value);
}
