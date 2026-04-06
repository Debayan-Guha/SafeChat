package com.safechat.userservice.mapper.toDto;

import com.safechat.userservice.dto.response.UserResponseDto;
import com.safechat.userservice.entity.UserEntity;

public class UserToDto {
    
    public static UserResponseDto convert(UserEntity entity) {

        return UserResponseDto.builder()
                .userName(entity.getUserName())
                .displayName(entity.getDisplayName())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .build();
    }
}
