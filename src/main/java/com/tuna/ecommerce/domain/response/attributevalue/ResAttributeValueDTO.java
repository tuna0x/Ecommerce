package com.tuna.ecommerce.domain.response.attributevalue;

import java.util.List;
import lombok.*;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResAttributeValueDTO {
    private Long id;
    private String value;
    private AttributeInner attribute;


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttributeInner {
    private Long id;
    private String name;
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
}
