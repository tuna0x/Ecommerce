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
<<<<<<< HEAD
import jakarta.persistence.JoinColumn;
=======
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
<<<<<<< HEAD
=======
import jakarta.validation.constraints.NotBlank;
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
<<<<<<< HEAD
@AllArgsConstructor
@NoArgsConstructor
=======
@NoArgsConstructor
@AllArgsConstructor
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
@Table(name = "brands")
@Entity
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
<<<<<<< HEAD
    private String name;
    private String description;

        private Instant createdAt;
=======
    @NotBlank(message = "name is not blank")
    private String name;
    private String image;
    private Instant createdAt;
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

<<<<<<< HEAD
    @OneToMany(mappedBy = "brand", fetch =FetchType.LAZY )
    @JsonIgnore
    private List<Product> products;

                @PrePersist
    public void handleBeforeCreate(){
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() ==true ?
        SecurityUtil.getCurrentUserLogin().get() : "";
=======
    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Product> products;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
        this.createdAt = Instant.now();

    }

<<<<<<< HEAD
        @PreUpdate
    public void handleBeforeUpdate(){
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() ==true ?
        SecurityUtil.getCurrentUserLogin().get() : "";
        this.updatedAt = Instant.now();

}
=======
    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        this.updatedAt = Instant.now();

    }
>>>>>>> 1a5b218cbf68d4f224d1e4a45849a844cc324fc8
}
