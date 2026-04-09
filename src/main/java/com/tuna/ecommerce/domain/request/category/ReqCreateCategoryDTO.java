package com.tuna.ecommerce.domain.request.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateCategoryDTO {
    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;
    private String description;
    private Boolean active = true;
    private Long parentId;

}
