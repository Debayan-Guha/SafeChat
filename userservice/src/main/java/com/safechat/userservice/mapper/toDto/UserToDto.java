package com.safechat.userservice.mapper.toDto;

import com.safechat.userservice.dto.response.UserResponseDto;
import com.safechat.userservice.entity.UserEntity;

public class UserToDto {

    public static UserResponseDto convert(UserEntity entity) {

        return UserResponseDto.builder()
                .id(entity.getId())
                .userName(entity.getUserName())
                .displayName(entity.getDisplayName())
                .email(entity.getEmail())
                .publicKey(entity.getPublicKey())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isDeletionScheduled(entity.isDeletionScheduled())
                .deletionScheduledRequestAt(entity.getDeletionScheduledRequestAt())
                .deletionScheduledFor(entity.getDeletionScheduledFor())
                .build();
    }
}
