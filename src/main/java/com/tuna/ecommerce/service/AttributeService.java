package com.tuna.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Attribute;
import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.request.attribute.ReqCreateAttributeDTO;
import com.tuna.ecommerce.domain.request.attribute.ReqUpdateAttributeDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.attribute.ResAttributeDTO;
import com.tuna.ecommerce.repository.AttributeRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class AttributeService {
    private final AttributeRepository attributeRepository;
    private final CategoryService categoryService;

    public Attribute createAttribute(ReqCreateAttributeDTO req) throws IdInvalidException {
        Attribute attribute = new Attribute();
        attribute.setName(req.getName());
        attribute.setActive(req.getActive());

        if (req.getCategoryIds() != null && !req.getCategoryIds().isEmpty()) {
            List<Category> categories = req.getCategoryIds().stream()
                    .map(id -> this.categoryService.handleGetById(id))
                    .filter(c -> c != null)
                    .collect(Collectors.toList());
            attribute.setCategories(categories);
        }
        return this.attributeRepository.save(attribute);
    }

    public ResultPaginationDTO getAllAttribute(Specification<Attribute> spec, Pageable page) {
        Page<Attribute> attributePage = this.attributeRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(attributePage.getNumber() + 1);
        meta.setPageSize(attributePage.getSize());
        meta.setPages(attributePage.getTotalPages());
        meta.setTotal(attributePage.getTotalElements());

        List<ResAttributeDTO> list = attributePage.getContent().stream()
                .map(item -> this.convertAttributeDTO(item))
                .collect(Collectors.toList());

        rs.setMeta(meta);
        rs.setResult(list);
        return rs;
    }

    public Attribute getAttributeById(Long id) {
        return this.attributeRepository.findById(id).orElse(null);
    }

    public Attribute updateAttribute(ReqUpdateAttributeDTO req) throws IdInvalidException {
        Attribute curAttribute = this.getAttributeById(req.getId());
        if (curAttribute == null) {
            throw new IdInvalidException("Attribute not found with id: " + req.getId());
        }

        curAttribute.setName(req.getName());
        curAttribute.setActive(req.getActive());

        if (req.getCategoryIds() != null) {
            List<Category> categories = req.getCategoryIds().stream()
                    .map(id -> this.categoryService.handleGetById(id))
                    .filter(c -> c != null)
                    .collect(Collectors.toList());
            curAttribute.setCategories(categories);
        }
        return this.attributeRepository.save(curAttribute);
    }

    public boolean existsByName(String name) {
        return this.attributeRepository.existsByName(name);
    }

    public void deleteAttribute(long id) {
        // CascadeType.ALL + orphanRemoval handles attributeValue deletion
        this.attributeRepository.deleteById(id);
    }

    public ResAttributeDTO convertAttributeDTO(Attribute attribute) {
        ResAttributeDTO res = new ResAttributeDTO();
        res.setId(attribute.getId());
        res.setName(attribute.getName());
        res.setActive(attribute.getActive());

        if (attribute.getCategories() != null) {
            List<ResAttributeDTO.CategoryInner> listCate = attribute.getCategories().stream().map(item -> {
                ResAttributeDTO.CategoryInner cate = new ResAttributeDTO.CategoryInner();
                cate.setId(item.getId());
                cate.setName(item.getName());
                return cate;
            }).collect(Collectors.toList());
            res.setCategories(listCate);
        }
        return res;
    }
}
