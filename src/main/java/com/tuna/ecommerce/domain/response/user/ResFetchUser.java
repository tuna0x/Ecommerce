package com.tuna.ecommerce.domain.response.user;

import java.time.Instant;

import com.tuna.ecommerce.ultil.constant.GenderEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResFetchUser {
    private Long id;
    private String name;
    private String email;
    private int age;
    private GenderEnum gender;
    private String image;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updateBy;
    private Boolean active;
    private Boolean verified;
    private RoleUser role;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class RoleUser {
        private Long id;
        private String name;
    }
}
