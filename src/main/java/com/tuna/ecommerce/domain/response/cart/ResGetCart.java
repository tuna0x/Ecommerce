package com.tuna.ecommerce.domain.response.cart;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResGetCart {
    private long id;
    private UserInner user;
    private List<CartItemInner> item;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class UserInner {
        private long id;
        private String name;
    }
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class  CartItemInner {
    private long id;
    private ProductIner product;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal totalPrice;

        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        @Setter
        public static class ProductIner {
            private long id;
            private String name;
            private String image;
        }
    }

}
