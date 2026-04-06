package com.tuna.ecommerce.domain.response.attributevalue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResAttributeValueDTO {
    private Long id;
    @JsonProperty("value")
    private String attributeValue;
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
