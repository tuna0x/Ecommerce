package com.tuna.ecommerce.service;


import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Brand;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.product.ResProductDTO;

import com.tuna.ecommerce.domain.request.brand.ReqCreateBrandDTO;
import com.tuna.ecommerce.domain.request.brand.ReqUpdateBrandDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;

import com.tuna.ecommerce.repository.BrandRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;


    public Brand createBrand(Brand brand){
        return this.brandRepository.save(brand);
    }

    public Brand getById(long id){
        return this.brandRepository.findById(id).orElse(null);
    }

    public Brand updateBrand(Brand brand){
        Brand cur= this.getById(brand.getId());
        if (cur!=null) {
            cur.setName(brand.getName());
            cur.setDescription(brand.getDescription());
            cur=this.brandRepository.save(cur);
        }
        return cur;

    }

    public Brand createBrand(ReqCreateBrandDTO brand) {
        Brand newBrand = new Brand();
        newBrand.setName(brand.getName());
        newBrand.setImage(brand.getImage());
        return this.brandRepository.save(newBrand);
    }

    public Brand getBrandById(Long id) {
        return this.brandRepository.findById(id).orElseThrow(() -> new RuntimeException("Brand not found"));
    }

    public Brand updateBrand(ReqUpdateBrandDTO brand) {
        Brand cur = this.getBrandById(brand.getId());
        if (cur != null) {
            cur.setName(brand.getName());
            cur.setImage(brand.getImage());

        }
        return this.brandRepository.save(cur);
    }

    public void deleteBrand(Long id) {
        this.brandRepository.deleteById(id);
    }

    public ResultPaginationDTO handleGetAll(Specification<Brand> spec, Pageable page) {
        Page<Brand> brand = this.brandRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(brand.getNumber() + 1);
        meta.setPageSize(brand.getSize());
        meta.setPages(brand.getTotalPages());
        meta.setTotal(brand.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(brand.getContent());
        return rs;
    }

    public void deleteBrand(long id){
        this.brandRepository.deleteById(id);
    }

    public boolean findByName(String name) {
       return this.brandRepository.findByName(name);
    }


    

}
