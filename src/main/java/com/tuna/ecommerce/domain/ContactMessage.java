package com.tuna.ecommerce.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContactMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Họ tên không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    private String phone;

    @NotBlank(message = "Chủ đề không được để trống")
    private String subject;

    @NotBlank(message = "Nội dung không được để trống")
    @Column(columnDefinition = "MEDIUMTEXT")
    private String message;

    private String status = "NEW"; // NEW, READ, REPLIED

    private Instant createdAt;

    @PrePersist
    public void handlePrePersist() {
        this.createdAt = Instant.now();
    }
}
