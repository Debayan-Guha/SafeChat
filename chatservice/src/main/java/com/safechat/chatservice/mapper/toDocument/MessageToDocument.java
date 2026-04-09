package com.safechat.chatservice.mapper.toDocument;

import java.time.LocalDateTime;

import com.safechat.chatservice.document.MessageDocument;
import com.safechat.chatservice.dto.request.create.MessageCreateRequestDto;

public class MessageToDocument {

    public static MessageDocument convert(String userId,MessageCreateRequestDto dto) {
        return MessageDocument.builder()
                .conversationId(dto.getConversationId())
                .senderId(userId)
                .encryptedMessage(dto.getEncryptedMessage())
                .sendAt(LocalDateTime.now())                
                .isDelivered(false)
                .isEdited(false)
                .expireAt(((dto.getExpirySeconds() != null && dto.getExpirySeconds() > 0))
                        ? LocalDateTime.now().plusSeconds(dto.getExpirySeconds())
                        : null)
                .build();

    }
}
