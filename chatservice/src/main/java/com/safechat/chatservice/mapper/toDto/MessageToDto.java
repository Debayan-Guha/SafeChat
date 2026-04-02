package com.safechat.chatservice.mapper.toDto;

import com.safechat.chatservice.document.MessageDocument;
import com.safechat.chatservice.dto.response.MessageResponseDto;

public class MessageToDto {
    
    public static MessageResponseDto convert(MessageDocument document){
        return MessageResponseDto.builder()
                .id(document.getId())
                .conversationId(document.getConversationId())
                .senderId(document.getSenderId())
                .sendAt(document.getSendAt())
                .receivedAt(document.getReceivedAt())
                .encryptedMessage(document.getEncryptedMessage())
                .isRead(document.getIsRead())
                .isDelivered(document.getIsDelivered())
                .isEdited(document.getIsEdited())
                .build();
    }
}
