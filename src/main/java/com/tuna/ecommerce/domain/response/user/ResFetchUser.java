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
    private String address;
    private int age;
    private GenderEnum gender;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updateBy;
}
