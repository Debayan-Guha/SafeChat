package com.safechat.userservice.service.chatService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
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

        log.debug("{} - Starting user deletion batch for {} users", METHOD_NAME, userIds.size());
        log.debug("{} - User IDs: {}", METHOD_NAME, userIds);

        try {
            log.debug("{} - Calling chat service API for user deletion", METHOD_NAME);
            
            ApiResponseFormatter<List<UserDeletionStatusDto>> response = chatServiceApiCall
                    .sendUserDeletionBatch(new ArrayList<>(userIds));

            if (response != null && response.getStatusCode() == 200 && response.getData() != null) {
                log.debug("{} - API call successful, received response with {} status entries", 
                         METHOD_NAME, response.getData().size());
                
                List<String> successUsers = new ArrayList<>();
                List<String> failedUsers = new ArrayList<>();

                for (UserDeletionStatusDto status : response.getData()) {
                    if (UserDeletionStatus.CHAT_SUCCESS.equals(status.getStatus())) {
                        successUsers.add(status.getUserId());
                        log.debug("{} - User {} deletion successful in chat service", METHOD_NAME, status.getUserId());
                    } else {
                        failedUsers.add(status.getUserId());
                        log.warn("{} - User {} deletion failed in chat service with status: {}", 
                                METHOD_NAME, status.getUserId(), status.getStatus());
                    }
                }

                log.info("{} - Batch result: {} successful, {} failed out of {} total users", 
                        METHOD_NAME, successUsers.size(), failedUsers.size(), userIds.size());

                // Batch delete successes
                if (!successUsers.isEmpty()) {
                    log.debug("{} - Deleting {} successful entries from pending deletions table", 
                             METHOD_NAME, successUsers.size());
                    
                    Specification<PendingUserDeletionEntity> deleteSpec = (root, query, cb) -> root
                            .get("userId").in(successUsers);
                    List<PendingUserDeletionEntity> toDelete = pendingUserDeletionDbService
                            .getPendingDeletions(deleteSpec, Pageable.unpaged())
                            .getContent();
                    OperationExecutor.dbRemove(() -> pendingUserDeletionDbService.deleteAll(toDelete),
                            SERVICE_NAME, METHOD_NAME);
                    
                    log.debug("{} - Successfully removed {} entries from pending deletions", 
                             METHOD_NAME, toDelete.size());
                }

                // Batch update failures with retry limit check
                if (!failedUsers.isEmpty()) {
                    log.debug("{} - Updating {} failed entries with retry count increment", 
                             METHOD_NAME, failedUsers.size());
                    
                    Specification<PendingUserDeletionEntity> updateSpec = (root, query, cb) -> root
                            .get("userId").in(failedUsers);
                    List<PendingUserDeletionEntity> toUpdate = pendingUserDeletionDbService
                            .getPendingDeletions(updateSpec, Pageable.unpaged())
                            .getContent();

                    int permanentlyFailedCount = 0;
                    for (PendingUserDeletionEntity entity : toUpdate) {
                        int newRetryCount = entity.getRetryCount() + 1;

                        if (newRetryCount >= MAX_RETRY_COUNT) {
                            // Mark as permanently failed after max retries
                            entity.setStatus(UserDeletionStatus.PERMANENTLY_FAILED);
                            permanentlyFailedCount++;
                            log.warn("{} - User {} reached max retry count ({}), marking as PERMANENTLY_FAILED", 
                                    METHOD_NAME, entity.getUserId(), MAX_RETRY_COUNT);
                        } else {
                            entity.setStatus(UserDeletionStatus.CHAT_FAILED);
                            entity.setRetryCount(newRetryCount);
                            log.debug("{} - User {} retry count incremented to {}/{}", 
                                    METHOD_NAME, entity.getUserId(), newRetryCount, MAX_RETRY_COUNT);
                        }
                    }
                    OperationExecutor.dbSave(() -> pendingUserDeletionDbService.saveAll(toUpdate),
                            SERVICE_NAME, METHOD_NAME);
                    
                    log.info("{} - Updated {} failed entries ({} permanently failed)", 
                            METHOD_NAME, toUpdate.size(), permanentlyFailedCount);
                }
            } else {
                log.warn("{} - API call returned null or non-200 status code. Status code: {}", 
                        METHOD_NAME, response != null ? response.getStatusCode() : "null");
                
                // Mark all as failed with retry limit check
                Specification<PendingUserDeletionEntity> updateSpec = (root, query, cb) -> root.get("userId")
                        .in(new ArrayList<>(userIds));
                List<PendingUserDeletionEntity> toUpdate = pendingUserDeletionDbService
                        .getPendingDeletions(updateSpec, Pageable.unpaged())
                        .getContent();

                int permanentlyFailedCount = 0;
                for (PendingUserDeletionEntity entity : toUpdate) {
                    int newRetryCount = entity.getRetryCount() + 1;

                    if (newRetryCount >= MAX_RETRY_COUNT) {
                        entity.setStatus(UserDeletionStatus.PERMANENTLY_FAILED);
                        permanentlyFailedCount++;
                        log.warn("{} - User {} reached max retry count ({}), marking as PERMANENTLY_FAILED", 
                                METHOD_NAME, entity.getUserId(), MAX_RETRY_COUNT);
                    } else {
                        entity.setStatus(UserDeletionStatus.API_CALL_FAILED);
                        entity.setRetryCount(newRetryCount);
                        log.debug("{} - User {} retry count incremented to {}/{} due to API failure", 
                                METHOD_NAME, entity.getUserId(), newRetryCount, MAX_RETRY_COUNT);
                    }
                }
                OperationExecutor.dbSave(() -> pendingUserDeletionDbService.saveAll(toUpdate),
                        SERVICE_NAME, METHOD_NAME);
                
                log.info("{} - Marked {} users as failed ({} permanently failed) due to invalid API response", 
                        METHOD_NAME, toUpdate.size(), permanentlyFailedCount);
            }
        } catch (Exception e) {
            log.error("{} - Exception occurred while processing user deletion batch", METHOD_NAME, e);
            
            // Mark all as failed with retry limit check
            Specification<PendingUserDeletionEntity> updateSpec = (root, query, cb) -> root.get("userId")
                    .in(new ArrayList<>(userIds));
            List<PendingUserDeletionEntity> toUpdate = pendingUserDeletionDbService
                    .getPendingDeletions(updateSpec, Pageable.unpaged())
                    .getContent();

            int permanentlyFailedCount = 0;
            for (PendingUserDeletionEntity entity : toUpdate) {
                int newRetryCount = entity.getRetryCount() + 1;

                if (newRetryCount >= MAX_RETRY_COUNT) {
                    entity.setStatus(UserDeletionStatus.PERMANENTLY_FAILED);
                    permanentlyFailedCount++;
                    log.warn("{} - User {} reached max retry count ({}), marking as PERMANENTLY_FAILED after exception", 
                            METHOD_NAME, entity.getUserId(), MAX_RETRY_COUNT);
                } else {
                    entity.setStatus(UserDeletionStatus.API_CALL_FAILED);
                    entity.setRetryCount(newRetryCount);
                    log.debug("{} - User {} retry count incremented to {}/{} after exception", 
                            METHOD_NAME, entity.getUserId(), newRetryCount, MAX_RETRY_COUNT);
                }
            }
            OperationExecutor.dbSave(() -> pendingUserDeletionDbService.saveAll(toUpdate),
                    SERVICE_NAME, METHOD_NAME);
            
            log.info("{} - After exception: Marked {} users as failed ({} permanently failed)", 
                    METHOD_NAME, toUpdate.size(), permanentlyFailedCount);
        }
        
        log.debug("{} - User deletion batch processing completed", METHOD_NAME);
    }
}