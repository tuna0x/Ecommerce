package com.tuna.ecommerce.domain.response.attribute;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResAttributeDTO {
    private Long id;
    private String name;
    private CategoryInner category;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInner {
    private Long id;
    private String name;
    }
}
