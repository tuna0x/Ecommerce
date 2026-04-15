package com.tuna.ecommerce.service;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.Wishlist;
import com.tuna.ecommerce.domain.response.product.ResProductDTO;
import com.tuna.ecommerce.repository.WishlistRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final UserService userService;
    private final ProductService productService;

    public User findCurrentUser() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : null;
        if (email == null) return null;
        return this.userService.findByUsername(email);
    }

    @Transactional
    public Wishlist addToWishlist(Long productId) {
        User user = findCurrentUser();
        if (user == null) return null;

        // Check if already in wishlist
        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(user.getId(), productId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Product product = productService.handleGetById(productId);
        if (product == null) return null;

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        return wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long productId) {
        User user = findCurrentUser();
        if (user == null) return;
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    @Transactional(readOnly = true)
    public List<ResProductDTO> getWishlistProducts() {
        User user = findCurrentUser();
        if (user == null) return List.of();
        List<Wishlist> wishlists = wishlistRepository.findByUserId(user.getId());
        return wishlists.stream()
                .map(wishlist -> productService.convertToResProductDTO(wishlist.getProduct()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(Long productId) {
        User user = findCurrentUser();
        if (user == null) return false;
        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }
}
