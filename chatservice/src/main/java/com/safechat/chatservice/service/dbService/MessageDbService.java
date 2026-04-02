package com.safechat.chatservice.service.dbService;

import com.safechat.chatservice.document.MessageDocument;
import com.safechat.chatservice.repository.MessageRepo;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageDbService {

    private final MessageRepo messageRepo;
    private final MongoTemplate mongoTemplate;

    public MessageDbService(MessageRepo messageRepo, MongoTemplate mongoTemplate) {
        this.messageRepo = messageRepo;
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<MessageDocument> getMessage(Query query) {
        return Optional.ofNullable(mongoTemplate.findOne(query, MessageDocument.class));
    }

    public List<MessageDocument> getMessages(Query query) {
        return mongoTemplate.find(query, MessageDocument.class);
    }

    public long count(Query query) {
        return mongoTemplate.count(query, MessageDocument.class);
    }

    public boolean exists(Query query) {
        return mongoTemplate.exists(query, MessageDocument.class);
    }

    public MessageDocument save(MessageDocument message) {
        return messageRepo.save(message);
    }

    public List<MessageDocument> saveAll(List<MessageDocument> messages) {
        return messageRepo.saveAll(messages);
    }

    public void delete(Query query) {
        mongoTemplate.remove(query, MessageDocument.class);
    }
}
