package com.safechat.chatservice.mapper.toDto;

import java.util.Collections;

import com.safechat.chatservice.document.MessageDocument;
import com.safechat.chatservice.dto.response.MessageResponseDto;

public class MessageToDto {
    
    public static MessageResponseDto convert(MessageDocument document){
        return MessageResponseDto.builder()
                .id(document.getId())
                .conversationId(document.getConversationId())
                .senderId(document.getSenderId())
                .sendAt(document.getSendAt())
                .encryptedMessage(document.getEncryptedMessage())
                .readBy(document.getReadBy()!=null && !document.getReadBy().isEmpty()? document.getReadBy():Collections.emptyMap())
                .isDelivered(document.getIsDelivered())
                .isEdited(document.getIsEdited())
                .build();
    }
}
