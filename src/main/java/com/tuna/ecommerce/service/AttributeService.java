package com.tuna.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Attribute;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.repository.AttributeRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AttributeService {
    private final AttributeRepository attributeRepository;

    public Attribute createAttribute(Attribute attribute) {
        return this.attributeRepository.save(attribute);
    }

    public ResultPaginationDTO fetchAllAttribute(Specification<Attribute> spec, Pageable page) {
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

    public Attribute fetchAttributeById(Long id) {
        return this.attributeRepository.findById(id).orElse(null);
    }

    public Attribute updateAttribute(Attribute attribute) {
        Attribute curAttribute = fetchAttributeById(attribute.getId());
        if (curAttribute != null) {
            curAttribute.setName(attribute.getName());
            attribute = this.attributeRepository.save(curAttribute);
        }
        return attribute;
    }

}
