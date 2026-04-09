package com.tuna.ecommerce.domain.response.attribute;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResAttributeDTO {
    private Long id;
    private String name;
    private Boolean active;
    private List<CategoryInner> categories;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInner {
    private Long id;
    private String name;
    }
}
