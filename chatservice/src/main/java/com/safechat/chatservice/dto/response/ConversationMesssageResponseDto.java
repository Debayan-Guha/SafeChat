package com.safechat.chatservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ConversationMesssageResponseDto {
    
    private ConversationResponseDto conversationResponseDto;

    private MessageResponseDto messageResponseDto;

    private long unreadCount;
}
