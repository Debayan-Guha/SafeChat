package com.safechat.userservice.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pending_user_deletions")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PendingUserDeletionEntity {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "status")
    private String status;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "kafka_sent_at")
    private LocalDateTime kafkaSentAt;

    @Column(name = "chat_processed_at")
    private LocalDateTime chatProcessedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}