package com.safechat.chatservice.mapper.toDocument;

import java.time.LocalDateTime;
import java.util.Set;

import com.safechat.chatservice.document.ConversationDocument;
import com.safechat.chatservice.dto.request.create.ConversationMesssageCreateRequestDto.ConversationCreate;

public class ConversationToDocument {
    
    public static ConversationDocument convert(String userId,ConversationCreate dto){
        return ConversationDocument.builder()
                                .participants(Set.copyOf(dto.getParticipantsId()))
                                .creatorId(userId)
                                .createdAt(LocalDateTime.now())
                                .build();
    }
}
