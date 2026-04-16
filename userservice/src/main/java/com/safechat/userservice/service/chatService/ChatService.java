package com.safechat.userservice.service.chatService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.entity.PendingUserDeletionEntity;
import com.safechat.userservice.externalApiCall.chatService.ChatServiceApiCall;
import com.safechat.userservice.externalApiCall.chatService.UserDeletionStatusDto;
import com.safechat.userservice.service.dbService.PendingUserDeletionDbService;
import com.safechat.userservice.service.dbService.UserDbService;
import com.safechat.userservice.service.userService.UserWriteService;
import com.safechat.userservice.utility.Enumeration.UserDeletionStatus;
import com.safechat.userservice.utility.OperationExecutor;
import com.safechat.userservice.utility.api.ApiResponseFormatter;

@Service
public class ChatService {

    private final String SERVICE_NAME = "ChatService";
    private static final int MAX_RETRY_COUNT = 50;

    private final UserDbService userDbService;
    private final PendingUserDeletionDbService pendingUserDeletionDbService;
    private final ChatServiceApiCall chatServiceApiCall;
    private final UserWriteService userWriteService;

    public ChatService(UserDbService userDbService,
            PendingUserDeletionDbService pendingUserDeletionDbService,
            ChatServiceApiCall chatServiceApiCall,
            UserWriteService userWriteService) {
        this.userDbService = userDbService;
        this.pendingUserDeletionDbService = pendingUserDeletionDbService;
        this.chatServiceApiCall = chatServiceApiCall;
        this.userWriteService = userWriteService;
    }

    public void userDeletionBatch(Set<String> userIds) {

        final String METHOD_NAME = "userDeletionBatch";

        try {
            ApiResponseFormatter<List<UserDeletionStatusDto>> response = chatServiceApiCall
                    .sendUserDeletionBatch(new ArrayList<>(userIds));

            if (response != null && response.getStatusCode() == 200 && response.getData() != null) {
                List<String> successUsers = new ArrayList<>();
                List<String> failedUsers = new ArrayList<>();

                for (UserDeletionStatusDto status : response.getData()) {
                    if (UserDeletionStatus.CHAT_SUCCESS.equals(status.getStatus())) {
                        successUsers.add(status.getUserId());
                    } else {
                        failedUsers.add(status.getUserId());
                    }
                }

                // Batch delete successes
                if (!successUsers.isEmpty()) {
                    Specification<PendingUserDeletionEntity> deleteSpec = (root, query, cb) -> root
                            .get("userId").in(successUsers);
                    List<PendingUserDeletionEntity> toDelete = pendingUserDeletionDbService
                            .getPendingDeletions(deleteSpec, Pageable.unpaged())
                            .getContent();
                    OperationExecutor.dbRemove(() -> pendingUserDeletionDbService.deleteAll(toDelete),
                            SERVICE_NAME, METHOD_NAME);
                }

                // Batch update failures with retry limit check
                if (!failedUsers.isEmpty()) {
                    Specification<PendingUserDeletionEntity> updateSpec = (root, query, cb) -> root
                            .get("userId").in(failedUsers);
                    List<PendingUserDeletionEntity> toUpdate = pendingUserDeletionDbService
                            .getPendingDeletions(updateSpec, Pageable.unpaged())
                            .getContent();

                    for (PendingUserDeletionEntity entity : toUpdate) {
                        int newRetryCount = entity.getRetryCount() + 1;

                        if (newRetryCount >= MAX_RETRY_COUNT) {
                            // Mark as permanently failed after max retries
                            entity.setStatus(UserDeletionStatus.PERMANENTLY_FAILED);
                        } else {
                            entity.setStatus(UserDeletionStatus.CHAT_FAILED);
                            entity.setRetryCount(newRetryCount);
                        }
                    }
                    OperationExecutor.dbSave(() -> pendingUserDeletionDbService.saveAll(toUpdate),
                            SERVICE_NAME, METHOD_NAME);
                }
            } else {
                // Mark all as failed with retry limit check
                Specification<PendingUserDeletionEntity> updateSpec = (root, query, cb) -> root.get("userId")
                        .in(new ArrayList<>(userIds));
                List<PendingUserDeletionEntity> toUpdate = pendingUserDeletionDbService
                        .getPendingDeletions(updateSpec, Pageable.unpaged())
                        .getContent();

                for (PendingUserDeletionEntity entity : toUpdate) {
                    int newRetryCount = entity.getRetryCount() + 1;

                    if (newRetryCount >= MAX_RETRY_COUNT) {
                        entity.setStatus(UserDeletionStatus.PERMANENTLY_FAILED);
                    } else {
                        entity.setStatus(UserDeletionStatus.API_CALL_FAILED);
                        entity.setRetryCount(newRetryCount);
                    }
                }
                OperationExecutor.dbSave(() -> pendingUserDeletionDbService.saveAll(toUpdate),
                        SERVICE_NAME, METHOD_NAME);
            }
        } catch (Exception e) {
            // Mark all as failed with retry limit check
            Specification<PendingUserDeletionEntity> updateSpec = (root, query, cb) -> root.get("userId")
                    .in(new ArrayList<>(userIds));
            List<PendingUserDeletionEntity> toUpdate = pendingUserDeletionDbService
                    .getPendingDeletions(updateSpec, Pageable.unpaged())
                    .getContent();

            for (PendingUserDeletionEntity entity : toUpdate) {
                int newRetryCount = entity.getRetryCount() + 1;

                if (newRetryCount >= MAX_RETRY_COUNT) {
                    entity.setStatus(UserDeletionStatus.PERMANENTLY_FAILED);
                } else {
                    entity.setStatus(UserDeletionStatus.API_CALL_FAILED);
                    entity.setRetryCount(newRetryCount);
                }
            }
            OperationExecutor.dbSave(() -> pendingUserDeletionDbService.saveAll(toUpdate),
                    SERVICE_NAME, METHOD_NAME);
        }
    }
}