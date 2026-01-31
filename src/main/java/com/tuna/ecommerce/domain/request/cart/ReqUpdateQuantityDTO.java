package com.tuna.ecommerce.domain.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqUpdateQuantityDTO {
    @NotNull(message = "Cart Item id is required")
    private Long itemId;
    private int quantity=1;
}
