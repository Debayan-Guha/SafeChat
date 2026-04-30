package com.safechat.chatservice.document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private Map<String,String> encryptedMessages;

    private Map<String,LocalDateTime> readBy;
    private Boolean isDelivered;
    private Boolean isEdited;

    private Set<String> deletedForUsers=new HashSet<>();

    @Indexed(expireAfterSeconds = 0) // delete exactly at expireAt
    private LocalDateTime expireAt;
}
