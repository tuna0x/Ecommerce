package com.tuna.ecommerce.controller;

<<<<<<< HEAD
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
=======
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Brand;
<<<<<<< HEAD
import com.tuna.ecommerce.domain.request.category.ReqCreateCategoryDTO;
import com.tuna.ecommerce.domain.request.category.ReqUpdateCategoryDTO;
=======
import com.tuna.ecommerce.domain.request.brand.ReqCreateBrandDTO;
import com.tuna.ecommerce.domain.request.brand.ReqUpdateBrandDTO;
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.BrandService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

<<<<<<< HEAD
=======
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class BrandController {
    private final BrandService brandService;

<<<<<<< HEAD
     @PostMapping("/brands")
     @APIMessage("Create new brand")
    public ResponseEntity<Brand> createCategory(@Valid @RequestBody Brand Brand) throws IdInvalidException {
        boolean check=this.brandService.findByName(Brand.getName());
        if (check==true) {
            throw new IdInvalidException("name's exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(this.brandService.createBrand(Brand));
=======
    @PostMapping("/brands")
    @APIMessage("Create new brand")
    public ResponseEntity<Brand> createBrand(@Valid @RequestBody ReqCreateBrandDTO brand) {
        return ResponseEntity.ok(this.brandService.createBrand(brand));
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
    }

    @PutMapping("/brands")
    @APIMessage("Update brand")
<<<<<<< HEAD
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
=======
    public ResponseEntity<Brand> updateBrand(@RequestBody ReqUpdateBrandDTO brand) throws IdInvalidException {
        if (this.brandService.getBrandById(brand.getId()) == null) {
            throw new IdInvalidException("Brand not found");
        }
        return ResponseEntity.ok(this.brandService.updateBrand(brand));
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
    }

    @GetMapping("/brands/{id}")
    @APIMessage("Get brand by id")
<<<<<<< HEAD
    public ResponseEntity<Brand> getCategoryById(@PathVariable ("id") long id) throws IdInvalidException{
        Brand cur= this.brandService.getById(id);
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        return ResponseEntity.ok().body(this.brandService.getById(id));
=======
    public ResponseEntity<Brand> getBrandById(@PathVariable Long id) throws IdInvalidException {
        if (this.brandService.getBrandById(id) == null) {
            throw new IdInvalidException("Brand not found");
        }
        return ResponseEntity.ok(this.brandService.getBrandById(id));
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
    }

    @GetMapping("/brands")
    @APIMessage("Get all brands with filter and pagination")
    public ResponseEntity<ResultPaginationDTO> getAllCategory(@Filter Specification<Brand> spec, Pageable page) {
        return ResponseEntity.ok().body(this.brandService.handleGetAll(spec, page));
    }
<<<<<<< HEAD
=======

    @DeleteMapping("/brands/{id}")
    @APIMessage("Delete brand by id")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) throws IdInvalidException {
        if (this.brandService.getBrandById(id) == null) {
            throw new IdInvalidException("Brand not found");
        }
        this.brandService.deleteBrand(id);
        return ResponseEntity.ok().body(null);
    }
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
}
