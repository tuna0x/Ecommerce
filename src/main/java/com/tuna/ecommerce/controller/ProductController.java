package com.tuna.ecommerce.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
@Tag(name = "Product", description = "Product Management API")
public class ProductController {
    private final ProductService productService;

    @PostMapping("/products")
    @APIMessage("Product created successfully")
    @Operation(summary = "Create a new product", description = "Create a product with data and multiple files")
    public ResponseEntity<ResProductDTO> createProduct(@RequestPart("data") ReqCreateProductDTO product,
            @RequestPart(value = "files", required = false) List<MultipartFile> files)
            throws IdInvalidException, IOException {
        Product cur = this.productService.handleCreate(product, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.productService.convertToResProductDTO(cur));

    }

    @PutMapping("/products")
    @APIMessage("Product updated successfully")
    @Operation(summary = "Update an existing product", description = "Update product details and images")
    public ResponseEntity<ResProductDTO> updateProduct(@RequestPart("data") ReqUpdateProductDTO product,
            @RequestPart(value = "files", required = false) List<MultipartFile> files)
            throws IdInvalidException, IOException {
        Product existingProduct = this.productService.handleGetById(product.getId());
        if (existingProduct == null) {
            throw new IdInvalidException("Id invalid");
        }
        existingProduct = this.productService.handleUpdate(product, files);
        return ResponseEntity.ok().body(this.productService.convertToResProductDTO(existingProduct));
    }

    @GetMapping("/products/{id}")
    @APIMessage("Get product by id successfully")
    @Operation(summary = "Get a product by ID", description = "Retrieve full details of a specific product")
    public ResponseEntity<ResProductDTO> getProductById(@PathVariable Long id) throws IdInvalidException {
        Product product = this.productService.handleGetById(id);
        if (product == null) {
            throw new IdInvalidException("Id invalid");
        }
        return ResponseEntity.ok().body(this.productService.convertToResProductDTO(product));
    }

    @GetMapping("/products")
    @APIMessage("Get all products successfully")
    @Operation(summary = "Get paginated products", description = "Retrieve list of products with filtering and searching")
    public ResponseEntity<ResultPaginationDTO> getAllProduct(
            @Filter Specification<Product> spec,
            @org.springframework.web.bind.annotation.RequestParam(value = "categoryId", required = false) Long categoryId,
            @org.springframework.web.bind.annotation.RequestParam(value = "search", required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
            Pageable pageable) {
        return ResponseEntity.ok().body(this.productService.handleGetAll(spec, categoryId, search, pageable, isPublic));
    }

    @GetMapping("/products/flash-sale")
    @APIMessage("Get all flash sale products successfully")
    public ResponseEntity<ResultPaginationDTO> getAllFlashSale(Pageable pageable) {
        return ResponseEntity.ok().body(this.productService.handleGetFlashSale(pageable));
    }

    @DeleteMapping("/products/{id}")
    @APIMessage("Product deleted successfully")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) throws IdInvalidException, IOException {
        Product product = this.productService.handleGetById(id);
        if (product == null) {
            throw new IdInvalidException("Id invalid");
        }
        this.productService.handleDelete(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/products/{id}/related")
    @APIMessage("Get related products successfully")
    public ResponseEntity<List<ResProductDTO>> getRelatedProducts(@PathVariable Long id) {
        return ResponseEntity.ok().body(this.productService.getRelatedProducts(id));
    }
}
