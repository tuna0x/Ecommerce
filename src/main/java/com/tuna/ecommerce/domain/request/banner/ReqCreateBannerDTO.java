package com.tuna.ecommerce.domain.request.banner;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
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
    private String subtitle;
    private String description;
    private String link;
    private String position;
    private Integer order;
    private Boolean isActive;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private MultipartFile file;
}
