package com.tuna.ecommerce.domain.response.product;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResProductImportDTO {
    private int productRows;
    private int variantRows;
    private int validProducts;
    private int validVariants;
    private int importedProducts;
    private boolean valid;
    private List<RowIssue> errors = new ArrayList<>();

    @Getter
    @Setter
    public static class RowIssue {
        private String sheet;
        private int row;
        private String productCode;
        private String message;

        public RowIssue(String sheet, int row, String productCode, String message) {
            this.sheet = sheet;
            this.row = row;
            this.productCode = productCode;
            this.message = message;
        }
    }
}
