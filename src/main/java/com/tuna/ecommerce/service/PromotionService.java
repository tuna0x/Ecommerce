package com.tuna.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductPromotion;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.ProductPromotionRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.repository.PromotionRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final ProductPromotionRepository productPromotionRepository;

    public Promotion createPromotion(Promotion promotion) {
        return promotionRepository.save(promotion);
    }

    public Promotion getPromotionById(Long id) {
        return promotionRepository.findById(id).orElse(null);
    }

    public Promotion updatePromotion(Promotion promotion) {
       Promotion existingPromotion = this.getPromotionById(promotion.getId());
       if (existingPromotion != null) {
           existingPromotion.setType(promotion.getType());
            existingPromotion.setValue(promotion.getValue());
           existingPromotion.setStartAt(promotion.getStartAt());
           existingPromotion.setEndAt(promotion.getEndAt());
           promotion = this.promotionRepository.save(existingPromotion);
       }
       return promotion;
    }

    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

        public ResultPaginationDTO handleGetAll(Specification<Promotion> spec,Pageable page){
        Page<Promotion> promotion= this.promotionRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(promotion.getNumber() + 1);
        meta.setPageSize(promotion.getSize());
        meta.setPages(promotion.getTotalPages());
        meta.setTotal(promotion.getTotalElements());


        rs.setResult(promotion.getContent());
        return rs;
    }

    public void applyPromotionToProduct(Long promotionId, Long productId) throws IdInvalidException {
        Promotion promotion = this.getPromotionById(promotionId);
        Product product = productRepository.findById(productId).orElse(null);
        if (promotion==null) {
            throw new IdInvalidException("Promotion not found");
        }
        if (product == null) {
            throw new IdInvalidException("Product not found");
        }
        if (productPromotionRepository.existsByProductIdAndPromotionId(productId, promotionId)) {
            throw new IdInvalidException("Promotion already applied to this product");
        }

        ProductPromotion productPromotion = new ProductPromotion();
        productPromotion.setProduct(product);
        productPromotion.setPromotion(promotion);
        this.productPromotionRepository.save(productPromotion);
    }
}
