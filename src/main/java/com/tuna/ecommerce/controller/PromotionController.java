package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.request.promotion.ReqCreatePromotionDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.PromotionService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;

import org.hibernate.query.Page;
import org.springframework.beans.factory.BeanRegistry.Spec;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ResponseEntity<Promotion> createPromotion(@RequestBody ReqCreatePromotionDTO promotion) {

        return ResponseEntity.status(HttpStatus.CREATED).body(this.promotionService.createPromotion(promotion));
    }

    @PutMapping("promotions/active/{id}")
    public ResponseEntity<Void> activePromotion(@PathVariable ("id") Long id) {
        this.promotionService.isActive(id);
        return ResponseEntity.ok().body(null);
    }

    @PutMapping("promotions/deactive/{id}")
    public ResponseEntity<Void> deactivePromotion(@PathVariable ("id") Long id) {
        this.promotionService.deActive(id);
        return ResponseEntity.ok().body(null);
    }

    @PutMapping("/promotions")
    @APIMessage("Update Promotion successfully")
    public ResponseEntity<Promotion> updatePromotion(@RequestBody Promotion promotion) throws IdInvalidException {
        Promotion newPromotion= this.promotionService.getPromotionById(promotion.getId());
        if (newPromotion == null) {
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
}
