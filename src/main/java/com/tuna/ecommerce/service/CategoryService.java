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

    @CacheEvict(value = { "categories", "category" }, allEntries = true)
    public Category handleCreate(ReqCreateCategoryDTO req) {
        Category category = new Category();
        category.setName(req.getName());
        category.setDescription(req.getDescription());
        category.setActive(req.getActive());
        if (req.getParentId() != null) {
            Category parent = this.handleGetById(req.getParentId());
            category.setParentCategory(parent);
        }
        category.setSlug(this.generateUniqueSlug(category.getName(), null));
        return this.categoryRepository.save(category);
    }

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
            category.setSlug(this.generateUniqueSlug(category.getName(), category.getId()));
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
    public List<ResCategoryDTO> handleGetAll() {
        // Sắp xếp theo ID giảm dần để thấy cái mới nhất lên đầu
        List<Category> categories = this.categoryRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        return categories.stream().map(this::convertToResCategoryDTO).collect(java.util.stream.Collectors.toList());
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

        res.setProductCount(category.getProductCount() != null ? category.getProductCount() : 0);

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

    public String getCategoriesSummaryForChatbot() {
        List<Category> categories = this.categoryRepository.findAll();
        if (categories == null || categories.isEmpty()) {
            return "Hiện shop chưa phân loại danh mục cụ thể.";
        }
        StringBuilder sb = new StringBuilder("\n--- CÁC DANH MỤC SẢN PHẨM TẠI SHOP ---\n");
        for (Category c : categories) {
            if (c.getActive() != null && c.getActive()) {
                sb.append("- ").append(c.getName());
                if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                    sb.append(": ").append(c.getDescription());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String generateUniqueSlug(String name, Long currentId) {
        String baseSlug = Category.toSlug(name);
        String slug = baseSlug;
        int count = 1;

        while (true) {
            boolean exists;
            if (currentId == null) {
                exists = this.categoryRepository.existsBySlug(slug);
            } else {
                exists = this.categoryRepository.existsBySlugAndIdNot(slug, currentId);
            }

            if (!exists) {
                return slug;
            }
            slug = baseSlug + "-" + count++;
        }
    }
}