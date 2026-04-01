package com.tuna.ecommerce.domain.request.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqUpdateUserDTO {
    private long id;
    private String name;
    private int age;
    private String gender;
    private String image;
}
