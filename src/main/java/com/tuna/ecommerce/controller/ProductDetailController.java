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

import com.tuna.ecommerce.domain.ProductDetail;
import com.tuna.ecommerce.domain.request.productDetail.ReqCreateProductDetailDTO;
import com.tuna.ecommerce.domain.request.productDetail.ReqUpdateProductDetailDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.ProductDetailService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("api/v1")
@AllArgsConstructor
public class ProductDetailController {
    private final ProductDetailService  productDetailService;
     @PostMapping("/product-detail")
     @APIMessage("Create new product detail")
    public ResponseEntity<ProductDetail> createCategory(@Valid @RequestBody ReqCreateProductDetailDTO req) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.productDetailService.createProductDetail(req));
    }

    @PutMapping("/product-detail")
    @APIMessage("Update product detail")
    public ResponseEntity<ProductDetail> updateCategory(@RequestBody ReqUpdateProductDetailDTO req) throws IdInvalidException{
        ProductDetail cur= this.productDetailService.getById(req.getId());
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        return ResponseEntity.ok().body(this.productDetailService.updateProductDetail(req));
    }

    @DeleteMapping("/product-detail/{id}")
    @APIMessage("Delete product detail by id")
    public ResponseEntity<Void> deleteCategory(@PathVariable ("id") long id) throws IdInvalidException{
        ProductDetail cur= this.productDetailService.getById(id);
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        this.productDetailService.deleteProductDetail(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/product-detail/{id}")
    @APIMessage("Get product detail by id")
    public ResponseEntity<ProductDetail> getCategoryById(@PathVariable ("id") long id) throws IdInvalidException{
        ProductDetail cur= this.productDetailService.getById(id);
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        return ResponseEntity.ok().body(this.productDetailService.getById(id));
    }

    @GetMapping("/product-detail")
    @APIMessage("Get all product-detail with filter and pagination")
    public ResponseEntity<ResultPaginationDTO> getAllCategory(@Filter Specification<ProductDetail> spec, Pageable page) {
        return ResponseEntity.ok().body(this.productDetailService.handleGetAll(spec, page));
    }
}
