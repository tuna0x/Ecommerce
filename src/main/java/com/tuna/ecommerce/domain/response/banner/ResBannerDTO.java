package com.tuna.ecommerce.domain.response.banner;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class ResBannerDTO implements Serializable {
    private Long id;
    private String title;
    private String subtitle;
    private String image;
    private String link;
    private String description;
    private String position;
    private Integer order;
    private Boolean isActive;
    private LocalDate startDate;
    private LocalDate endDate;
}
