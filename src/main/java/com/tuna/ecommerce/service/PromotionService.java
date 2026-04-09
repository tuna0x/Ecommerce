package com.tuna.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

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
import com.tuna.ecommerce.domain.request.promotion.ReqUpdatePromotionDTO;
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
        promotion.setName(req.getName());
        promotion.setDescription(req.getDescription());
        promotion.setType(req.getType());
        promotion.setDiscountValue(req.getDiscountValue());
        promotion.setMinOrderValue(req.getMinOrderValue());
        promotion.setMaxDiscountValue(req.getMaxDiscountValue());
        promotion.setStartAt(req.getStartAt());
        promotion.setEndAt(req.getEndAt());
        promotion.setActive(req.getActive());
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

    public Promotion updatePromotion(ReqUpdatePromotionDTO dto) {
        if (dto.getId() == null) return null;
        Promotion current = this.getPromotionById(dto.getId());
        if (current != null) {
            current.setName(dto.getName());
            current.setDescription(dto.getDescription());
            current.setType(dto.getType());
            current.setDiscountValue(dto.getDiscountValue());
            current.setMinOrderValue(dto.getMinOrderValue());
            current.setMaxDiscountValue(dto.getMaxDiscountValue());
            current.setStartAt(dto.getStartAt());
            current.setEndAt(dto.getEndAt());
            current.setActive(dto.getActive());
            return this.promotionRepository.save(current);
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
    }

    public boolean existById(Long id) {
        return this.promotionRepository.existsById(id);
    }

    public List<Product> getProductsByPromotionId(Long promotionId) {
        return this.productPromotionRepository.findByPromotionId(promotionId)
                .stream()
                .map(ProductPromotion::getProduct)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignProductsToPromotion(Long promotionId, List<Long> productIds) throws IdInvalidException {
        Promotion promotion = this.getPromotionById(promotionId);
        if (promotion == null) {
            throw new IdInvalidException("Promotion not found with id: " + promotionId);
        }

        // 1. Clear current assignments
        this.productPromotionRepository.deleteByPromotionId(promotionId);

        // 2. Add new ones
        if (productIds != null && !productIds.isEmpty()) {
            List<ProductPromotion> newAssignments = productIds.stream()
                    .map(pid -> {
                        Product p = this.productRepository.findById(pid).orElse(null);
                        if (p != null) {
                            ProductPromotion pp = new ProductPromotion();
                            pp.setProduct(p);
                            pp.setPromotion(promotion);
                            return pp;
                        }
                        return null;
                    })
                    .filter(pp -> pp != null)
                    .collect(Collectors.toList());
            this.productPromotionRepository.saveAll(newAssignments);
        }
    }

    @Transactional
    public void assignAllProductsToPromotion(Long promotionId) throws IdInvalidException {
        Promotion promotion = this.getPromotionById(promotionId);
        if (promotion == null) {
            throw new IdInvalidException("Promotion not found with id: " + promotionId);
        }

        this.productPromotionRepository.deleteByPromotionId(promotionId);

        List<Product> allProducts = this.productRepository.findAll();
        List<ProductPromotion> newAssignments = allProducts.stream()
                .map(p -> {
                    ProductPromotion pp = new ProductPromotion();
                    pp.setProduct(p);
                    pp.setPromotion(promotion);
                    return pp;
                })
                .collect(Collectors.toList());
        this.productPromotionRepository.saveAll(newAssignments);
    }
}
