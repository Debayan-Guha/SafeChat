package com.safechat.chatservice.mapper.toDto;

import java.util.ArrayList;

import com.safechat.chatservice.document.ConversationDocument;
import com.safechat.chatservice.dto.response.ConversationResponseDto;

public class ConversationToDto {
    
    public static ConversationResponseDto convert(ConversationDocument  document){
        return ConversationResponseDto.builder()
                .id(document.getId())
                .creatorId(document.getCreatorId())
                .participants(new ArrayList<>(document.getParticipants()))
                .createdAt(document.getCreatedAt())
                .build();
    }
}
