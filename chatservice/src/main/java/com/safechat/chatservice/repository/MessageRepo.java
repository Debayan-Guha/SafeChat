package com.safechat.chatservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.safechat.chatservice.document.MessageDocument;

@Repository
public interface MessageRepo extends MongoRepository<MessageDocument,String>{
    
}
