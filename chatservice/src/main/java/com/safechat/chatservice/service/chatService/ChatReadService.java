package com.safechat.chatservice.service.chatService;

import com.safechat.chatservice.document.ConversationDocument;
import com.safechat.chatservice.document.MessageDocument;
import com.safechat.chatservice.dto.response.ConversationMesssageResponseDto;
import com.safechat.chatservice.dto.response.ConversationResponseDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import com.safechat.chatservice.exception.ApplicationException.NotFoundException;
import com.safechat.chatservice.jwt.JwtUtils;
import com.safechat.chatservice.mapper.toDto.ConversationToDto;
import com.safechat.chatservice.mapper.toDto.MessageToDto;
import com.safechat.chatservice.service.dbService.ConversationDbService;
import com.safechat.chatservice.service.dbService.MessageDbService;
import com.safechat.chatservice.utility.OperationExecutor;
import com.safechat.chatservice.utility.Enumeration.SortDirection;
import com.safechat.chatservice.utility.api.ApiMessage;
import com.safechat.chatservice.utility.api.PaginationData;
import com.safechat.chatservice.utility.encryption.AesEncryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatReadService {

        private final String SERVICE_NAME = "ChatReadService";
        private static final Logger log = LoggerFactory.getLogger(ChatReadService.class);

        private final ConversationDbService conversationDbService;
        private final MessageDbService messageDbService;
        private final AesEncryption aesEncryption;
        private final JwtUtils jwtUtils;

        public ChatReadService(ConversationDbService conversationDbService,
                        MessageDbService messageDbService, AesEncryption aesEncryption, JwtUtils jwtUtils) {
                this.conversationDbService = conversationDbService;
                this.messageDbService = messageDbService;
                this.aesEncryption = aesEncryption;
                this.jwtUtils = jwtUtils;
        }

        public Map<String, Object> getConversations(String encryptToken, int page, int size) throws NotFoundException {
                final String METHOD_NAME = "getConversations";

                log.debug("{} - Starting, page: {}, size: {}", METHOD_NAME, page, size);

                log.debug("{} - Decrypting token", METHOD_NAME);
                String decryptToken = aesEncryption.decrypt(encryptToken);

                log.debug("{} - Extracting userId from JWT", METHOD_NAME);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
                log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

                Pageable pageable = PageRequest.of(page - 1, size, Sort.by("lastMessageAt").descending());
                log.debug("{} - Pageable created: page={}, size={}", METHOD_NAME, page - 1, size);

                Query query1 = new Query();
                query1.addCriteria(Criteria.where("participants").in(userId)
                                .and("deletedForUsers").nin(userId));
                query1.with(pageable);

                log.debug("{} - Querying conversations for userId: {}", METHOD_NAME, userId);

                List<ConversationDocument> conversations = OperationExecutor.dbGet(
                                () -> conversationDbService.getConversations(query1),
                                SERVICE_NAME, METHOD_NAME);

                if (conversations == null || conversations.isEmpty()) {
                        log.warn("{} - No conversations found for userId: {}", METHOD_NAME, userId);
                        throw new NotFoundException(ApiMessage.CONVERSATION_NOT_FOUND);
                }

                log.debug("{} - Found {} conversations", METHOD_NAME, conversations.size());

                // 4. Create Page object (Automatically handles count and metadata)
                Page<ConversationDocument> pageResult = PageableExecutionUtils.getPage(
                                conversations,
                                pageable,
                                () -> conversationDbService.count(Query.of(query1).limit(-1).skip(-1)));

                log.debug("{} - Page result: totalElements={}, totalPages={}",
                                METHOD_NAME, pageResult.getTotalElements(), pageResult.getTotalPages());

                log.debug("{} - Building response data with messages and unread counts", METHOD_NAME);

                List<ConversationMesssageResponseDto> data = pageResult.getContent()
                                .stream()
                                .map(conversation -> {

                                        ConversationResponseDto conversationDto = ConversationToDto
                                                        .convert(conversation);

                                        MessageResponseDto messageDto = null;

                                        String lastMessageId = conversation.getLastMessageId();

                                        if (lastMessageId != null) {
                                                log.debug("{} - Fetching last message for conversation: {}",
                                                                METHOD_NAME, conversation.getId());

                                                Query msgQuery = new Query(
                                                                Criteria.where("id").is(lastMessageId));

                                                messageDto = messageDbService
                                                                .getMessage(msgQuery)
                                                                .map(MessageToDto::convert)
                                                                .orElse(null);
                                        }

                                        log.debug("{} - Counting unread messages for conversation: {}", METHOD_NAME,
                                                        conversation.getId());

                                        Long unreadCount = OperationExecutor.dbGet(
                                                        () -> {
                                                                Query query2 = new Query();
                                                                query2.addCriteria(Criteria.where("conversationId")
                                                                                .is(conversation.getId())
                                                                                .and("receiverId").is(userId)
                                                                                .and("isRead").is(false)
                                                                                .and("deletedForUsers").nin(userId));
                                                                return messageDbService.count(query2);
                                                        },
                                                        SERVICE_NAME, METHOD_NAME);

                                        return ConversationMesssageResponseDto.builder()
                                                        .conversationResponseDto(conversationDto)
                                                        .messageResponseDto(messageDto)
                                                        .unreadCount(unreadCount)
                                                        .build();
                                })
                                .toList();

                PaginationData pagination = PaginationData.builder()
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .currentPageTotalElements(data.size())
                                .currentPage(page)
                                .build();

                log.info("{} - Successfully fetched {} conversations for userId: {}", METHOD_NAME, data.size(), userId);

                Map<String, Object> result = new HashMap<>();
                result.put("data", data);
                result.put("pagination", pagination);

                return result;
        }

        public ConversationResponseDto getConversationInfoById(String encryptToken, String conversationId)
                        throws NotFoundException {

                final String METHOD_NAME = "getConversationInfoById";

                log.debug("{} - Starting for conversationId: {}", METHOD_NAME, conversationId);

                log.debug("{} - Decrypting token", METHOD_NAME);
                String decryptToken = aesEncryption.decrypt(encryptToken);

                log.debug("{} - Extracting userId from JWT", METHOD_NAME);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
                log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

                Query query = Query.query(
                                Criteria.where("id").is(conversationId)
                                                .and("participants").in(userId)
                                                .and("deletedForUsers").nin(userId));

                log.debug("{} - Querying conversation for userId: {}, conversationId: {}", METHOD_NAME, userId,
                                conversationId);

                ConversationDocument conversation = OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(query),
                                SERVICE_NAME,
                                METHOD_NAME)
                                .orElseThrow(() -> {
                                        log.warn("{} - Conversation not found for conversationId: {}, userId: {}",
                                                        METHOD_NAME, conversationId, userId);
                                        return new NotFoundException(
                                                        "Conversation not found for id: " + conversationId);
                                });

                log.debug("{} - Conversation found, converting to DTO", METHOD_NAME);

                ConversationResponseDto response = OperationExecutor.map(
                                () -> ConversationToDto.convert(conversation),
                                SERVICE_NAME,
                                METHOD_NAME);

                log.info("{} - Successfully fetched conversation info for conversationId: {}, userId: {}",
                                METHOD_NAME, conversationId, userId);

                return response;
        }

        public Map<String, Object> getMessages(String encryptToken, String conversationId,
                        String sortBy, String sortDir, int page, int size)
                        throws NotFoundException {
                final String METHOD_NAME = "getMessages";

                log.debug("{} - Starting for conversationId: {}, sortBy: {}, sortDir: {}, page: {}, size: {}",
                                METHOD_NAME, conversationId, sortBy, sortDir, page, size);

                log.debug("{} - Decrypting token", METHOD_NAME);
                String decryptToken = aesEncryption.decrypt(encryptToken);

                log.debug("{} - Extracting userId from JWT", METHOD_NAME);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
                log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

                Query query1 = new Query();
                query1.addCriteria(Criteria.where("id").is(conversationId)
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                log.debug("{} - Checking if conversation exists for userId: {}, conversationId: {}", METHOD_NAME,
                                userId, conversationId);

                boolean conversationExists = OperationExecutor.dbGet(
                                () -> conversationDbService.exists(query1),
                                SERVICE_NAME, METHOD_NAME);

                if (!conversationExists) {
                        log.warn("{} - Conversation not found for conversationId: {}, userId: {}", METHOD_NAME,
                                        conversationId, userId);
                        throw new NotFoundException("Conversation not found for id: " + conversationId);
                }

                log.debug("{} - Conversation exists, proceeding to fetch messages", METHOD_NAME);

                Sort sort = sortDir.equalsIgnoreCase(SortDirection.DESC)
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();

                log.debug("{} - Sort created: {} {}", METHOD_NAME, sortBy, sortDir);

                Pageable pageable = PageRequest.of(page - 1, size, sort);
                log.debug("{} - Pageable created: page={}, size={}", METHOD_NAME, page - 1, size);

                Query query2 = new Query();
                query2.addCriteria(Criteria.where("conversationId").is(conversationId)
                                .and("deletedForUsers").nin(userId));
                query2.with(pageable);

                log.debug("{} - Querying messages for conversationId: {}", METHOD_NAME, conversationId);

                List<MessageDocument> messages = OperationExecutor.dbGet(
                                () -> messageDbService.getMessages(query2),
                                SERVICE_NAME, METHOD_NAME);

                if (messages.isEmpty()) {
                        log.warn("{} - No messages found for conversationId: {}", METHOD_NAME, conversationId);
                        throw new NotFoundException(ApiMessage.MESSAGE_NOT_FOUND);
                }

                log.debug("{} - Found {} messages", METHOD_NAME, messages.size());

                Page<MessageDocument> pageResult = PageableExecutionUtils.getPage(
                                messages,
                                pageable,
                                () -> messageDbService.count(Query.of(query2).limit(-1).skip(-1)));

                log.debug("{} - Page result: totalElements={}, totalPages={}",
                                METHOD_NAME, pageResult.getTotalElements(), pageResult.getTotalPages());

                List<MessageResponseDto> data = pageResult.getContent().stream()
                                .map(MessageToDto::convert)
                                .collect(Collectors.toList());

                PaginationData pagination = PaginationData.builder()
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .currentPageTotalElements(data.size())
                                .currentPage(page)
                                .build();

                log.info("{} - Successfully fetched {} messages for conversationId: {}, userId: {}",
                                METHOD_NAME, data.size(), conversationId, userId);

                Map<String, Object> result = new HashMap<>();
                result.put("data", data);
                result.put("pagination", pagination);

                return result;
        }

        public Map<String, Object> getMessagesBeforeDate(String encryptToken, String conversationId,
                        LocalDateTime beforeDate,
                        String sortBy, String sortDir, int page, int size)
                        throws NotFoundException {
                final String METHOD_NAME = "getMessagesBeforeDate";

                log.debug("{} - Starting for conversationId: {}, beforeDate: {}, sortBy: {}, sortDir: {}, page: {}, size: {}",
                                METHOD_NAME, conversationId, beforeDate, sortBy, sortDir, page, size);

                log.debug("{} - Decrypting token", METHOD_NAME);
                String decryptToken = aesEncryption.decrypt(encryptToken);

                log.debug("{} - Extracting userId from JWT", METHOD_NAME);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
                log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

                Query query1 = new Query();
                query1.addCriteria(Criteria.where("id").is(conversationId)
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                log.debug("{} - Checking if conversation exists for userId: {}, conversationId: {}", METHOD_NAME,
                                userId, conversationId);

                boolean conversationExists = OperationExecutor.dbGet(
                                () -> conversationDbService.exists(query1),
                                SERVICE_NAME, METHOD_NAME);

                if (!conversationExists) {
                        log.warn("{} - Conversation not found for conversationId: {}, userId: {}", METHOD_NAME,
                                        conversationId, userId);
                        throw new NotFoundException("Conversation not found for id: " + conversationId);
                }

                log.debug("{} - Conversation exists, proceeding to fetch messages before date: {}", METHOD_NAME,
                                beforeDate);

                Sort sort = sortDir.equalsIgnoreCase(SortDirection.DESC)
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();

                log.debug("{} - Sort created: {} {}", METHOD_NAME, sortBy, sortDir);

                Pageable pageable = PageRequest.of(page - 1, size, sort);
                log.debug("{} - Pageable created: page={}, size={}", METHOD_NAME, page - 1, size);

                Query query2 = new Query();
                query2.addCriteria(Criteria.where("conversationId").is(conversationId)
                                .and("sendAt").lt(beforeDate)
                                .and("deletedForUsers").nin(userId));
                query2.with(pageable);

                log.debug("{} - Querying messages before date: {} for conversationId: {}", METHOD_NAME, beforeDate,
                                conversationId);

                List<MessageDocument> messages = OperationExecutor.dbGet(
                                () -> messageDbService.getMessages(query2),
                                SERVICE_NAME, METHOD_NAME);

                if (messages.isEmpty()) {
                        log.warn("{} - No messages found before date: {} for conversationId: {}", METHOD_NAME,
                                        beforeDate, conversationId);
                        throw new NotFoundException(ApiMessage.MESSAGE_NOT_FOUND);
                }

                log.debug("{} - Found {} messages", METHOD_NAME, messages.size());

                Page<MessageDocument> pageResult = PageableExecutionUtils.getPage(
                                messages,
                                pageable,
                                () -> messageDbService.count(Query.of(query2).limit(-1).skip(-1)));

                log.debug("{} - Page result: totalElements={}, totalPages={}",
                                METHOD_NAME, pageResult.getTotalElements(), pageResult.getTotalPages());

                List<MessageResponseDto> data = pageResult.getContent().stream()
                                .map(MessageToDto::convert)
                                .collect(Collectors.toList());

                PaginationData pagination = PaginationData.builder()
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .currentPageTotalElements(data.size())
                                .currentPage(page)
                                .build();

                log.info("{} - Successfully fetched {} messages before date: {} for conversationId: {}, userId: {}",
                                METHOD_NAME, data.size(), beforeDate, conversationId, userId);

                Map<String, Object> result = new HashMap<>();
                result.put("data", data);
                result.put("pagination", pagination);

                return result;
        }
}