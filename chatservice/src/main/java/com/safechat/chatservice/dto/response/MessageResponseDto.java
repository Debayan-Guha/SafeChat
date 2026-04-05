package com.safechat.chatservice.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageResponseDto {

    private String id;
    private String conversationId;
    private String senderId;
    private LocalDateTime sendAt;
    private String encryptedMessage;

    private Map<String,LocalDateTime> readBy;
    private Boolean isDelivered;
    private Boolean isEdited;

    private LocalDateTime expireAt;
}
