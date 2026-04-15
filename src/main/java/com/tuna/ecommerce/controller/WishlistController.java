package com.tuna.ecommerce.controller;

import com.tuna.ecommerce.domain.response.product.ResProductDTO;
import com.tuna.ecommerce.service.WishlistService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    @PostMapping("/wishlist/{productId}")
    @APIMessage("Added to wishlist successfully")
    public ResponseEntity<Void> addToWishlist(@PathVariable Long productId) {
        wishlistService.addToWishlist(productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/wishlist/{productId}")
    @APIMessage("Removed from wishlist successfully")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wishlist")
    @APIMessage("Get wishlist successfully")
    public ResponseEntity<List<ResProductDTO>> getWishlist() {
        return ResponseEntity.ok().body(wishlistService.getWishlistProducts());
    }

    @GetMapping("/wishlist/check/{productId}")
    @APIMessage("Check wishlist status successfully")
    public ResponseEntity<Boolean> checkWishlist(@PathVariable Long productId) {
        return ResponseEntity.ok().body(wishlistService.isInWishlist(productId));
    }
}
