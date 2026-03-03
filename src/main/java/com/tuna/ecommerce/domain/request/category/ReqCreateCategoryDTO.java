package com.tuna.ecommerce.domain.request.category;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateCategoryDTO {
    private String name;
    private String description;
    private boolean active;
    private Long parentId;

}
