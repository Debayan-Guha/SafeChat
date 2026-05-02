package com.safechat.userservice.service.userService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.entity.PendingUserDeletionEntity;
import com.safechat.userservice.entity.UserEntity;
import com.safechat.userservice.service.EmailService;
import com.safechat.userservice.service.chatService.ChatService;
import com.safechat.userservice.service.dbService.PendingUserDeletionDbService;
import com.safechat.userservice.service.dbService.UserDbService;
import com.safechat.userservice.utility.OperationExecutor;
import com.safechat.userservice.utility.Enumeration.UserDeletionStatus;

@Service
public class UserScheduledDeletionService {

    private final String SERVICE_NAME = "UserScheduledDeletionService";
    private static final Logger log = LoggerFactory.getLogger(UserScheduledDeletionService.class);

    private final AtomicBoolean isRunning_DeleteExpiredAccounts = new AtomicBoolean(false);
    private final AtomicBoolean isRunning_RetryFailedUserDeletions = new AtomicBoolean(false);
    private final UserScheduledDeletionService self;

    private final UserDbService userDbService;
    private final EmailService emailService;
    private final PendingUserDeletionDbService pendingUserDeletionDbService;
    private final ChatService chatService;

    public UserScheduledDeletionService(UserDbService userDbService, EmailService emailService,
            PendingUserDeletionDbService pendingUserDeletionDbService, ChatService chatService,
            @Lazy UserScheduledDeletionService self) {
        this.userDbService = userDbService;
        this.emailService = emailService;
        this.pendingUserDeletionDbService = pendingUserDeletionDbService;
        this.chatService = chatService;
        this.self = self;
    }

    public void deleteExpiredAccounts() {

        final String METHOD_NAME = "deleteExpiredAccounts";
        int BATCH_SIZE = 1000;

        if (!isRunning_DeleteExpiredAccounts.compareAndSet(false, true)) {
            log.debug("{} - Already running, skipping execution", METHOD_NAME);
            return;
        }

        log.info("{} - Started", METHOD_NAME);

        try {
            int page = 0;

            while (true) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);

                Specification<UserEntity> spec = (root, query, cb) -> {
                    Predicate isScheduled = cb.isTrue(root.get("isDeletionScheduled"));
                    Predicate timePassed = cb.lessThanOrEqualTo(
                            root.get("deletionScheduledFor"),
                            LocalDateTime.now());
                    return cb.and(isScheduled, timePassed);
                };

                log.debug("{} - Querying DB for expired accounts, page: {}", METHOD_NAME, page);

                List<UserEntity> usersToDelete = OperationExecutor
                        .dbGet(() -> userDbService.getUsers(spec, pageable).getContent(), SERVICE_NAME, METHOD_NAME);

                if (usersToDelete.isEmpty()) {
                    log.debug("{} - No more expired accounts found, stopping at page: {}", METHOD_NAME, page);
                    break;
                }

                log.debug("{} - Found {} expired accounts on page: {}", METHOD_NAME, usersToDelete.size(), page);

                Set<String> userIds = usersToDelete.stream()
                        .map(UserEntity::getId)
                        .collect(Collectors.toSet());

                // Save to pending table
                List<PendingUserDeletionEntity> entities = userIds.stream()
                        .map(userId -> PendingUserDeletionEntity.builder()
                                .userId(userId)
                                .status(UserDeletionStatus.PENDING)
                                .retryCount(0)
                                .build())
                        .collect(Collectors.toList());

                log.debug("{} - Saving {} records to pending deletion table", METHOD_NAME, entities.size());
                OperationExecutor.dbSave(() -> pendingUserDeletionDbService.saveAll(entities), SERVICE_NAME,
                        METHOD_NAME);

                // Delete users from User DB
                log.debug("{} - Deleting batch of {} users from DB", METHOD_NAME, usersToDelete.size());
                self.deleteBatch(usersToDelete, METHOD_NAME);

                // send confirm delete email
                usersToDelete.forEach(user -> {
                    emailService.sendAfterDeletionEmail(user.getEmail());
                });
                log.debug("{} - Deletion confirmation emails sent for {} users", METHOD_NAME, usersToDelete.size());

                chatService.userDeletionBatch(userIds);
                log.debug("{} - Chat service notified for batch of {} userIds", METHOD_NAME, userIds.size());

                page++;
            }

            log.info("{} - Completed successfully, processed {} page(s)", METHOD_NAME, page);

        } finally {
            isRunning_DeleteExpiredAccounts.set(false);
            log.debug("{} - Lock released", METHOD_NAME);
        }
    }

    @Transactional
    public void deleteBatch(List<UserEntity> usersToDelete, String METHOD_NAME) {
        log.debug("{} - Deleting batch of {} users", METHOD_NAME, usersToDelete.size());
        OperationExecutor.dbRemove(() -> userDbService.deleteAll(usersToDelete), SERVICE_NAME, METHOD_NAME);
        log.debug("{} - Batch delete completed", METHOD_NAME);
    }

    public void retryFailedUserDeletions() {

        final String METHOD_NAME = "retryFailedUserDeletions";
        int BATCH_SIZE = 1000;

        if (!isRunning_RetryFailedUserDeletions.compareAndSet(false, true)) {
            log.debug("{} - Already running, skipping execution", METHOD_NAME);
            return;
        }

        log.info("{} - Started", METHOD_NAME);

        try {
            Specification<PendingUserDeletionEntity> spec = (root, query, cb) -> cb.or(
                    cb.and(
                            cb.equal(root.get("status"), UserDeletionStatus.PENDING),
                            cb.lessThan(root.get("createdAt"), LocalDateTime.now().minusMinutes(10))),
                    cb.equal(root.get("status"), UserDeletionStatus.API_CALL_FAILED),
                    cb.equal(root.get("status"), UserDeletionStatus.CHAT_FAILED));

            int page = 0;

            while (true) {

                // Add sorting to process oldest records first
                Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
                Pageable pageable = PageRequest.of(page, BATCH_SIZE, sort);

                log.debug("{} - Querying pending deletions, page: {}", METHOD_NAME, page);

                List<PendingUserDeletionEntity> stuckRecords = OperationExecutor
                        .dbGet(() -> pendingUserDeletionDbService
                                .getPendingDeletions(spec, pageable)
                                .getContent(), SERVICE_NAME, METHOD_NAME);

                if (stuckRecords.isEmpty()) {
                    log.debug("{} - No more stuck records found, stopping at page: {}", METHOD_NAME, page);
                    break;
                }

                log.debug("{} - Found {} stuck records on page: {}", METHOD_NAME, stuckRecords.size(), page);

                Set<String> userIds = stuckRecords.stream()
                        .map(PendingUserDeletionEntity::getUserId)
                        .collect(Collectors.toSet());

                // Check which users still exist in the users table
                Specification<UserEntity> existsSpec = (root, query, cb) -> root.get("id").in(userIds);

                log.debug("{} - Checking which users still exist in DB for {} userIds", METHOD_NAME, userIds.size());

                List<UserEntity> existingUsers = OperationExecutor.dbGet(
                        () -> userDbService.getUsers(existsSpec, Pageable.unpaged()).getContent(), SERVICE_NAME,
                        METHOD_NAME);

                // Delete any that are still present (means step 2 failed previously)
                if (!existingUsers.isEmpty()) {
                    log.debug("{} - Found {} users still in DB, retrying delete", METHOD_NAME, existingUsers.size());
                    self.deleteBatch(existingUsers, METHOD_NAME);
                } else {
                    log.debug("{} - No leftover users found in DB for this batch", METHOD_NAME);
                }

                // Call Chat Service
                log.debug("{} - Notifying chat service for {} userIds", METHOD_NAME, userIds.size());
                chatService.userDeletionBatch(userIds);

                page++;
            }

            log.info("{} - Completed successfully, processed {} page(s)", METHOD_NAME, page);

        } finally {
            isRunning_RetryFailedUserDeletions.set(false);
            log.debug("{} - Lock released", METHOD_NAME);
        }
    }
}