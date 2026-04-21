package com.tuna.ecommerce.domain.request.user;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqUpdateUserDTO {
    private long id;
    private String name;
    private Integer age;
    private String gender;
    private String image;
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;
}
