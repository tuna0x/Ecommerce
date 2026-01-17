package com.tuna.ecommerce.service;

import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.tuna.ecommerce.domain.Attribute;
import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.request.attributeValue.ReqCreateAttributesValueDTO;
import com.tuna.ecommerce.domain.request.attributeValue.ReqUpdateAttributesValueDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.AttributeValueRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AttributeValueService {
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeService attributeService;

    public AttributeValue createAttributeValue(ReqCreateAttributesValueDTO attributeValue) {
        AttributeValue newAttributeValue = new AttributeValue();
        newAttributeValue.setValue(attributeValue.getValue());
        Attribute attribute = this.attributeService.getAttributeById(attributeValue.getAttributeId());
        if (attribute != null ) {
                newAttributeValue.setAttribute(attribute);
        }
        return this.attributeValueRepository.save(newAttributeValue);
    }

    public AttributeValue getAttributeValueById(Long id) {
        return this.attributeValueRepository.findById(id).orElse(null);
    }

    public AttributeValue updateAttributeValue(ReqUpdateAttributesValueDTO attributeValue) {
        AttributeValue curAttribute = this.getAttributeValueById(attributeValue.getId());
        if (curAttribute != null) {
            curAttribute.setValue(attributeValue.getValue());
            Attribute attribute = this.attributeService.getAttributeById(attributeValue.getAttributeId());
            if (attribute != null) {
                curAttribute.setAttribute(attribute);
            }
            curAttribute = this.attributeValueRepository.save(curAttribute);
        }
        return curAttribute;
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

    public boolean existsByAttributeIdAndValue(Long attributeId, String value) {
        return this.attributeValueRepository.existsByAttributeIdAndValue(attributeId, value);
    }

}
