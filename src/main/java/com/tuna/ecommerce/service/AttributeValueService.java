package com.tuna.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Attribute;
import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.request.attributeValue.ReqCreateAttributesValueDTO;
import com.tuna.ecommerce.domain.request.attributeValue.ReqUpdateAttributesValueDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.attributevalue.ResAttributeValueDTO;
import com.tuna.ecommerce.repository.AttributeValueRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class AttributeValueService {
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeService attributeService;

    public AttributeValue createAttributeValue(ReqCreateAttributesValueDTO req) throws IdInvalidException {
        Attribute attribute = this.attributeService.getAttributeById(req.getAttributeId());
        if (attribute == null) {
            throw new IdInvalidException("Attribute parent not found with id: " + req.getAttributeId());
        }

        AttributeValue newAttributeValue = new AttributeValue();
        newAttributeValue.setValue(req.getValue());
        newAttributeValue.setAttribute(attribute);
        return this.attributeValueRepository.save(newAttributeValue);
    }

    public AttributeValue getAttributeValueById(Long id) {
        return this.attributeValueRepository.findById(id).orElse(null);
    }

    public AttributeValue updateAttributeValue(ReqUpdateAttributesValueDTO req) throws IdInvalidException {
        AttributeValue curValue = this.getAttributeValueById(req.getId());
        if (curValue == null) {
            throw new IdInvalidException("Attribute value not found with id: " + req.getId());
        }

        curValue.setValue(req.getValue());
        Attribute attribute = this.attributeService.getAttributeById(req.getAttributeId());
        if (attribute != null) {
            curValue.setAttribute(attribute);
        }
        return this.attributeValueRepository.save(curValue);
    }

    public ResultPaginationDTO getAllAttributeValue(Specification<AttributeValue> spec, Pageable page) {
        Page<AttributeValue> attributeValues = this.attributeValueRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(attributeValues.getNumber() + 1);
        meta.setPageSize(attributeValues.getSize());
        meta.setPages(attributeValues.getTotalPages());
        meta.setTotal(attributeValues.getTotalElements());

        List<ResAttributeValueDTO> list = attributeValues.getContent().stream()
                .map(item -> this.convertToAttributeValueDTO(item))
                .collect(Collectors.toList());

        rs.setMeta(meta);
        rs.setResult(list);
        return rs;
    }

    public void deleteAttributeValue(Long id) {
        this.attributeValueRepository.deleteById(id);
    }

    public boolean existsByAttributeIdAndValue(Long attributeId, String value) {
        return this.attributeValueRepository.existsByAttributeIdAndValue(attributeId, value);
    }

    public ResAttributeValueDTO convertToAttributeValueDTO(AttributeValue attributeValue) {
        ResAttributeValueDTO res = new ResAttributeValueDTO();
        res.setId(attributeValue.getId());
        res.setValue(attributeValue.getValue());

        if (attributeValue.getAttribute() != null) {
            ResAttributeValueDTO.AttributeInner attr = new ResAttributeValueDTO.AttributeInner();
            attr.setId(attributeValue.getAttribute().getId());
            attr.setName(attributeValue.getAttribute().getName());

            if (attributeValue.getAttribute().getCategories() != null) {
                List<ResAttributeValueDTO.AttributeInner.CategoryInner> listCate = attributeValue.getAttribute().getCategories().stream().map(item -> {
                    ResAttributeValueDTO.AttributeInner.CategoryInner cate = new ResAttributeValueDTO.AttributeInner.CategoryInner();
                    cate.setId(item.getId());
                    cate.setName(item.getName());
                    return cate;
                }).collect(Collectors.toList());
                attr.setCategories(listCate);
            }
            res.setAttribute(attr);
        }

        return res;
    }
}
