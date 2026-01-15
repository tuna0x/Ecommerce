package com.tuna.ecommerce.service;

import java.security.KeyStore.Entry.Attribute;

import org.springframework.beans.factory.BeanRegistry.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.AttributeValueRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AttributeValueService {
    private final AttributeValueRepository attributeValueRepository;

    public AttributeValue save(AttributeValue attributeValue) {
        return this.attributeValueRepository.save(attributeValue);
    }

    public AttributeValue findById(Long id) {
        return this.attributeValueRepository.findById(id).orElse(null);
    }

    public AttributeValue update(AttributeValue attributeValue) {
        AttributeValue curAttributeValue = findById(attributeValue.getId());
        if (curAttributeValue != null) {
            curAttributeValue.setValue(attributeValue.getValue());
            attributeValue = this.attributeValueRepository.save(curAttributeValue);
        }
        return attributeValue;
    }

    public ResultPaginationDTO getAllAttributeValue(Specification<AttributeValue> spec, Pageable page) {
        Page<AttributeValue> attributeValues = this.attributeValueRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(attributeValues.getNumber() + 1);
        meta.setPageSize(attributeValues.getSize());
        meta.setPages(attributeValues.getTotalPages());
        meta.setTotal(attributeValues.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(attributeValues.getContent());
        return rs;
    }

    public void deleteByAttributeId(Long id) {
        this.attributeValueRepository.deleteById(id);
    }
}
