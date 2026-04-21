package com.tuna.ecommerce.domain.request.auth;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;
}
