package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        }
        return cart;
    }

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
                        new ResGetCart.CartItemInner.ProductIner(x.getProduct().getId(), x.getProduct().getName()),
                        x.getUnitPrice(),
                        x.getQuantity(),
                        x.getTotalPrice(),
                        x.getProductVariant() != null ? x.getProductVariant().getId() : null,
                        x.getProductVariant() != null ? x.getProductVariant().getAttributeValues().stream()
                                .map(av -> new ResGetCart.CartItemInner.VariantAttributeInner(av.getAttribute().getName(), av.getValue()))
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
                        new ResAddToCart.CartItemInner.ProductIner(x.getProduct().getId(), x.getProduct().getName()),
                        x.getUnitPrice(),
                        x.getQuantity(),
                        x.getTotalPrice(),
                        x.getProductVariant() != null ? x.getProductVariant().getId() : null,
                        x.getProductVariant() != null ? x.getProductVariant().getAttributeValues().stream()
                                .map(av -> new ResAddToCart.CartItemInner.VariantAttributeInner(av.getAttribute().getName(), av.getValue()))
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
            if (variant != null) {
                totalWeight += variant.getWeight() * item.getQuantity();
            }
        }
        return totalWeight;
    }
}
