package com.tuna.ecommerce.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.request.category.ReqCreateCategoryDTO;
import com.tuna.ecommerce.domain.request.category.ReqUpdateCategoryDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.category.ResCategoryDTO;
import com.tuna.ecommerce.repository.CategoryRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @CacheEvict(value = "categories", allEntries = true)
    public Category handleCreate(ReqCreateCategoryDTO req) {
        Category category = new Category();
        category.setName(req.getName());
        category.setDescription(req.getDescription());
        category.setActive(req.getActive());
        if (req.getParentId() != null) {
            Category parent = this.handleGetById(req.getParentId());
            category.setParentCategory(parent);
        }
        return this.categoryRepository.save(category);
    }

    @Cacheable(value = "category", key = "#id", unless = "#result == null")
    public Category handleGetById(Long id) {
        if (id == null)
            return null;
        Optional<Category> category = this.categoryRepository.findById(id);
        return category.isPresent() ? category.get() : null;
    }

    @CacheEvict(value = { "categories", "category" }, allEntries = true)
    public Category handleUpdate(ReqUpdateCategoryDTO req) {
        Category category = this.handleGetById(req.getId());
        if (category != null) {
            category.setName(req.getName());
            category.setDescription(req.getDescription());
            category.setActive(req.getActive());
            if (req.getParentId() != null) {
                Category parent = this.handleGetById(req.getParentId());
                category.setParentCategory(parent);
            } else {
                category.setParentCategory(null);
            }
            category = this.categoryRepository.save(category);
        }
        return category;
    }

    @CacheEvict(value = { "categories", "category" }, allEntries = true)
    public void handleDelete(Long id) throws IdInvalidException {
        Category category = this.handleGetById(id);
        if (category != null) {
            if (category.getProducts() != null && !category.getProducts().isEmpty()) {
                throw new IdInvalidException("Không thể xóa danh mục vì vẫn còn sản phẩm liên kết.");
            }
            if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
                throw new IdInvalidException("Không thể xóa danh mục vì vẫn còn các danh mục con.");
            }
            this.categoryRepository.deleteById(id);
        }
    }

    @Cacheable(value = "categories")
    public List<Category> handleGetAll() {
        return this.categoryRepository.findAll();
    }

    public ResultPaginationDTO handleGetAll(Specification<Category> spec, Pageable page) {
        Page<Category> categoryPage = this.categoryRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(categoryPage.getNumber() + 1);
        meta.setPageSize(categoryPage.getSize());
        meta.setPages(categoryPage.getTotalPages());
        meta.setTotal(categoryPage.getTotalElements());

        rs.setMeta(meta);

        List<ResCategoryDTO> list = categoryPage.getContent().stream()
                .map(this::convertToResCategoryDTO)
                .toList();

        rs.setResult(list);
        return rs;
    }

    public ResCategoryDTO convertToResCategoryDTO(Category category) {
        ResCategoryDTO res = new ResCategoryDTO();
        res.setId(category.getId());
        res.setName(category.getName());
        res.setDescription(category.getDescription());
        res.setSlug(category.getSlug());
        res.setActive(category.getActive());
        res.setCreatedAt(category.getCreatedAt());
        res.setUpdatedAt(category.getUpdatedAt());

        if (category.getParentCategory() != null) {
            res.setParentCategory(new ResCategoryDTO.ParentCategory(
                    category.getParentCategory().getId(),
                    category.getParentCategory().getName()));
        }

        if (category.getProducts() != null) {
            res.setProductCount(category.getProducts().size());
        } else {
            res.setProductCount(0);
        }

        return res;
    }

    public boolean findByName(String name) {
        return this.categoryRepository.existsByName(name);
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return this.categoryRepository.existsByNameAndIdNot(name, id);
    }

    public Category getCategoryByName(String name) {
        return this.categoryRepository.findByName(name);
    }

    public List<Long> getAllIdsInHierarchy(Long parentId) {
        java.util.Set<Long> allIds = new java.util.HashSet<>();
        if (parentId == null)
            return new java.util.ArrayList<>();

        collectIdsRecursively(parentId, allIds);
        return new java.util.ArrayList<>(allIds);
    }

    private void collectIdsRecursively(Long parentId, java.util.Set<Long> allIds) {
        if (!allIds.add(parentId)) {
            return; // Already processed to prevent infinite loop
        }

        List<Category> children = this.categoryRepository.findByParentCategory_Id(parentId);
        if (children != null) {
            for (Category child : children) {
                collectIdsRecursively(child.getId(), allIds);
            }
        }
    }
}