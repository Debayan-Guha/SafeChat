package com.safechat.chatservice.document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "messages")
@Getter
@Setter
@Builder
public class MessageDocument {

    @Id
    private String id;

    private String conversationId;
    private String senderId;
    private LocalDateTime sendAt;
    private LocalDateTime receivedAt;
    private String encryptedMessage;

    private Boolean isRead;
    private Boolean isDelivered;
    private Boolean isEdited;

    private Set<String> deletedForUsers=new HashSet<>();

    @Indexed(expireAfterSeconds = 0) // delete exactly at expireAt
    private LocalDateTime expireAt;
}
