package com.tuna.ecommerce.domain.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageMessage implements Serializable {
    private Long productId;
    private List<String> tempFilePaths;
}
