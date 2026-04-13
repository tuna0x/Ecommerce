package com.tuna.ecommerce.domain.request.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateAddressDTO {
    private String receiverName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String detail;
    private Boolean isDefault;

}
