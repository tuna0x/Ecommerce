package com.tuna.ecommerce.domain.request.banner;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReqCreateBannerDTO {
    @NotBlank(message = "title is not blank")
    private String title;
    private String link;
    private String position;
    private Integer order;
    @JsonProperty("isActive")
    private Boolean isActive;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate endDate;
    private org.springframework.web.multipart.MultipartFile file;
}
