package com.safechat.chatservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.safechat.chatservice.document.ConversationDocument;

@Repository
public interface ConversationRepo extends MongoRepository<ConversationDocument,String>{
    
}
