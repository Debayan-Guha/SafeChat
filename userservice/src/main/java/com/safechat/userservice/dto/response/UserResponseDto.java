package com.safechat.userservice.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {

    private String id;
    private String userName;
    private String displayName;
    private String email;
    private String publicKey;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isDeletionScheduled;
    private LocalDateTime deletionScheduledRequestAt;
    private LocalDateTime deletionScheduledFor;
}
