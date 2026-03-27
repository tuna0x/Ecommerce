package com.tuna.ecommerce.domain.request.banner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqUpdateBannerDTO extends ReqCreateBannerDTO {
    private Long id;
}
