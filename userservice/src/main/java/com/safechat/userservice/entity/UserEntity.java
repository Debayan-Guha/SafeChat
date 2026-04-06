package com.safechat.userservice.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class UserEntity {

    private String id;
    private String userName;
    private String displayName;
    private String email;
    private String password;
    private String publicKey;
    private String encryptedPrivateKey;
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private boolean isDeletionScheduled;
    private LocalDateTime deletionScheduledRequestAt;
    private LocalDateTime deletionScheduledFor;

}
