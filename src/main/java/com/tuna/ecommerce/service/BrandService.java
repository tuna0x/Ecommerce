package com.tuna.ecommerce.service;

import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Brand;
import com.tuna.ecommerce.domain.request.brand.ReqCreateBrandDTO;
import com.tuna.ecommerce.domain.request.brand.ReqUpdateBrandDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.BrandRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class BrandService {
    private final BrandRepository brandRepository;

    public Brand handleCreate(ReqCreateBrandDTO req) {
        Brand newBrand = new Brand();
        newBrand.setName(req.getName());
        newBrand.setImage(req.getImage());
        return this.brandRepository.save(newBrand);
    }

    public Brand handleGetById(long id) {
        return this.brandRepository.findById(id).orElse(null);
    }

    public Brand handleUpdate(ReqUpdateBrandDTO req) {
        Brand cur = this.handleGetById(req.getId());
        if (cur != null) {
            cur.setName(req.getName());
            cur.setImage(req.getImage());
            return this.brandRepository.save(cur);
        }
        return null;
    }

    public void handleDelete(long id) {
        this.brandRepository.deleteById(id);
    }

    public ResultPaginationDTO handleGetAll(Specification<Brand> spec, Pageable page) {
        Page<Brand> brands = this.brandRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(brands.getNumber() + 1);
        meta.setPageSize(brands.getSize());
        meta.setPages(brands.getTotalPages());
        meta.setTotal(brands.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(brands.getContent());
        return rs;
    }

    public boolean findByName(String name) {
        return this.brandRepository.findByName(name);
    }
}
