package com.tuna.ecommerce.domain.request.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqRegisterDTO {
    private String name;
    private String email;
    private String password;
}
