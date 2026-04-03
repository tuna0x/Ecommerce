package com.tuna.ecommerce.service;


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
    private final CloudinaryService cloudinaryService;

    public Brand handleCreate(ReqCreateBrandDTO req, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        Brand newBrand = new Brand();
        newBrand.setName(req.getName());
        newBrand.setDescription(req.getDescription());
        newBrand.setImage(req.getImage());
        newBrand.setIsFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false);
        newBrand.setActive(req.getActive() != null ? req.getActive() : true);
        
        if (file != null && !file.isEmpty()) {
            java.util.Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
            newBrand.setImage(uploadResult.get("secure_url").toString());
            newBrand.setPublicId(uploadResult.get("public_id").toString());
        }
        
        return this.brandRepository.save(newBrand);
    }

    public Brand handleGetById(Long id) {
        if (id == null) return null;
        return this.brandRepository.findById(id).orElse(null);
    }

    public Brand handleUpdate(ReqUpdateBrandDTO req, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (req == null || req.getId() == null) {
            return null;
        }
        Brand cur = this.handleGetById(req.getId());
        if (cur != null) {
            cur.setName(req.getName());
            cur.setDescription(req.getDescription());
            cur.setIsFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false);
            cur.setActive(req.getActive() != null ? req.getActive() : true);
            
            if (file != null && !file.isEmpty()) {
                // Delete old image if exists
                if (cur.getPublicId() != null) {
                    this.cloudinaryService.deleteFile(cur.getPublicId());
                }
                
                java.util.Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
                cur.setImage(uploadResult.get("secure_url").toString());
                cur.setPublicId(uploadResult.get("public_id").toString());
            }
            
            return this.brandRepository.save(cur);
        }
        return null;
    }

    public void handleDelete(Long id) throws java.io.IOException {
        Brand cur = this.handleGetById(id);
        if (cur != null) {
            if (cur.getPublicId() != null) {
                this.cloudinaryService.deleteFile(cur.getPublicId());
            }
            this.brandRepository.deleteById(id);
        }
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

    public boolean existsByName(String name) {
        return this.brandRepository.existsByName(name);
    }
}
