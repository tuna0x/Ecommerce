package com.tuna.ecommerce.domain.request.attributeValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateAttributesValueDTO {
    private String attributeValue;
    private Long attributeId;
    
}
