package com.tuna.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductPromotion;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.request.promotion.ReqAssignPromotionDTO;
import com.tuna.ecommerce.domain.request.promotion.ReqCreatePromotionDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.ProductPromotionRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.repository.PromotionRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.tuna.ecommerce.service.NotificationService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final ProductPromotionRepository productPromotionRepository;
    private final NotificationService notificationService;

    public Promotion createPromotion(ReqCreatePromotionDTO req) {
        Promotion promotion = new Promotion();
        promotion.setType(req.getType());
        promotion.setStartAt(req.getStartAt());
        promotion.setEndAt(req.getEndAt());
        promotion.setValue(req.getValue());
        promotion.setActive(false);
        return promotionRepository.save(promotion);
    }

    public void isActive(Long id) throws IdInvalidException {
        Promotion promotion = this.getPromotionById(id);
        if (promotion == null) {
            throw new IdInvalidException("Promotion not found with id: " + id);
        }
        promotion.setActive(true);
        this.promotionRepository.save(promotion);

        // Gửi thông báo cho tất cả người dùng
        this.notificationService.sendNotificationToAllUsers(
            "Khuyến mãi mới!", 
            "Một chương trình khuyến mãi mới vừa được kích hoạt. Hãy kiểm tra ngay!", 
            "PROMOTION_ACTIVE"
        );
    }

    public void deActive(Long id) throws IdInvalidException {
        Promotion promotion = this.getPromotionById(id);
        if (promotion == null) {
            throw new IdInvalidException("Promotion not found with id: " + id);
        }
        promotion.setActive(false);
        this.promotionRepository.save(promotion);
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
            return this.promotionRepository.save(existingPromotion);
        }
        return null;
    }

    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    public ResultPaginationDTO handleGetAll(Specification<Promotion> spec, Pageable page) {
        Page<Promotion> promotionContent = this.promotionRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(promotionContent.getNumber() + 1);
        meta.setPageSize(promotionContent.getSize());
        meta.setPages(promotionContent.getTotalPages());
        meta.setTotal(promotionContent.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(promotionContent.getContent());
        return rs;
    }

    public void applyPromotionToProduct(ReqAssignPromotionDTO req) throws IdInvalidException {
        Promotion promotion = this.getPromotionById(req.getPromotionId());
        if (promotion == null) {
            throw new IdInvalidException("Promotion not found with id: " + req.getPromotionId());
        }

        Product product = productRepository.findById(req.getProductId()).orElse(null);
        if (product == null) {
            throw new IdInvalidException("Product not found with id: " + req.getProductId());
        }

        if (productPromotionRepository.existsByProductIdAndPromotionId(req.getProductId(), req.getPromotionId())) {
            throw new IdInvalidException("Promotion is already applied to this product.");
        }

        ProductPromotion productPromotion = new ProductPromotion();
        productPromotion.setProduct(product);
        productPromotion.setPromotion(promotion);
        this.productPromotionRepository.save(productPromotion);
        
        // Ensure promotion marked as active if applied to a product
        promotion.setActive(true);
        this.promotionRepository.save(promotion);
    }

    public boolean existById(Long id) {
        return this.promotionRepository.existsById(id);
    }
}
