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

import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.request.category.ReqCreateCategoryDTO;
import com.tuna.ecommerce.domain.request.category.ReqUpdateCategoryDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.category.ResCategoryDTO;
import com.tuna.ecommerce.service.CategoryService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class CategoryController {
        private final CategoryService categoryService;


     @PostMapping("/categories")
     @APIMessage("Create new category")
    public ResponseEntity<ResCategoryDTO> createCategory(@Valid @RequestBody ReqCreateCategoryDTO categoryDTO) throws IdInvalidException {
        boolean check=this.categoryService.findByName(categoryDTO.getName());
        if (check==true) {
            throw new IdInvalidException("Tên danh mục đã tồn tại");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(this.categoryService.convertToResCategoryDTO(this.categoryService.handleCreate(categoryDTO)));
    }

    @PutMapping("/categories")
    @APIMessage("Update category")
    public ResponseEntity<ResCategoryDTO> updateCategory(@Valid @RequestBody ReqUpdateCategoryDTO categoryDTO) throws IdInvalidException{
        Category cur= this.categoryService.handleGetById(categoryDTO.getId());
        if (cur == null) {
            throw new IdInvalidException("Danh mục không tồn tại");
        }

        boolean check = this.categoryService.existsByNameAndIdNot(categoryDTO.getName(), categoryDTO.getId());
        if (check) {
            throw new IdInvalidException("Tên danh mục mới đã được sử dụng");
        }

        return ResponseEntity.ok().body(this.categoryService.convertToResCategoryDTO(this.categoryService.handleUpdate(categoryDTO)));
    }

    @DeleteMapping("/categories/{id}")
    @APIMessage("Delete category by id")
    public ResponseEntity<Void> deleteCategory(@PathVariable ("id") long id) throws IdInvalidException{
        Category cur= this.categoryService.handleGetById(id);
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        this.categoryService.handleDelete(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/categories/{id}")
    @APIMessage("Get category by id")
    public ResponseEntity<ResCategoryDTO> getCategoryById(@PathVariable ("id") long id) throws IdInvalidException{
        Category cur= this.categoryService.handleGetById(id);
        if (cur == null) {
            throw new IdInvalidException("id is not exists");
        }
        return ResponseEntity.ok().body(this.categoryService.convertToResCategoryDTO(cur));
    }

    @GetMapping("/categories/all")
    @APIMessage("Get all categories without pagination (cached)")
    public ResponseEntity<java.util.List<ResCategoryDTO>> getAllCategoriesWithoutPagination() {
        return ResponseEntity.ok().body(this.categoryService.handleGetAll());
    }

    @GetMapping("/categories")
    @APIMessage("Get all categories with filter and pagination")
    public ResponseEntity<ResultPaginationDTO> getAllCategory(
            @Filter Specification<Category> spec,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable page) {
        return ResponseEntity.ok().body(this.categoryService.handleGetAll(spec, page));
    }

    // @GetMapping("/categories")
    // public ResponseEntity<Category> getCategoryByName(@RequestParam String name) {
    //     return ResponseEntity.ok().body(this.categoryService.getCategoryByName(name));
    // }
    
}
