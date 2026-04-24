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
import com.tuna.ecommerce.domain.response.product.ResProductDetailDTO;
import com.tuna.ecommerce.domain.request.productDetail.ReqCreateProductDetailDTO;
import com.tuna.ecommerce.domain.request.productDetail.ReqUpdateProductDetailDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.ProductDetailService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
@Tag(name = "Product Detail", description = "Product Detail Management API")
public class ProductDetailController {
    private final ProductDetailService productDetailService;

    @PostMapping("/product-detail")
    @APIMessage("Create new product detail")
    @Operation(summary = "Create product detail", description = "Create detailed information for a specific product")
    public ResponseEntity<ResProductDetailDTO> createProductDetail(@Valid @RequestBody ReqCreateProductDetailDTO req)
            throws IdInvalidException {
        ProductDetail res = this.productDetailService.createProductDetail(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.productDetailService.convertToResProductDetailDTO(res));
    }

    @PutMapping("/product-detail")
    @APIMessage("Update product detail")
    @Operation(summary = "Update product detail", description = "Update existing detailed information")
    public ResponseEntity<ResProductDetailDTO> updateProductDetail(@RequestBody ReqUpdateProductDetailDTO req)
            throws IdInvalidException {
        ProductDetail cur = this.productDetailService.getById(req.getId());
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        ProductDetail res = this.productDetailService.updateProductDetail(req);
        return ResponseEntity.ok().body(this.productDetailService.convertToResProductDetailDTO(res));
    }

    @DeleteMapping("/product-detail/{id}")
    @APIMessage("Delete product detail by id")
    public ResponseEntity<Void> deleteProductDetail(@PathVariable("id") long id) throws IdInvalidException {
        ProductDetail cur = this.productDetailService.getById(id);
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        this.productDetailService.deleteProductDetail(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/product-detail/{id}")
    @APIMessage("Get product detail by id")
    public ResponseEntity<ResProductDetailDTO> getProductDetailById(@PathVariable("id") long id)
            throws IdInvalidException {
        ProductDetail cur = this.productDetailService.getById(id);
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        return ResponseEntity.ok().body(this.productDetailService.convertToResProductDetailDTO(cur));
    }

    @GetMapping("/product-detail")
    @APIMessage("Get all product-detail with filter and pagination")
    @Operation(summary = "Get paginated product details", description = "Retrieve list of all product details")
    public ResponseEntity<ResultPaginationDTO> getAllProductDetails(@Filter Specification<ProductDetail> spec,
            Pageable page) {
        return ResponseEntity.ok().body(this.productDetailService.handleGetAll(spec, page));
    }
}
