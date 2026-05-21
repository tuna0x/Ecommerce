package com.tuna.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.request.cart.ReqAddToCartDTO;
import com.tuna.ecommerce.domain.request.cart.ReqUpdateQuantityDTO;
import com.tuna.ecommerce.domain.response.cart.ResAddToCart;
import com.tuna.ecommerce.domain.response.cart.ResGetCart;
import com.tuna.ecommerce.service.CartService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping("/cart")
    @APIMessage("Add to cart successfully")
    public ResponseEntity<ResAddToCart> addToCart(@Valid @RequestBody ReqAddToCartDTO req) throws IdInvalidException {
        Cart cart=this.cartService.addToCart(req);
        return ResponseEntity.ok().body(this.cartService.convertToLightResAddToCart(cart));
    }

    @GetMapping("/cart")
    @APIMessage("Get cart successfully")
    public ResponseEntity<ResGetCart> getCart() {
        return ResponseEntity.ok().body(this.cartService.getCurrentCartSummary());
    }

    @PutMapping("/cart")
    @APIMessage("Update cart item quantity successfully")
    public ResponseEntity<ResAddToCart> updateQuantity(@Valid @RequestBody ReqUpdateQuantityDTO req) throws IdInvalidException {
        Cart cart = this.cartService.updateQuantity(req);
        return ResponseEntity.ok().body(this.cartService.convertToLightResAddToCart(cart));
    }

    @DeleteMapping("/cart/{id}")
    @APIMessage("Remove item from cart successfully")
    public ResponseEntity<Void> removeItem(@PathVariable("id") long id) throws IdInvalidException {
        this.cartService.removeItem(id);
        return ResponseEntity.ok().build();
    }
}

