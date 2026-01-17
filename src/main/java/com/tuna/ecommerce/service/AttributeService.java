package com.tuna.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Attribute;
import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.attribute.ReqCreateAttributeDTO;
import com.tuna.ecommerce.domain.request.attribute.ReqUpdateAttributeDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.repository.AttributeRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AttributeService {
    private final AttributeRepository attributeRepository;
    private final CategoryService categoryService;

    public Attribute createAttribute(ReqCreateAttributeDTO attribute) {
        Attribute curAttribute = new Attribute();
        Category category = this.categoryService.handleGetById(attribute.getCategoryId());
        if (category!=null) {
            curAttribute.setCategory(category);
            curAttribute.setName(attribute.getName());
        }
        return this.attributeRepository.save(curAttribute);
    }

    public ResultPaginationDTO getAllAttribute(Specification<Attribute> spec, Pageable page) {
        Page<Attribute> attribute = this.attributeRepository.findAll(spec, page);
                ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(attribute.getNumber() + 1);
        meta.setPageSize(attribute.getSize());
        meta.setPages(attribute.getTotalPages());
        meta.setTotal(attribute.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(attribute.getContent());
        return rs;
    }

    public Attribute getAttributeById(Long id) {
        return this.attributeRepository.findById(id).orElse(null);
    }

    public Attribute updateAttribute(ReqUpdateAttributeDTO attribute) {
        Attribute curAttribute = this.getAttributeById(attribute.getId());
        if (curAttribute != null) {
            curAttribute.setName(attribute.getName());
            Category category = this.categoryService.handleGetById(attribute.getCategoryId());
            if (category!=null) {
                curAttribute.setCategory(category);
            }
                curAttribute = this.attributeRepository.save(curAttribute);

        }
        return curAttribute;
    }

    public boolean existsByName(String name) {
        return this.attributeRepository.existsByName(name);
    }

}
