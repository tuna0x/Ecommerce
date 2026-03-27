package com.tuna.ecommerce.domain.request.brand;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateBrandDTO {
    private String name;
    private String description;
    private String image;
    private org.springframework.web.multipart.MultipartFile file;
}
