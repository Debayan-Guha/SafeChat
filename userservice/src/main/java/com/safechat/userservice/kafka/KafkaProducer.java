package com.safechat.userservice.kafka;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.safechat.userservice.dto.kafkaEvent.ReceiveEventUserDeletionStatus;
import com.safechat.userservice.service.kafkaService.KafkaService;
import com.safechat.userservice.utility.Enumeration.UserDeletionStatus;

@Service
public class KafkaProducer {

    private final KafkaTemplate<List<String>, Object> kafkaTemplate;
    private final KafkaService kafkaService;

    public KafkaProducer(KafkaTemplate<List<String>, Object> kafkaTemplate,@Lazy KafkaService kafkaService) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaService = kafkaService;
    }

    public void sendUserDeletionEventBatch(List<String> userIds) {
        try {
            kafkaTemplate.send("user.deleted", userIds);

            List<ReceiveEventUserDeletionStatus> statusList = userIds.stream()
                    .map(id -> new ReceiveEventUserDeletionStatus(id, UserDeletionStatus.KAFKA_SENT))
                    .toList();

            kafkaService.updateStatusBatch(statusList);
        } catch (Exception ex) {
            List<ReceiveEventUserDeletionStatus> failedStatusList = userIds.stream()
                    .map(id -> new ReceiveEventUserDeletionStatus(id, UserDeletionStatus.KAFKA_SENT_FAILED))
                    .toList();

            kafkaService.updateStatusBatch(failedStatusList);
        }

    }

}