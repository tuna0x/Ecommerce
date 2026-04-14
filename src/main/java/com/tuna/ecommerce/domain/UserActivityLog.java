package com.tuna.ecommerce.domain;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.constant.ActionTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_activity_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lưu chuỗi Email để tracking luôn cả khi khách mất session, hoặc "anonymous" nếu chưa Đăng nhập
    @Column(name = "user_email")
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnore
    private User user;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionTypeEnum actionType;

    @Column(columnDefinition = "TEXT")
    private String metadata; // Format JSON để lưu các dữ liệu linh hoạt (productId, keyword, duration...)

    private String ipAddress;

    private Instant createdAt;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
        if (this.userEmail == null || this.userEmail.isEmpty()) {
            this.userEmail = SecurityUtil.getCurrentUserLogin().orElse("anonymous");
        }
    }
}
