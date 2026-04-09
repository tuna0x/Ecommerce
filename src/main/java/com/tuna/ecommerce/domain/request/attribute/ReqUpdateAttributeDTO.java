package com.tuna.ecommerce.domain.request.attribute;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqUpdateAttributeDTO {
    private Long id;
    private String name;
    private Boolean active = true;
    private List<Long> categoryIds;
}