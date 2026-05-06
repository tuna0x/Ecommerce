package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductVariant;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.cart.ReqAddToCartDTO;
import com.tuna.ecommerce.domain.request.cart.ReqUpdateQuantityDTO;
import com.tuna.ecommerce.domain.response.cart.ResAddToCart;
import com.tuna.ecommerce.domain.response.cart.ResGetCart;
import com.tuna.ecommerce.repository.CartItemRepository;
import com.tuna.ecommerce.repository.CartRepository;
import com.tuna.ecommerce.repository.ProductVariantRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;

    public Cart getOrCreateCart() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) return null;

        User user = this.userRepository.findByEmail(email);
        if (user == null) return null;

        Cart cart = this.cartRepository.findByUser(user);
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart.setItems(new ArrayList<>());
            cart = this.cartRepository.save(cart);
        } else {
            java.util.Map<String, CartItem> uniqueItems = new java.util.HashMap<>();
            List<CartItem> toRemove = new ArrayList<>();
            boolean changed = false;

            for (CartItem item : cart.getItems()) {
                // 1. Dynamic Price Sync
                com.tuna.ecommerce.domain.response.product.ResProductDTO productDTO = this.productService.convertToResProductDTO(item.getProduct());
                BigDecimal currentPrice = productDTO.getFinalPrice();
                if (item.getProductVariant() != null && productDTO.getVariants() != null) {
                    for (com.tuna.ecommerce.domain.response.product.ResProductDTO.ProductVariantInner vi : productDTO.getVariants()) {
                        if (vi.getId() == item.getProductVariant().getId()) {
                            currentPrice = vi.getFinalPrice() != null ? vi.getFinalPrice() : vi.getPrice();
                            break;
                        }
                    }
                }
                if (currentPrice != null && (item.getUnitPrice() == null || item.getUnitPrice().compareTo(currentPrice) != 0)) {
                    item.setUnitPrice(currentPrice);
                    item.setTotalPrice(currentPrice.multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                    changed = true;
                }

                // 2. Duplicate Detection
                String key = item.getProduct().getId() + "-" + (item.getProductVariant() != null ? item.getProductVariant().getId() : "null");
                if (uniqueItems.containsKey(key)) {
                    CartItem existing = uniqueItems.get(key);
                    existing.setQuantity(existing.getQuantity() + item.getQuantity());
                    if (existing.getUnitPrice() != null) {
                        existing.setTotalPrice(existing.getUnitPrice().multiply(java.math.BigDecimal.valueOf(existing.getQuantity())));
                    }
                    toRemove.add(item);
                    changed = true;
                } else {
                    uniqueItems.put(key, item);
                }
            }

            if (changed) {
                for (CartItem item : toRemove) {
                    cart.removeCartItem(item);
                }
                cart = this.cartRepository.save(cart);
            }
        }
        return cart;
    }

    @Retryable(
        value = { CannotAcquireLockException.class, ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 500)
    )
    public Cart addToCart(ReqAddToCartDTO req) throws IdInvalidException {
        Cart cart = this.getOrCreateCart();
        if (cart == null) throw new IdInvalidException("User cart not found");

        Product product = this.productService.handleGetById(req.getProductId());
        if (product == null) {
            throw new IdInvalidException("Product not found with id: " + req.getProductId());
        }

        ProductVariant variant = null;
        if (req.getVariantId() != null) {
            variant = this.productVariantRepository.findById(req.getVariantId())
                    .orElseThrow(() -> new IdInvalidException("Variant not found with id: " + req.getVariantId()));
        }

        final ProductVariant finalVariant = variant;

        // Check if item already exists in cart with SAME product AND SAME variant
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(req.getProductId()) &&
                               (finalVariant == null ? item.getProductVariant() == null : 
                                finalVariant.getId().equals(item.getProductVariant() != null ? item.getProductVariant().getId() : null)))
                .findFirst()
                .orElse(null);

        BigDecimal price = variant != null && variant.getPrice() != null ? variant.getPrice() : product.getOriginalPrice();
        
        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(req.getQuantity());
            cartItem.setUnitPrice(price);
            cartItem.setTotalPrice(price.multiply(BigDecimal.valueOf(req.getQuantity())));
            cartItem.setProductVariant(variant);
            cart.addCartItem(cartItem);
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + req.getQuantity());
            cartItem.setUnitPrice(price);
            cartItem.setTotalPrice(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        return this.cartRepository.save(cart);
    }

    public Cart updateQuantity(ReqUpdateQuantityDTO req) throws IdInvalidException {
        CartItem item = this.cartItemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IdInvalidException("Cart item not found"));

        Cart cart = item.getCart();
        if (req.getQuantity() <= 0) {
            cart.removeCartItem(item);
        } else {
            BigDecimal price = item.getUnitPrice();
            item.setQuantity(req.getQuantity());
            item.setTotalPrice(price.multiply(BigDecimal.valueOf(req.getQuantity())));
        }

        return this.cartRepository.save(cart);
    }

    public void removeItem(Long itemId) throws IdInvalidException {
        CartItem item = this.cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IdInvalidException("Cart item not found"));
        Cart cart = item.getCart();
        cart.removeCartItem(item);
        this.cartRepository.save(cart);
    }

    public ResGetCart convertToResGetCart(Cart cart) {
        ResGetCart res = new ResGetCart();
        res.setId(cart.getId());

        if (cart.getUser() != null) {
            ResGetCart.UserInner resUser = new ResGetCart.UserInner();
            resUser.setId(cart.getUser().getId());
            if (cart.getUser().getUserProfile() != null) {
                resUser.setName(cart.getUser().getUserProfile().getName());
            }
            res.setUser(resUser);
        }

        List<ResGetCart.CartItemInner> list = (cart.getItems() == null ? Collections.<CartItem>emptyList() : cart.getItems())
                .stream().map(x -> new ResGetCart.CartItemInner(
                        x.getId(),
                        new ResGetCart.CartItemInner.ProductIner(x.getProduct().getId(), x.getProduct().getName(), 
                            (x.getProduct().getImages() != null && !x.getProduct().getImages().isEmpty()) ? x.getProduct().getImages().get(0).getImageUrl() : "",
                            x.getProduct().getBrand() != null ? x.getProduct().getBrand().getName() : "No Brand",
                            x.getProductVariant() != null && x.getProductVariant().getPrice() != null ? x.getProductVariant().getPrice() : x.getProduct().getOriginalPrice(),
                            x.getProductVariant() != null ? (x.getProductVariant().getInventory() != null ? x.getProductVariant().getInventory().getStock() : 0) : (x.getProduct().getVariants() != null ? x.getProduct().getVariants().stream().map(v -> v.getInventory() != null ? v.getInventory().getStock() : 0).mapToInt(Integer::intValue).sum() : 0)),
                        x.getUnitPrice(),
                        x.getQuantity(),
                        x.getTotalPrice(),
                        x.getProductVariant() != null ? x.getProductVariant().getId() : null,
                        x.getProductVariant() != null ? x.getProductVariant().getAttributeValues().stream()
                                .map(av -> new ResGetCart.CartItemInner.VariantAttributeInner(av.getAttribute().getName(), av.getAttributeValue()))
                                .collect(Collectors.toList()) : null))
                .collect(Collectors.toList());
        res.setItem(list);
        return res;
    }

    public ResAddToCart convertToResAddToCart(Cart cart) {
        ResAddToCart res = new ResAddToCart();
        res.setId(cart.getId());

        if (cart.getUser() != null) {
            ResAddToCart.UserInner resUser = new ResAddToCart.UserInner();
            resUser.setId(cart.getUser().getId());
            if (cart.getUser().getUserProfile() != null) {
                resUser.setName(cart.getUser().getUserProfile().getName());
            }
            res.setUser(resUser);
        }

        List<ResAddToCart.CartItemInner> list = (cart.getItems() == null ? Collections.<CartItem>emptyList() : cart.getItems())
                .stream().map(x -> new ResAddToCart.CartItemInner(
                        x.getId(),
                        new ResAddToCart.CartItemInner.ProductIner(x.getProduct().getId(), x.getProduct().getName(),
                            (x.getProduct().getImages() != null && !x.getProduct().getImages().isEmpty()) ? x.getProduct().getImages().get(0).getImageUrl() : "",
                            x.getProduct().getBrand() != null ? x.getProduct().getBrand().getName() : "No Brand",
                            x.getProductVariant() != null && x.getProductVariant().getPrice() != null ? x.getProductVariant().getPrice() : x.getProduct().getOriginalPrice(),
                            x.getProductVariant() != null ? (x.getProductVariant().getInventory() != null ? x.getProductVariant().getInventory().getStock() : 0) : (x.getProduct().getVariants() != null ? x.getProduct().getVariants().stream().map(v -> v.getInventory() != null ? v.getInventory().getStock() : 0).mapToInt(Integer::intValue).sum() : 0)),
                        x.getUnitPrice(),
                        x.getQuantity(),
                        x.getTotalPrice(),
                        x.getProductVariant() != null ? x.getProductVariant().getId() : null,
                        x.getProductVariant() != null ? x.getProductVariant().getAttributeValues().stream()
                                .map(av -> new ResAddToCart.CartItemInner.VariantAttributeInner(av.getAttribute().getName(), av.getAttributeValue()))
                                .collect(Collectors.toList()) : null))
                .collect(Collectors.toList());
        res.setItem(list);
        return res;
    }

    public double calculateTotalWeight(List<CartItem> items) {
        if (items == null) return 0.0;
        double totalWeight = 0.0;
        for (CartItem item : items) {
            ProductVariant variant = item.getProductVariant();
            if (variant != null && variant.getWeight() > 0) {
                totalWeight += variant.getWeight() * item.getQuantity();
            } else if (item.getProduct() != null) {
                // Fallback to product weight or a default 0.5kg
                totalWeight += 0.5 * item.getQuantity();
            }
        }
        return totalWeight;
    }
}
