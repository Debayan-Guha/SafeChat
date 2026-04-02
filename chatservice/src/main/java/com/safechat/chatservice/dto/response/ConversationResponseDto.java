package com.safechat.chatservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ConversationResponseDto {

    private String id;
    private String creatorId;
    private List<String> participants;
    private LocalDateTime createdAt;
}
