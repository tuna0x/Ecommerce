package com.tuna.ecommerce.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Brand;

import com.tuna.ecommerce.domain.request.category.ReqCreateCategoryDTO;
import com.tuna.ecommerce.domain.request.category.ReqUpdateCategoryDTO;

import com.tuna.ecommerce.domain.request.brand.ReqCreateBrandDTO;
import com.tuna.ecommerce.domain.request.brand.ReqUpdateBrandDTO;

import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.BrandService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class BrandController {
    private final BrandService brandService;

     @PostMapping("/brands")
     @APIMessage("Create new brand")
    public ResponseEntity<Brand> createCategory(@Valid @RequestBody Brand Brand) throws IdInvalidException {
        boolean check=this.brandService.findByName(Brand.getName());
        if (check==true) {
            throw new IdInvalidException("name's exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(this.brandService.createBrand(Brand));
    }


    @PutMapping("/brands")
    @APIMessage("Update brand")

    public ResponseEntity<Brand> updateCategory(@RequestBody Brand Brand) throws IdInvalidException{
        Brand cur= this.brandService.getById(Brand.getId());
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        return ResponseEntity.ok().body(this.brandService.updateBrand(Brand));
    }


    @DeleteMapping("/brands/{id}")
    @APIMessage("Delete brand by id")
    public ResponseEntity<Void> deleteCategory(@PathVariable ("id") long id) throws IdInvalidException{
        Brand cur= this.brandService.getById(id);
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        this.brandService.deleteBrand(id);
        return ResponseEntity.ok().body(null);
    }


    @GetMapping("/brands/{id}")
    @APIMessage("Get brand by id")
    public ResponseEntity<Brand> getBrandById(@PathVariable Long id) throws IdInvalidException {
        if (this.brandService.getBrandById(id) == null) {
            throw new IdInvalidException("Brand not found");
        }
        return ResponseEntity.ok(this.brandService.getBrandById(id));

    }

    @GetMapping("/brands")
    @APIMessage("Get all brands with filter and pagination")
    public ResponseEntity<ResultPaginationDTO> getAllCategory(@Filter Specification<Brand> spec, Pageable page) {
        return ResponseEntity.ok().body(this.brandService.handleGetAll(spec, page));
    }



}
