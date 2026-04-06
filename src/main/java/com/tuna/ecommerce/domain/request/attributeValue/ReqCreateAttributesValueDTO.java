package com.tuna.ecommerce.domain.request.attributeValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateAttributesValueDTO {
    @JsonProperty("value")
    private String attributeValue;
    private Long attributeId;
    
}
