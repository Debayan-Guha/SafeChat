package com.safechat.chatservice.document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "conversations")
@Getter
@Setter
@Builder
public class ConversationDocument {

    @Id
    private String id;

    private String creatorId;
    private Set<String> participants=new HashSet<>(); 
    private LocalDateTime createdAt;

    private LocalDateTime lastMessageAt;  
    private String lastMessageId;  

    private Set<String> deletedForUsers=new HashSet<>();

}
