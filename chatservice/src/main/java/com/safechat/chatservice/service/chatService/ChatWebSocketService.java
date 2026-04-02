package com.safechat.chatservice.service.chatService;

import com.safechat.chatservice.document.ConversationDocument;
import com.safechat.chatservice.document.MessageDocument;
import com.safechat.chatservice.dto.request.create.MessageCreateRequestDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import com.safechat.chatservice.exception.ApplicationException.NotFoundException;
import com.safechat.chatservice.exception.ApplicationException.ValidationException;
import com.safechat.chatservice.jwt.JwtUtils;
import com.safechat.chatservice.mapper.toDocument.MessageToDocument;
import com.safechat.chatservice.mapper.toDto.MessageToDto;
import com.safechat.chatservice.service.dbService.ConversationDbService;
import com.safechat.chatservice.service.dbService.MessageDbService;
import com.safechat.chatservice.utility.Enumeration.DeleteType;
import com.safechat.chatservice.utility.OperationExecutor;
import com.safechat.chatservice.utility.api.ApiMessage;
import com.safechat.chatservice.utility.encryption.AesEncryption;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class ChatWebSocketService {

        private final String SERVICE_NAME = "ChatWebSocketService";

        private final ConversationDbService conversationDbService;
        private final MessageDbService messageDbService;
        private final AesEncryption aesEncryption;
        private final JwtUtils jwtUtils;

        public ChatWebSocketService(ConversationDbService conversationDbService,
                        MessageDbService messageDbService,
                        AesEncryption aesEncryption,
                        JwtUtils jwtUtils) {
                this.conversationDbService = conversationDbService;
                this.messageDbService = messageDbService;
                this.aesEncryption = aesEncryption;
                this.jwtUtils = jwtUtils;
        }

        /**
         * Send normal message
         */
        public MessageResponseDto sendMessage(String encryptToken, String conversationId,
                        MessageCreateRequestDto requestDto)
                        throws NotFoundException {
                final String METHOD_NAME = "sendMessage";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                // Verify conversation exists and user is participant
                Query conversationQuery = new Query();
                conversationQuery.addCriteria(Criteria.where("id").is(conversationId)
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                ConversationDocument conversation = OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(conversationQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.CONVERSATION_NOT_FOUND));

                // Create message
                MessageDocument message = MessageToDocument.convert(requestDto);

                // Save message
                MessageDocument savedMessage = OperationExecutor.dbSaveAndReturn(
                                () -> messageDbService.save(message),
                                SERVICE_NAME, METHOD_NAME);

                // Update conversation last message
                Query updateQuery = new Query();
                updateQuery.addCriteria(Criteria.where("id").is(conversationId));

                conversation.setLastMessageId(savedMessage.getId());
                conversation.setLastMessageAt(savedMessage.getSendAt());

                OperationExecutor.dbSave(
                                () -> conversationDbService.save(conversation),
                                SERVICE_NAME, METHOD_NAME);

                return OperationExecutor.map(() -> MessageToDto.convert(savedMessage), SERVICE_NAME, METHOD_NAME);
        }

        /**
         * Send privacy message (vanish mode)
         * Messages are NOT stored in database - only delivered via WebSocket
         * If receiver is offline, message is lost (by design for privacy)
         */
        public MessageResponseDto sendPrivacyMessage(String encryptToken, String conversationId,
                        MessageCreateRequestDto requestDto)
                        throws NotFoundException {
                final String METHOD_NAME = "sendPrivacyMessage";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                // Verify conversation exists and user is participant
                Query conversationQuery = new Query();
                conversationQuery.addCriteria(Criteria.where("id").is(conversationId)
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                ConversationDocument conversation = OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(conversationQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.CONVERSATION_NOT_FOUND));

                // Create message DTO for WebSocket response (NO DATABASE SAVE)
                MessageResponseDto messageResponse = MessageResponseDto.builder()
                                .conversationId(conversationId)
                                .senderId(userId)
                                .encryptedMessage(requestDto.getEncryptedMessage())
                                .sendAt(LocalDateTime.now())
                                .isRead(false)
                                .isDelivered(false)
                                .isEdited(false)
                                .expireAt(((requestDto.getExpirySeconds() != null && requestDto.getExpirySeconds() > 0))
                                                ? LocalDateTime.now().plusSeconds(requestDto.getExpirySeconds())
                                                : null)
                                .build();

                // DO NOT save to database - directly return for WebSocket broadcast
                // Update conversation last message (optional - for UI preview only)
                conversation.setLastMessageId(null); // No message ID for privacy chat
                conversation.setLastMessageAt(LocalDateTime.now());

                OperationExecutor.dbSave(
                                () -> conversationDbService.save(conversation),
                                SERVICE_NAME, METHOD_NAME);

                return messageResponse;
        }

        public MessageResponseDto markMessageAsDelivered(String encryptToken, String messageId)
                        throws NotFoundException {
                final String METHOD_NAME = "markMessageAsDelivered";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                // Fetch message where user is NOT the sender (i.e. user is the receiver)
                Query messageQuery = new Query();
                messageQuery.addCriteria(Criteria.where("id").is(messageId)
                                .and("senderId").ne(userId)
                                .and("isDelivered").is(false));

                MessageDocument message = OperationExecutor.dbGet(
                                () -> messageDbService.getMessage(messageQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.MESSAGE_NOT_FOUND));

                // Verify user is a participant in the conversation
                Query conversationQuery = new Query();
                conversationQuery.addCriteria(Criteria.where("id").is(message.getConversationId())
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(conversationQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.CONVERSATION_NOT_FOUND));

                // Mark as delivered
                message.setIsDelivered(true);
                message.setReceivedAt(LocalDateTime.now());

                MessageDocument savedMessage = OperationExecutor.dbSaveAndReturn(
                                () -> messageDbService.save(message),
                                SERVICE_NAME, METHOD_NAME);

                return OperationExecutor.map(() -> MessageToDto.convert(savedMessage), SERVICE_NAME, METHOD_NAME);
        }

        public MessageResponseDto markMessageAsRead(String encryptToken, String messageId) throws NotFoundException {
                final String METHOD_NAME = "markMessageAsRead";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                // Fetch message where user is NOT the sender (i.e. user is the receiver)
                Query messageQuery = new Query();
                messageQuery.addCriteria(Criteria.where("id").is(messageId)
                                .and("senderId").ne(userId)
                                .and("isRead").is(false));

                MessageDocument message = OperationExecutor.dbGet(
                                () -> messageDbService.getMessage(messageQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.MESSAGE_NOT_FOUND));

                // Verify user is a participant in the conversation
                Query conversationQuery = new Query();
                conversationQuery.addCriteria(Criteria.where("id").is(message.getConversationId())
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(conversationQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.CONVERSATION_NOT_FOUND));

                // Read implies delivered
                message.setIsRead(true);
                message.setIsDelivered(true);
                message.setReceivedAt(LocalDateTime.now());

                MessageDocument savedMessage = OperationExecutor.dbSaveAndReturn(
                                () -> messageDbService.save(message),
                                SERVICE_NAME, METHOD_NAME);

                return OperationExecutor.map(() -> MessageToDto.convert(savedMessage), SERVICE_NAME, METHOD_NAME);
        }

        public Map<String, Object> typingIndicator(String encryptToken, String conversationId, Boolean isTyping)
                        throws NotFoundException {
                final String METHOD_NAME = "typingIndicator";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                // Verify user is a participant in the conversation
                Query conversationQuery = new Query();
                conversationQuery.addCriteria(Criteria.where("id").is(conversationId)
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(conversationQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.CONVERSATION_NOT_FOUND));

                // No DB operation - just broadcast typing status
                Map<String, Object> typingStatus = new HashMap<>();
                typingStatus.put("conversationId", conversationId);
                typingStatus.put("userId", userId);
                typingStatus.put("isTyping", isTyping);
                typingStatus.put("timestamp", LocalDateTime.now());

                return typingStatus;
        }

        public Map<String, Object> deleteMessage(String encryptToken, String messageId, String deleteType)
                        throws NotFoundException {
                final String METHOD_NAME = "deleteMessage";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                // Fetch message and verify not already deleted for this user
                Query messageQuery = new Query();
                messageQuery.addCriteria(Criteria.where("id").is(messageId)
                                .and("deletedForUsers").nin(userId));

                MessageDocument message = OperationExecutor.dbGet(
                                () -> messageDbService.getMessage(messageQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.MESSAGE_NOT_FOUND));

                // Verify user is a participant in the conversation
                Query conversationQuery = new Query();
                conversationQuery.addCriteria(Criteria.where("id").is(message.getConversationId())
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                ConversationDocument conversation = OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(conversationQuery),
                                SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(ApiMessage.CONVERSATION_NOT_FOUND));

                if (DeleteType.EVERYONE.equals(deleteType)) {
                        // Only sender can delete for everyone
                        if (!message.getSenderId().equals(userId)) {
                                throw new ValidationException("Only sender can delete for everyone");
                        }

                        // Hard delete - remove from DB entirely
                        Query deleteQuery = new Query();
                        deleteQuery.addCriteria(Criteria.where("id").is(messageId)
                                        .and("conversationId").is(message.getConversationId()));
                        OperationExecutor.dbRemove(
                                        () -> messageDbService.delete(deleteQuery),
                                        SERVICE_NAME, METHOD_NAME);

                        // Update conversation last message after deletion
                        OperationExecutor.dbSave(() -> {
                                // Find the most recent message in this conversation (regardless of deletion)
                                Query latestMessageQuery = new Query();
                                latestMessageQuery.addCriteria(
                                                Criteria.where("conversationId").is(message.getConversationId()));
                                latestMessageQuery.with(Sort.by("sendAt").descending());
                                latestMessageQuery.limit(1);

                                MessageDocument latestMessage = messageDbService.getMessage(latestMessageQuery)
                                                .orElse(null);

                                // Update conversation with latest message info
                                if (latestMessage != null) {
                                        conversation.setLastMessageId(latestMessage.getId());
                                        conversation.setLastMessageAt(latestMessage.getSendAt());
                                } else {
                                        // If no messages left, keep the original lastMessageAt (don't set to null)
                                        // Or you can keep the existing value
                                        conversation.setLastMessageId(null);
                                        // lastMessageAt remains unchanged
                                }

                                conversationDbService.save(conversation);
                        }, SERVICE_NAME, METHOD_NAME);

                } else {
                        // DELETE_FOR_ME - soft delete, just add to deletedForUsers
                        message.getDeletedForUsers().add(userId);

                        OperationExecutor.dbSave(() -> {
                                messageDbService.save(message);

                                // Get the most recent message in the conversation (including those deleted for
                                // others)
                                Query latestMessageQuery = new Query();
                                latestMessageQuery.addCriteria(
                                                Criteria.where("conversationId").is(message.getConversationId()));
                                latestMessageQuery.with(Sort.by("sendAt").descending());
                                latestMessageQuery.limit(1);

                                MessageDocument latestMessage = messageDbService.getMessage(latestMessageQuery)
                                                .orElse(null);

                                // Update conversation - lastMessageId should be the most recent message
                                // globally
                                if (latestMessage != null) {
                                        conversation.setLastMessageId(latestMessage.getId());
                                        conversation.setLastMessageAt(latestMessage.getSendAt());
                                }
                                // lastMessageAt always has the time of the most recent message

                                conversationDbService.save(conversation);
                        }, SERVICE_NAME, METHOD_NAME);
                }

                Map<String, Object> deleteNotification = new HashMap<>();
                deleteNotification.put("messageId", messageId);
                deleteNotification.put("conversationId", message.getConversationId());
                deleteNotification.put("deleteType", deleteType);
                deleteNotification.put("timestamp", LocalDateTime.now());

                return deleteNotification;
        }

        public MessageResponseDto editMessage(String encryptToken, String messageId, String encryptedMessage)
                        throws NotFoundException {

                final String METHOD_NAME = "editMessage";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                Query query = Query.query(
                                Criteria.where("id").is(messageId)
                                                .and("senderId").is(userId));

                MessageDocument message = OperationExecutor.dbGet(
                                () -> messageDbService.getMessage(query),
                                SERVICE_NAME,
                                METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(
                                                ApiMessage.MESSAGE_NOT_FOUND));

                message.setEncryptedMessage(encryptedMessage);
                message.setIsEdited(true);

                MessageDocument updated = OperationExecutor.dbSaveAndReturn(
                                () -> messageDbService.save(message),
                                SERVICE_NAME,
                                METHOD_NAME);

                return OperationExecutor.map(() -> MessageToDto.convert(updated), SERVICE_NAME, METHOD_NAME);
        }

        public Map<String, Object> deleteMessages(String encryptToken, List<String> messageIdList, String deleteType)
                        throws NotFoundException {

                final String METHOD_NAME = "deleteMessages";

                String decryptToken = aesEncryption.decrypt(encryptToken);

                if (messageIdList == null || messageIdList.isEmpty()) {
                        throw new ValidationException("Message IDs list cannot be empty");
                }

                String conversationId = null;
                int deletedCount = 0;

                for (String messageId : messageIdList) {
                        try {
                                Map<String, Object> response = deleteMessage(encryptToken, messageId, deleteType);
                                if (conversationId == null) {
                                        conversationId = (String) response.get("conversationId");
                                }
                                deletedCount++;
                        } catch (NotFoundException e) {
                                // Skip if message not found, continue with others
                                continue;
                        }
                }

                Map<String, Object> deleteNotification = new HashMap<>();
                deleteNotification.put("messageIds", messageIdList);
                deleteNotification.put("conversationId", conversationId);
                deleteNotification.put("deleteType", deleteType);
                deleteNotification.put("count", deletedCount);
                deleteNotification.put("timestamp", LocalDateTime.now());

                return deleteNotification;
        }
}