package com.tuna.ecommerce.domain.request.attribute;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateAttributeDTO {
    private String name;
    private Long categoryId;
}
