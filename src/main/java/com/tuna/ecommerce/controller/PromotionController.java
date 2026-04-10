package com.tuna.ecommerce.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.request.promotion.ReqCreatePromotionDTO;
import com.tuna.ecommerce.domain.request.promotion.ReqUpdatePromotionDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.PromotionService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;




@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

    @PostMapping("/promotions")
    @APIMessage("Create Promotion successfully")
    public ResponseEntity<Promotion> createPromotion(@RequestBody ReqCreatePromotionDTO promotion) throws IdInvalidException {

        return ResponseEntity.status(HttpStatus.CREATED).body(this.promotionService.createPromotion(promotion));
    }

    @PatchMapping("/promotions/{id}/active")
    @APIMessage("Toggle Promotion active status")
    public ResponseEntity<Void> toggleActive(@PathVariable("id") Long id, @RequestParam("active") boolean active) throws IdInvalidException {
        if (active) {
            this.promotionService.isActive(id);
        } else {
            this.promotionService.deActive(id);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/promotions")
    @APIMessage("Update Promotion successfully")
    public ResponseEntity<Promotion> updatePromotion(@RequestBody ReqUpdatePromotionDTO promotion) throws IdInvalidException {
        if (this.promotionService.getPromotionById(promotion.getId()) == null) {
            throw new IdInvalidException("Promotion not found");
        }
        return ResponseEntity.ok(this.promotionService.updatePromotion(promotion));
    }

    @GetMapping("/promotions/{id}")
    @APIMessage("Get Promotion successfully")
    public ResponseEntity<Promotion> getPromotionById(@PathVariable("id") Long id) throws IdInvalidException {
        Promotion promotion = this.promotionService.getPromotionById(id);
        if (promotion == null) {
            throw new IdInvalidException("Promotion not found");
        }
        return ResponseEntity.ok().body(promotion);
    }

    @GetMapping("/promotions")
    @APIMessage("Get all Promotions successfully")
    public ResponseEntity<ResultPaginationDTO> getAllPromotion(@Filter Specification<Promotion> spec,Pageable   page) {
        return ResponseEntity.ok(this.promotionService.handleGetAll(spec, page));
    }

    @DeleteMapping("/promotions/{id}")
    @APIMessage("Delete Promotion successfully")
    public ResponseEntity<Void> deletePromotion(@PathVariable("id") Long id) throws IdInvalidException {
        if (!this.promotionService.existById(id)) {
            throw new IdInvalidException("Promotion not found");
        }
        this.promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/promotions/{id}/products")
    @APIMessage("Get assigned products successfully")
    public ResponseEntity<List<com.tuna.ecommerce.domain.Product>> getAssignedProducts(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.promotionService.getProductsByPromotionId(id));
    }

    @PostMapping("/promotions/{id}/products")
    @APIMessage("Assign products to promotion successfully")
    public ResponseEntity<Void> assignProducts(@PathVariable("id") Long id, @RequestBody List<Long> productIds) throws IdInvalidException {
        this.promotionService.assignProductsToPromotion(id, productIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/promotions/{id}/products/all")
    @APIMessage("Assign all products to promotion successfully")
    public ResponseEntity<Void> assignAllProducts(@PathVariable("id") Long id) throws IdInvalidException {
        this.promotionService.assignAllProductsToPromotion(id);
        return ResponseEntity.ok().build();
    }
}
