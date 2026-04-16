package com.safechat.userservice.service.userService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
            return;
        }

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

                List<UserEntity> usersToDelete = OperationExecutor
                        .dbGet(() -> userDbService.getUsers(spec, pageable).getContent(), SERVICE_NAME, METHOD_NAME);

                if (usersToDelete.isEmpty()) {
                    break;
                }

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
                OperationExecutor.dbSave(() -> pendingUserDeletionDbService.saveAll(entities), SERVICE_NAME,
                        METHOD_NAME);

                // Delete users from User DB
                self.deleteBatch(usersToDelete, METHOD_NAME);

                // send confirm delete email
                usersToDelete.parallelStream().forEach(user -> {
                    emailService.sendAfterDeletionEmail(user.getEmail());
                });

                chatService.userDeletionBatch(userIds);

                page++;
            }
        } finally {
            isRunning_DeleteExpiredAccounts.set(false);
        }
    }

    @Transactional
    public void deleteBatch(List<UserEntity> usersToDelete, String METHOD_NAME) {
        OperationExecutor.dbRemove(() -> userDbService.deleteAll(usersToDelete), SERVICE_NAME, METHOD_NAME);
    }

    public void retryFailedUserDeletions() {

        final String METHOD_NAME = "retryFailedUserDeletions";
        int BATCH_SIZE = 1000;

        if (!isRunning_RetryFailedUserDeletions.compareAndSet(false, true)) {
            return;
        }

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

                List<PendingUserDeletionEntity> stuckRecords = OperationExecutor
                        .dbGet(() -> pendingUserDeletionDbService
                                .getPendingDeletions(spec, pageable)
                                .getContent(), SERVICE_NAME, METHOD_NAME);

                if (stuckRecords.isEmpty()) {
                    break;
                }

                Set<String> userIds = stuckRecords.stream()
                        .map(PendingUserDeletionEntity::getUserId)
                        .collect(Collectors.toSet());

                // Check which users still exist in the users table
                Specification<UserEntity> existsSpec = (root, query, cb) -> root.get("id").in(userIds);
                List<UserEntity> existingUsers = OperationExecutor.dbGet(
                        () -> userDbService.getUsers(existsSpec, Pageable.unpaged()).getContent(), SERVICE_NAME,
                        METHOD_NAME);

                // Delete any that are still present (means step 2 failed previously)
                if (!existingUsers.isEmpty()) {
                    self.deleteBatch(existingUsers, METHOD_NAME);
                }

                // Call Chat Service
                chatService.userDeletionBatch(userIds);

                page++;
            }
        } finally {
            isRunning_RetryFailedUserDeletions.set(false);
        }
    }
}
