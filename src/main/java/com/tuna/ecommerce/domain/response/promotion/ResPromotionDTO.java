package com.tuna.ecommerce.domain.response.promotion;

import com.tuna.ecommerce.ultil.constant.PromotionTypeEnum;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ResPromotionDTO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private PromotionTypeEnum type;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountValue;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean active;
    private Boolean global;
}
