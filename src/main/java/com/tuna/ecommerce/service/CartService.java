package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.cart.ReqAddToCartDTO;
import com.tuna.ecommerce.domain.request.cart.ReqUpdateQuantityDTO;
import com.tuna.ecommerce.domain.response.cart.ResAddToCart;
import com.tuna.ecommerce.domain.response.cart.ResGetCart;
import com.tuna.ecommerce.repository.CartItemRepository;
import com.tuna.ecommerce.repository.CartRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;

    public Cart getOrCreateCart(){
        String email=this.securityUtil.getCurrentUserLogin().isPresent() ? this.securityUtil.getCurrentUserLogin().get() : null;
        User user= this.userRepository.findByEmail(email);
        Cart cart=this.cartRepository.findByUser(user);
        if (cart==null) {
            Cart cur= new Cart();
            cur.setUser(user);
            cur.setItems(new ArrayList<>());
            cart=this.cartRepository.save(cur);
        }
        return cart;
    }

    public CartItem createNewCartItem(Cart cart,Product product, BigDecimal price, int quantity){
        CartItem cartItem= new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setUnitPrice(price);
        cartItem.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
        return this.cartItemRepository.save(cartItem);
    }

    public CartItem updateCartItem(Cart cart, BigDecimal price, int quantity){
        CartItem cartItem= new CartItem();
        cartItem.setQuantity(quantity);
        cartItem.setUnitPrice(price);
        cartItem.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
        return cartItem;
    }




    public Cart addToCart(ReqAddToCartDTO req) throws IdInvalidException{
        Cart cart=this.getOrCreateCart();

        // Kiểm tra xem product tồn tại ko
        Product product=this.productService.handleGetById(req.getProductId());
        if (product==null) {
            throw new IdInvalidException("product not found"+req.getProductId());
        }
        // kiểm tra xem cart item có tồn taji không
        CartItem cartItem =this.cartItemRepository.findByCartIdAndProductId(cart.getId(),req.getProductId());
        BigDecimal price=product.getOriginalPrice();
        if (cartItem==null) {
            cartItem = this.createNewCartItem(cart, product, price, req.getQuantity());
        }else{
            cartItem.setQuantity(cartItem.getQuantity()+req.getQuantity());
            cartItem.setTotalPrice(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            this.cartItemRepository.save(cartItem);
        }
        cart.getItems().add(cartItem);

        return cart;
    }

    public Cart updateQuantity(ReqUpdateQuantityDTO req){
        this.getOrCreateCart();
       CartItem item=this.cartItemRepository.findById(req.getItemId()).orElse(null);

       if (req.getQuantity()<=0) {
        this.cartItemRepository.delete(item);
       }else{
        item.setQuantity(req.getQuantity());
        item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        item=this.cartItemRepository.save(item);
       }

       return item.getCart();
    }

    public void removeItem(Long itemId){
       CartItem item = this.cartItemRepository.findById(itemId).orElse(null);
       Cart cart =item.getCart();
       cart.getItems().remove(item);
       this.cartItemRepository.delete(item);
    }

    public ResGetCart convertToResGetCart(Cart cart){
        ResGetCart res = new ResGetCart();
        ResGetCart.UserInner resUser = new ResGetCart.UserInner();
        res.setId(cart.getId());
        resUser.setId(cart.getUser().getId());
        resUser.setName(cart.getUser().getName());
        res.setUser(resUser);

        List<CartItem> items = cart.getItems() == null
        ? Collections.emptyList()
        : cart.getItems();

        List<ResGetCart.CartItemInner> list=items.stream().map(
            x->new ResGetCart.CartItemInner(x.getId(),
            new ResGetCart.CartItemInner.ProductIner(x.getProduct().getId(),x.getProduct().getName(),x.getProduct().getImage()),
            x.getUnitPrice(), x.getQuantity(), x.getTotalPrice()))
            .collect(Collectors.toList());
        res.setItem(list);
        return res;
    }

        public ResAddToCart convertToResAddToCart(Cart cart){
        ResAddToCart res = new ResAddToCart();
        ResAddToCart.UserInner resUser = new ResAddToCart.UserInner();
        res.setId(cart.getId());
        resUser.setId(cart.getUser().getId());
        resUser.setName(cart.getUser().getName());
        res.setUser(resUser);

        List<CartItem> items = cart.getItems() == null
        ? Collections.emptyList()
        : cart.getItems();

        List<ResAddToCart.CartItemInner> list=items.stream().map(
            x->new ResAddToCart.CartItemInner(x.getId(),
            new ResAddToCart.CartItemInner.ProductIner(x.getProduct().getId(),x.getProduct().getName(),x.getProduct().getImage()),
            x.getUnitPrice(), x.getQuantity(), x.getTotalPrice()))
            .collect(Collectors.toList());
        res.setItem(list);
        return res;
    }
}
