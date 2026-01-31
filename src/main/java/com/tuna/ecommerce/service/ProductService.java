package com.tuna.ecommerce.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductAttributeValue;
import com.tuna.ecommerce.domain.request.product.ReqCreateProductDTO;
import com.tuna.ecommerce.domain.request.product.ReqUpdateProductDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.product.ResProductDTO;
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.repository.AttributeValueRepository;
import com.tuna.ecommerce.repository.ProductAttributeValueRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {
      private final ProductRepository productRepository;
      private final AttributeValueRepository attributeValueRepository;
      private final ProductAttributeValueRepository productAttributeValueRepository;
      private final CategoryService categoryService;

    public Product handleCreate(ReqCreateProductDTO product) throws IdInvalidException{
        Category category=this.categoryService.handleGetById(product.getCategoryId());
        if (category==null) {
            throw new IdInvalidException("Id invalid");
        }
        Product newProduct=new Product();
        newProduct.setName(product.getName());
        newProduct.setDescription(product.getDescription());
        newProduct.setOriginalPrice(product.getOriginalPrice());
        newProduct.setStock(product.getStock());
        newProduct.setImage(product.getImage());
        newProduct.setCategory(category);
        this.productRepository.save(newProduct);

        for(Long id: product.getAttributeValue()){
            AttributeValue attributeValue=this.attributeValueRepository.findById(id).orElse(null);
              if (this.productAttributeValueRepository.existsByProductIdAndAttributeValueId(newProduct.getId(), id)) {
                throw new RuntimeException("Duplicate attribute value");
              }
              ProductAttributeValue pav=new ProductAttributeValue();
                pav.setProduct(newProduct);
                pav.setAttributeValue(attributeValue);
                this.productAttributeValueRepository.save(pav);
            }
        return newProduct;
    }

    public Product handleGetById(long id){
        return this.productRepository.findById(id).orElse(null);
    }

    public Product handleUpdate(ReqUpdateProductDTO product) throws IdInvalidException{
        Category category = this.categoryService.handleGetById(product.getCategoryId());
        if (category == null) {
            throw new IdInvalidException("Id invalid");
        }

        Product cur = this.handleGetById(product.getId());
        if (cur != null) {
            cur.setId(product.getId());
            cur.setName(product.getName());
            cur.setDescription(product.getDescription());
            cur.setImage(product.getImage());
            cur.setOriginalPrice(product.getOriginalPrice());
            cur.setStock(product.getStock());
            cur.setCategory(category);
        }
        return this.productRepository.save(cur);
    }

    public void handleDelete(long id){
        this.productRepository.deleteById(id);
    }

    public ResultPaginationDTO handleGetAll(Specification<Product> spec,Pageable page){
        Page<Product> product= this.productRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(product.getNumber() + 1);
        meta.setPageSize(product.getSize());
        meta.setPages(product.getTotalPages());
        meta.setTotal(product.getTotalElements());

        rs.setMeta(meta);
        List<ResProductDTO> list=product.getContent().stream().map(item->this.convertToResProductDTO(item)).collect(Collectors.toList());
        rs.setResult(list);
        return rs;
    }

    public boolean findByName(String name){
        return this.productRepository.existsByName(name);
    }

    public double findByOriginalPrice(long id){
        return this.findByOriginalPrice(id);
    }


    public ResProductDTO convertToResProductDTO(Product product){
        ResProductDTO res=new ResProductDTO();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setDescription(product.getDescription());
        res.setImage(product.getImage());
        res.setOriginalPrice(product.getOriginalPrice());
        res.setStock(product.getStock());

        ResProductDTO.CategoryInner categoryInner=new ResProductDTO.CategoryInner();
        categoryInner.setId(product.getCategory().getId());
        Category category= this.categoryService.handleGetById(product.getCategory().getId());
        categoryInner.setName(category.getName());
        res.setCategory(categoryInner);

        return res;
    }
}
