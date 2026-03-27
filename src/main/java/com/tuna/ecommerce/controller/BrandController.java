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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import com.tuna.ecommerce.domain.Brand;
import com.tuna.ecommerce.domain.request.brand.ReqCreateBrandDTO;
import com.tuna.ecommerce.domain.request.brand.ReqUpdateBrandDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.BrandService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @PostMapping("/brands")
    @APIMessage("Create new brand")
    public ResponseEntity<Brand> createBrand(@Valid @ModelAttribute ReqCreateBrandDTO brand) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.brandService.handleCreate(brand, brand.getFile()));
    }

    @PutMapping("/brands")
    @APIMessage("Update brand")
    public ResponseEntity<Brand> updateBrand(@Valid @ModelAttribute ReqUpdateBrandDTO brand) throws IdInvalidException, IOException {
        if (brand == null || brand.getId() == null) { // Added null check for brand and brand.getId()
            throw new IdInvalidException("Brand ID cannot be null for update operation");
        }
        Brand updatedBrand = this.brandService.handleUpdate(brand, brand.getFile());
        if (updatedBrand == null) {
            throw new IdInvalidException("Brand not found");
        }
        return ResponseEntity.ok().body(updatedBrand);
    }

    @DeleteMapping("/brands/{id}")
    @APIMessage("Delete brand by id")
    public ResponseEntity<Void> deleteBrand(@PathVariable("id") long id) throws IdInvalidException, IOException {
        Brand cur = this.brandService.handleGetById(id);
        if (cur == null) {
            throw new IdInvalidException("Brand not found with id: " + id);
        }
        this.brandService.handleDelete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/brands/{id}")
    @APIMessage("Get brand by id")
    public ResponseEntity<Brand> getBrandById(@PathVariable Long id) throws IdInvalidException {
        if (id == null) { // Added null check for id
            throw new IdInvalidException("Brand ID cannot be null");
        }
        Brand brand = this.brandService.handleGetById(id);
        if (brand == null) {
            throw new IdInvalidException("Brand not found with id: " + id);
        }
        return ResponseEntity.ok(brand);
    }

    @GetMapping("/brands")
    @APIMessage("Get all brands with filter and pagination")
    public ResponseEntity<ResultPaginationDTO> getAllBrands(@Filter Specification<Brand> spec, Pageable page) {
        return ResponseEntity.ok().body(this.brandService.handleGetAll(spec, page));
    }
}
