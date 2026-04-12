package com.tuna.ecommerce.domain.response.category;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResCategoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private String slug;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private ParentCategory parentCategory;
    private long productCount;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParentCategory {
        private Long id;
        private String name;
    }
}
