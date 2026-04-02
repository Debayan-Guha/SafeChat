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

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                Pageable pageable = PageRequest.of(page - 1, size, Sort.by("lastMessageAt").descending());

                Query query1 = new Query();
                query1.addCriteria(Criteria.where("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                query1.with(pageable);
                List<ConversationDocument> conversations = OperationExecutor.dbGet(
                                () -> conversationDbService.getConversations(query1),
                                SERVICE_NAME, METHOD_NAME);

                if (conversations == null || conversations.isEmpty()) {
                        throw new NotFoundException(ApiMessage.CONVERSATION_NOT_FOUND);
                }

                // 4. Create Page object (Automatically handles count and metadata)
                Page<ConversationDocument> pageResult = PageableExecutionUtils.getPage(
                                conversations,
                                pageable,
                                () -> conversationDbService.count(Query.of(query1).limit(-1).skip(-1)));

                List<ConversationMesssageResponseDto> data = pageResult.getContent()
                                .stream()
                                .map(conversation -> {

                                        ConversationResponseDto conversationDto = ConversationToDto
                                                        .convert(conversation);

                                        MessageResponseDto messageDto = null;

                                        String lastMessageId = conversation.getLastMessageId();

                                        if (lastMessageId != null) {

                                                Query msgQuery = new Query(
                                                                Criteria.where("id").is(lastMessageId));

                                                messageDto = messageDbService
                                                                .getMessage(msgQuery)
                                                                .map(MessageToDto::convert)
                                                                .orElse(null);
                                        }

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

                Map<String, Object> result = new HashMap<>();
                result.put("data", data);
                result.put("pagination", pagination);

                return result;
        }

        public ConversationResponseDto getConversationInfoById(String encryptToken, String conversationId)
                        throws NotFoundException {

                final String METHOD_NAME = "getConversationInfoById";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                Query query = Query.query(
                                Criteria.where("_id").is(conversationId)
                                                .and("participants").in(userId)
                                                .and("deletedForUsers").nin(userId));

                ConversationDocument conversation = OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(query),
                                SERVICE_NAME,
                                METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(
                                                "Conversation not found for id: " + conversationId));

                return OperationExecutor.map(
                                () -> ConversationToDto.convert(conversation),
                                SERVICE_NAME,
                                METHOD_NAME);
        }

        public Map<String, Object> getMessages(String encryptToken, String conversationId,
                        String sortBy, String sortDir, int page, int size)
                        throws NotFoundException {
                final String METHOD_NAME = "getMessages";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                Query query1 = new Query();
                query1.addCriteria(Criteria.where("_id").is(conversationId)
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                boolean conversationExists = OperationExecutor.dbGet(
                                () -> conversationDbService.exists(query1),
                                SERVICE_NAME, METHOD_NAME);

                if (!conversationExists) {
                        throw new NotFoundException("Conversation not found for id: " + conversationId);
                }

                Sort sort = sortDir.equalsIgnoreCase(SortDirection.DESC)
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();

                Pageable pageable = PageRequest.of(page - 1, size, sort);

                Query query2 = new Query();
                query2.addCriteria(Criteria.where("conversationId").is(conversationId)
                                .and("deletedForUsers").nin(userId));
                query2.with(pageable);

                List<MessageDocument> messages = OperationExecutor.dbGet(
                                () -> messageDbService.getMessages(query2),
                                SERVICE_NAME, METHOD_NAME);

                if (messages.isEmpty()) {
                        throw new NotFoundException(ApiMessage.MESSAGE_NOT_FOUND);
                }

                Page<MessageDocument> pageResult = PageableExecutionUtils.getPage(
                                messages,
                                pageable,
                                () -> messageDbService.count(Query.of(query2).limit(-1).skip(-1)));

                List<MessageResponseDto> data = pageResult.getContent().stream()
                                .map(MessageToDto::convert)
                                .collect(Collectors.toList());

                PaginationData pagination = PaginationData.builder()
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .currentPageTotalElements(data.size())
                                .currentPage(page)
                                .build();

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

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                Query query1 = new Query();
                query1.addCriteria(Criteria.where("_id").is(conversationId)
                                .and("participants").in(userId)
                                .and("deletedForUsers").nin(userId));

                boolean conversationExists = OperationExecutor.dbGet(
                                () -> conversationDbService.exists(query1),
                                SERVICE_NAME, METHOD_NAME);

                if (!conversationExists) {
                        throw new NotFoundException("Conversation not found for id: " + conversationId);
                }

                Sort sort = sortDir.equalsIgnoreCase(SortDirection.DESC)
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();

                Pageable pageable = PageRequest.of(page - 1, size, sort);

                Query query2 = new Query();
                query2.addCriteria(Criteria.where("conversationId").is(conversationId)
                                .and("sendAt").lt(beforeDate)
                                .and("deletedForUsers").nin(userId));
                query2.with(pageable);

                List<MessageDocument> messages = OperationExecutor.dbGet(
                                () -> messageDbService.getMessages(query2),
                                SERVICE_NAME, METHOD_NAME);

                if (messages.isEmpty()) {
                        throw new NotFoundException(ApiMessage.MESSAGE_NOT_FOUND);
                }

                Page<MessageDocument> pageResult = PageableExecutionUtils.getPage(
                                messages,
                                pageable,
                                () -> messageDbService.count(Query.of(query2).limit(-1).skip(-1)));

                List<MessageResponseDto> data = pageResult.getContent().stream()
                                .map(MessageToDto::convert)
                                .collect(Collectors.toList());

                PaginationData pagination = PaginationData.builder()
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .currentPageTotalElements(data.size())
                                .currentPage(page)
                                .build();

                Map<String, Object> result = new HashMap<>();
                result.put("data", data);
                result.put("pagination", pagination);

                return result;
        }

}