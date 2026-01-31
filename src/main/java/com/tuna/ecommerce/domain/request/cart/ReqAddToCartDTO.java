package com.tuna.ecommerce.domain.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqAddToCartDTO {
    @NotNull(message = "Product id is required")
    private Long productId;
    @Min(value = 1,message = "Quantity must be at least 1")
    private int quantity=1;
}
