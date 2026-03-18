package com.tuna.ecommerce.domain.response.attributevalue;

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
}
