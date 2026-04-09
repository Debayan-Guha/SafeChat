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
public class AdminResponseDto {

    private String id;
    private String name;
    private String email;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}