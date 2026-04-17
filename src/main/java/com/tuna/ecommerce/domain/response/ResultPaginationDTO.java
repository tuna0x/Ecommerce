package com.tuna.ecommerce.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResultPaginationDTO implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Meta meta;
    private Object result;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Meta implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private int page;
        private int pageSize;
        private long total;
        private int pages;
    }
}
