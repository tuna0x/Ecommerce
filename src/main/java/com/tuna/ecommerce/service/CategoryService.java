package com.tuna.ecommerce.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.request.category.ReqCreateCategoryDTO;
import com.tuna.ecommerce.domain.request.category.ReqUpdateCategoryDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.CategoryRepository;
import com.tuna.ecommerce.repository.ProductRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CategoryService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Category handleCreate(ReqCreateCategoryDTO req){
        Category category=new Category();
        category.setName(req.getName());
        category.setDescription(req.getDescription());
        
        if (req.getParentId() !=null) {
            Category parent = this.handleGetById(req.getParentId());
            category.setParentCategory(parent);
        }
        return this.categoryRepository.save(category);
    }

    public Category handleGetById(long id){
        Optional<Category> category=this.categoryRepository.findById(id);
        return category.isPresent() ? category.get() : null;
    }

    public Category handleUpdate(ReqUpdateCategoryDTO req){
        Category category = this.handleGetById(req.getId());
        if (category != null) {
            category.setName(req.getName());
            category.setDescription(req.getDescription());
            if (req.getParentId() !=null) {
                Category parent = this.handleGetById(req.getParentId());
                category.setParentCategory(parent);
            }
            category=this.categoryRepository.save(category);
        }
        return category;
    }

    public void handleDelete(long id){
        Category category =this.handleGetById(id);
        // delete product
        category.getProducts().forEach(product-> productRepository.delete(product));
        //delete category
        this.categoryRepository.deleteById(id);
    }

    public ResultPaginationDTO handleGetAll(Specification<Category> spec,Pageable page){
         Page<Category> category= this.categoryRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(category.getNumber() + 1);
        meta.setPageSize(category.getSize());
        meta.setPages(category.getTotalPages());
        meta.setTotal(category.getTotalElements());

        rs.setResult(category.getContent());
        return rs;
    }

    public boolean findByName(String name){
        return this.productRepository.existsByName(name);
    }
}