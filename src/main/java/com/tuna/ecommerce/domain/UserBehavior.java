package com.tuna.ecommerce.domain;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.constant.ActionTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_behaviors", indexes = {
        @Index(name = "idx_behavior_email", columnList = "userEmail"),
        @Index(name = "idx_behavior_session", columnList = "sessionId"),
        @Index(name = "idx_behavior_action", columnList = "actionType"),
        @Index(name = "idx_behavior_created", columnList = "createdAt")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email")
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnore
    private User user;

    // === TRƯỜNG MỚI ===

    @Column(length = 36)
    private String sessionId; // UUID per browser session

    @Column(length = 20)
    private String deviceType; // MOBILE | DESKTOP | TABLET

    @Column(length = 500)
    private String referrer; // document.referrer

    @Column(length = 500)
    private String pageUrl; // location.pathname

    // === TRƯỜNG CŨ ===

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionTypeEnum actionType;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON chuẩn hóa

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
