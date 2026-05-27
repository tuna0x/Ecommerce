package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.AttributeValue;
@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long>,JpaSpecificationExecutor<AttributeValue> {
    boolean existsByAttributeIdAndAttributeValue(Long attributeId, String attributeValue);
    AttributeValue findByAttributeValue(String attributeValue);

    @Query("""
            SELECT av FROM AttributeValue av
            JOIN av.attribute a
            WHERE LOWER(TRIM(a.name)) = LOWER(TRIM(:attributeName))
              AND LOWER(TRIM(av.attributeValue)) = LOWER(TRIM(:attributeValue))
            """)
    AttributeValue findByAttributeNameAndValue(@Param("attributeName") String attributeName,
            @Param("attributeValue") String attributeValue);

    List<AttributeValue> findByAttributeId(Long attributeId);


}
