package com.tuna.ecommerce.domain.response.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResUserPermissionDTO implements Serializable {
    private String name;
    private String apiPath;
    private String method;
    private String module;
}
