package com.tuna.ecommerce.controller;

import org.hibernate.query.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.request.product.ReqCreateProductDTO;
import com.tuna.ecommerce.domain.request.product.ReqUpdateProductDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.product.ResProductDTO;
import com.tuna.ecommerce.service.ProductService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping("/products")
    @APIMessage("Product created successfully")
    public ResponseEntity<ResProductDTO> createProduct(@RequestBody ReqCreateProductDTO product) throws IdInvalidException {
        Product cur=this.productService.handleCreate(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.productService.convertToResProductDTO(cur));

    }


    @PutMapping("/products")
    @APIMessage("Product updated successfully")
    public ResponseEntity<ResProductDTO> updateProduct(@RequestBody ReqUpdateProductDTO product) throws IdInvalidException {
        Product existingProduct = this.productService.handleGetById(product.getId());
        if (existingProduct == null) {
            throw new IdInvalidException("Id invalid");
        }
        existingProduct=this.productService.handleUpdate(product);
        return ResponseEntity.ok().body(this.productService.convertToResProductDTO(existingProduct));
    }

    @GetMapping("/products/{id}")
    @APIMessage("Get product by id successfully")
    public ResponseEntity<ResProductDTO> getProductById(@PathVariable Long id) throws IdInvalidException {
        Product product = this.productService.handleGetById(id);
        if (product == null) {
            throw new IdInvalidException("Id invalid");
        }
        return ResponseEntity.ok().body(this.productService.convertToResProductDTO(product));
    }

    @GetMapping("/products")
    @APIMessage("Get all products successfully")
    public ResponseEntity<ResultPaginationDTO> getAllProduct(@Filter Specification<Product> spec, Pageable pageable) {
        return ResponseEntity.ok().body(this.productService.handleGetAll(spec, pageable));
    }

    @DeleteMapping("/products/{id}")
    @APIMessage("Product deleted successfully")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) throws IdInvalidException {
        Product product = this.productService.handleGetById(id);
        if (product == null) {
            throw new IdInvalidException("Id invalid");
        }
        this.productService.handleDelete(id);
        return ResponseEntity.ok().body(null);
    }
}
