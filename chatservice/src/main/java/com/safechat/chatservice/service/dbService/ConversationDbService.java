package com.safechat.chatservice.service.dbService;

import com.safechat.chatservice.document.ConversationDocument;
import com.safechat.chatservice.repository.ConversationRepo;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationDbService {

    private final ConversationRepo conversationRepo;
    private final MongoTemplate mongoTemplate;

    public ConversationDbService(ConversationRepo conversationRepo, MongoTemplate mongoTemplate) {
        this.conversationRepo = conversationRepo;
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<ConversationDocument> getConversation(Query query) {
        return Optional.ofNullable(mongoTemplate.findOne(query, ConversationDocument.class));
    }

    public List<ConversationDocument> getConversations(Query query) {
        return mongoTemplate.find(query, ConversationDocument.class);
    }

    public long count(Query query) {
        return mongoTemplate.count(query, ConversationDocument.class);
    }

    public boolean exists(Query query) {
        return mongoTemplate.exists(query, ConversationDocument.class);
    }

    public ConversationDocument save(ConversationDocument conversation) {
        return conversationRepo.save(conversation);
    }

    public List<ConversationDocument> saveAll(List<ConversationDocument> conversations) {
        return conversationRepo.saveAll(conversations);
    }

    public void delete(Query query) {
        mongoTemplate.remove(query, ConversationDocument.class);
    }
}
