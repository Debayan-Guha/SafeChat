package com.safechat.userservice.service.kafkaService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.dto.kafkaEvent.ReceiveEventUserDeletionStatus;
import com.safechat.userservice.entity.PendingUserDeletionEntity;
import com.safechat.userservice.kafka.KafkaProducer;
import com.safechat.userservice.service.dbService.PendingUserDeletionDbService;
import com.safechat.userservice.utility.Enumeration.UserDeletionStatus;


@Service
public class KafkaService {

    private static final int BATCH_SIZE = 1000;

    private final PendingUserDeletionDbService pendingUserDeletionDbService;
    private final KafkaProducer kafkaProducer;

    public KafkaService(PendingUserDeletionDbService pendingUserDeletionDbService,
           @Lazy KafkaProducer kafkaProducer) {
        this.pendingUserDeletionDbService = pendingUserDeletionDbService;
        this.kafkaProducer = kafkaProducer;
    }

    public void updateStatusBatch(List<ReceiveEventUserDeletionStatus> dtoList) {

        // Get all userIds
        List<String> userIds = dtoList.stream()
                .map(ReceiveEventUserDeletionStatus::getUserId)
                .collect(Collectors.toList());

        // Fetch existing records
        Specification<PendingUserDeletionEntity> spec = (root, query, cb) -> root.get("userId").in(userIds);

        List<PendingUserDeletionEntity> existingEntities = pendingUserDeletionDbService
                .getPendingDeletions(spec, Pageable.unpaged())
                .getContent();

        Map<String, PendingUserDeletionEntity> existingMap = existingEntities.stream()
                .collect(Collectors.toMap(PendingUserDeletionEntity::getUserId, e -> e));

        List<PendingUserDeletionEntity> entitiesToSave = new ArrayList<>();
        List<String> successIds = new ArrayList<>();

        for (ReceiveEventUserDeletionStatus dto : dtoList) {
            String userId = dto.getUserId();
            String status = dto.getStatus();

            PendingUserDeletionEntity entity = existingMap.get(userId);

            if (entity == null) {
                // New record
                entity = PendingUserDeletionEntity.builder()
                        .userId(userId)
                        .status(status)
                        .retryCount(0)
                        .build();

                if (UserDeletionStatus.KAFKA_SENT.equals(status)
                        || UserDeletionStatus.KAFKA_SENT_FAILED.equals(status)) {
                    entity.setKafkaSentAt(LocalDateTime.now());
                }
                if (UserDeletionStatus.CHAT_SUCCESS.equals(status) || UserDeletionStatus.CHAT_FAILED.equals(status)) {
                    entity.setChatProcessedAt(LocalDateTime.now());
                }
                entitiesToSave.add(entity);

            } else {
                // Existing record - update based on status
                entity.setStatus(status);

                if (UserDeletionStatus.KAFKA_SENT.equals(status)) {
                    entity.setKafkaSentAt(LocalDateTime.now());

                } else if (UserDeletionStatus.KAFKA_SENT_FAILED.equals(status)) {
                    entity.setRetryCount(entity.getRetryCount() + 1);

                } else if (UserDeletionStatus.CHAT_SUCCESS.equals(status)) {
                    entity.setChatProcessedAt(LocalDateTime.now());
                    successIds.add(userId);

                } else if (UserDeletionStatus.CHAT_FAILED.equals(status)) {
                    entity.setRetryCount(entity.getRetryCount() + 1);
                }

                entitiesToSave.add(entity);
            }
        }

        // Batch save all updates
        pendingUserDeletionDbService.saveAll(entitiesToSave);

        // Delete successful ones after save
        if (!successIds.isEmpty()) {
            Specification<PendingUserDeletionEntity> deleteSpec = (root, query, cb) -> root.get("userId")
                    .in(successIds);

            List<PendingUserDeletionEntity> entitiesToDelete = pendingUserDeletionDbService
                    .getPendingDeletions(deleteSpec, Pageable.unpaged())
                    .getContent();

            pendingUserDeletionDbService.deleteAll(entitiesToDelete);
        }
    }

    public void retryFailedDeletions() {
        Specification<PendingUserDeletionEntity> spec = (root, query, cb) -> cb.or(
                cb.equal(root.get("status"), UserDeletionStatus.KAFKA_SENT_FAILED),
                cb.equal(root.get("status"), UserDeletionStatus.CHAT_FAILED));

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        List<PendingUserDeletionEntity> failedEntities = pendingUserDeletionDbService
                .getPendingDeletions(spec, pageable).getContent();

        if (failedEntities.isEmpty()) {
            return;
        }

        List<String> userIds = failedEntities.stream()
                .map(PendingUserDeletionEntity::getUserId)
                .collect(Collectors.toList());

        kafkaProducer.sendUserDeletionEventBatch(userIds);
    }

}