package com.safechat.chatservice.service.chatService;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safechat.chatservice.externalApiCall.UserDeletionStatusDto;
import com.safechat.chatservice.service.dbService.ConversationDbService;
import com.safechat.chatservice.service.dbService.MessageDbService;
import com.safechat.chatservice.utility.Enumeration.UserDeletionStatus;
import com.safechat.chatservice.utility.OperationExecutor;

@Service
public class ChatWriteService {

    private final String SERVICE_NAME = "ChatWriteService";
    private static final Logger log = LoggerFactory.getLogger(ChatWriteService.class);

    private final ConversationDbService conversationDbService;
    private final MessageDbService messageDbService;

    public ChatWriteService(ConversationDbService conversationDbService,
            MessageDbService messageDbService) {
        this.conversationDbService = conversationDbService;
        this.messageDbService = messageDbService;
    }

    @Transactional
    public List<UserDeletionStatusDto> handleUserDeletion(List<String> userIds) {
        final String METHOD_NAME = "handleUserDeletion";
        
        log.debug("{} - Starting user deletion for {} users", METHOD_NAME, userIds != null ? userIds.size() : 0);
        log.debug("{} - User IDs: {}", METHOD_NAME, userIds);
        
        List<UserDeletionStatusDto> statusList = new ArrayList<>();

        try {
            log.debug("{} - Step 1: Deleting 1:1 conversations for users", METHOD_NAME);
            
            // 1. Batch delete all 1:1 conversations
            Query oneToOneQuery = new Query(
                    Criteria.where("participants").in(userIds)
                            .and("participants").size(2));
            OperationExecutor.dbRemove(
                    () -> conversationDbService.delete(oneToOneQuery),
                    SERVICE_NAME, METHOD_NAME);
            
            log.debug("{} - Step 1 completed: Deleted 1:1 conversations", METHOD_NAME);

            log.debug("{} - Step 2: Deleting messages sent by users", METHOD_NAME);
            
            // 2. Batch delete all messages sent by these users
            Query userMessagesQuery = new Query(
                    Criteria.where("senderId").in(userIds));
            OperationExecutor.dbRemove(
                    () -> messageDbService.delete(userMessagesQuery),
                    SERVICE_NAME, METHOD_NAME);
            
            log.debug("{} - Step 2 completed: Deleted user messages", METHOD_NAME);

            log.debug("{} - Step 3: Removing users from group chat participants", METHOD_NAME);
            
            // 3. Batch remove users from participants in group chats (ONE update operation)
            Query groupChatQuery = new Query(
                    Criteria.where("participants").in(userIds)
                            .and("participants").nin(userIds)); // Only groups where users exist
            Update update = new Update().pullAll("participants", userIds.toArray());
            OperationExecutor.dbSave(
                    () -> conversationDbService.updateMulti(groupChatQuery, update),
                    SERVICE_NAME, METHOD_NAME);
            
            log.debug("{} - Step 3 completed: Removed users from group chats", METHOD_NAME);

            // All succeeded
            log.debug("{} - Building success status list for {} users", METHOD_NAME, userIds.size());
            
            for (String userId : userIds) {
                statusList.add(UserDeletionStatusDto.builder()
                        .userId(userId)
                        .status(UserDeletionStatus.CHAT_SUCCESS)
                        .build());
            }
            
            log.info("{} - User deletion completed successfully for {} users", METHOD_NAME, userIds.size());

        } catch (Exception e) {
            log.error("{} - Exception occurred during user deletion: {}", METHOD_NAME, e.getMessage());
            log.debug("{} - Exception details: ", METHOD_NAME, e);
            
            // If batch fails, mark all as CHAT_FAILED
            log.warn("{} - Marking all {} users as CHAT_FAILED due to exception", METHOD_NAME, userIds.size());
            
            for (String userId : userIds) {
                statusList.add(UserDeletionStatusDto.builder()
                        .userId(userId)
                        .status(UserDeletionStatus.CHAT_FAILED)
                        .build());
            }
            
            log.info("{} - User deletion failed, returning {} failure statuses", METHOD_NAME, statusList.size());
        }

        log.debug("{} - Returning status list with {} entries", METHOD_NAME, statusList.size());
        return statusList;
    }
}