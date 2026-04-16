package com.safechat.chatservice.service.chatService;

import java.util.ArrayList;
import java.util.List;

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
        List<UserDeletionStatusDto> statusList = new ArrayList<>();

        try {
            // 1. Batch delete all 1:1 conversations
            Query oneToOneQuery = new Query(
                    Criteria.where("participants").in(userIds)
                            .and("participants").size(2));
            OperationExecutor.dbRemove(
                    () -> conversationDbService.delete(oneToOneQuery),
                    SERVICE_NAME, METHOD_NAME);

            // 2. Batch delete all messages sent by these users
            Query userMessagesQuery = new Query(
                    Criteria.where("senderId").in(userIds));
            OperationExecutor.dbRemove(
                    () -> messageDbService.delete(userMessagesQuery),
                    SERVICE_NAME, METHOD_NAME);

            // 3. Batch remove users from participants in group chats (ONE update operation)
            Query groupChatQuery = new Query(
                    Criteria.where("participants").in(userIds)
                            .and("participants").nin(userIds)); // Only groups where users exist
            Update update = new Update().pullAll("participants", userIds.toArray());
            OperationExecutor.dbSave(
                    () -> conversationDbService.updateMulti(groupChatQuery, update),
                    SERVICE_NAME, METHOD_NAME);

            // All succeeded
            for (String userId : userIds) {
                statusList.add(UserDeletionStatusDto.builder()
                        .userId(userId)
                        .status(UserDeletionStatus.CHAT_SUCCESS)
                        .build());
            }

        } catch (Exception e) {
            // If batch fails, mark all as CHAT_FAILED
            for (String userId : userIds) {
                statusList.add(UserDeletionStatusDto.builder()
                        .userId(userId)
                        .status(UserDeletionStatus.CHAT_FAILED)
                        .build());
            }
        }

        return statusList;
    }
}