package com.tuna.ecommerce.domain.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqSocialLoginDTO {
    @NotBlank(message = "Id Token không được để trống")
    private String idToken;
}
