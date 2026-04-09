package com.safechat.userservice.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
        // 1. For login by email (isEmailExists, login)
        @Index(name = "idx_email", columnList = "email", unique = true),

        // 2. For login by displayName (isDisplayNameExists, login, searchUsers exact
        // match)
        @Index(name = "idx_display_name", columnList = "display_name", unique = true),

        // 3. CRITICAL: For deletion scheduler (deleteExpiredAccounts query)
        @Index(name = "idx_deletion_cleanup", columnList = "is_deletion_scheduled, deletion_scheduled_for")
})
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "display_name", unique = true, nullable = false)
    private String displayName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "public_key")
    private String publicKey;

    @Column(name = "encrypted_private_key")
    private String encryptedPrivateKey;

    @Column(name = "status", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deletion_scheduled")
    private boolean isDeletionScheduled;

    @Column(name = "deletion_scheduled_request_at")
    private LocalDateTime deletionScheduledRequestAt;

    @Column(name = "deletion_scheduled_for")
    private LocalDateTime deletionScheduledFor;
}