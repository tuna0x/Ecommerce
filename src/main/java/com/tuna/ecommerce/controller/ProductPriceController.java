package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.response.promotion.ResPriceResultDTO;
import com.tuna.ecommerce.service.PricingService;
import com.tuna.ecommerce.service.ProductService;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RequestMapping("/api/v1")
@AllArgsConstructor
@RestController
public class ProductPriceController {
    private final PricingService pricingService;
    private final ProductService productService;

    @GetMapping("/price/{id}")
    public ResponseEntity<ResPriceResultDTO> getPrice(@PathVariable("id") Long id) {
        Product product=this.productService.handleGetById(id);
        return ResponseEntity.ok().body(this.pricingService.calculatePrice(product));
    }

}
