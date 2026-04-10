package com.safechat.userservice.kafka;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.safechat.userservice.dto.kafkaEvent.ReceiveEventUserDeletionStatus;
import com.safechat.userservice.service.kafkaService.KafkaService;

@Service
public class KafkaConsumer {

    private final KafkaService kafkaService;

    public KafkaConsumer(KafkaService kafkaService) {
        this.kafkaService = kafkaService;
    }

    @KafkaListener(topics = "user.delete.status", groupId = "user-service-group")
    public void consumeDeletionStatus(List<ReceiveEventUserDeletionStatus> events) {
        // Batch update DB
        kafkaService.updateStatusBatch(events);
    }
}