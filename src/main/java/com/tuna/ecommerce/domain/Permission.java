package com.tuna.ecommerce.domain;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tuna.ecommerce.ultil.SecurityUtil;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Name is not blank")
    private String name;
    @NotBlank(message = "API Path is not blank")
    private String apiPath;
    @NotBlank(message = "Method is not blank")
    private String method;
    @NotBlank(message = "Module is not blank")
    private String module;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;


    public Permission(@NotBlank(message = "Name is not blank") String name,
            @NotBlank(message = "API Path is not blank") String apiPath,
            @NotBlank(message = "Method is not blank") String method,
            @NotBlank(message = "Module is not blank") String module) {
        this.name = name;
        this.apiPath = apiPath;
        this.method = method;
        this.module = module;
    }

    @ManyToMany(fetch = FetchType.LAZY,mappedBy = "permissions")
    @JsonIgnore
    private List<Role> roles;

        @PrePersist
    public void handleCreatedAt() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleUpdatedAt() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.updatedAt = Instant.now();
    }

}
