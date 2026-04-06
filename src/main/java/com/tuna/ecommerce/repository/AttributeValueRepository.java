package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.AttributeValue;
@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long>,JpaSpecificationExecutor<AttributeValue> {
    boolean existsByAttributeIdAndAttributeValue(Long attributeId, String attributeValue);
    AttributeValue findByAttributeValue(String attributeValue);

    List<AttributeValue> findByAttributeId(Long attributeId);


}
