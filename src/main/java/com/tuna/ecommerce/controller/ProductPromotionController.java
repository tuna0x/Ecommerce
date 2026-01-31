package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.request.promotion.ReqAssignPromotionDTO;
import com.tuna.ecommerce.service.PromotionService;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RequestMapping("/api/v1")
@RestController
@AllArgsConstructor
public class ProductPromotionController {
    private final PromotionService promotionService;

    @PostMapping("/product-promotions")
    public ResponseEntity<Void> assignPromotion(@RequestBody ReqAssignPromotionDTO req) throws IdInvalidException {
        this.promotionService.applyPromotionToProduct(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

}
