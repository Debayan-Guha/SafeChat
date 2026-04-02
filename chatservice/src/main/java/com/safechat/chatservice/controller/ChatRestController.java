package com.safechat.chatservice.controller;

import com.safechat.chatservice.dto.response.ConversationMesssageResponseDto;
import com.safechat.chatservice.dto.response.ConversationResponseDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import com.safechat.chatservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.chatservice.exception.ApplicationException.NotFoundException;
import com.safechat.chatservice.exception.ApplicationException.ValidationException;
import com.safechat.chatservice.service.chatService.ChatReadService;
import com.safechat.chatservice.service.chatService.ChatWriteService;
import com.safechat.chatservice.utility.api.ApiMessage;
import com.safechat.chatservice.utility.api.ApiResponseFormatter;
import com.safechat.chatservice.utility.api.PaginationData;
import com.safechat.chatservice.utility.Enumeration.DeleteType;
import com.safechat.chatservice.utility.Enumeration.SortDirection;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatRestController {

        private final ChatReadService chatReadService;
        private final ChatWriteService chatWriteService;

        public ChatRestController(ChatReadService chatReadService, ChatWriteService chatWriteService) {
                this.chatReadService = chatReadService;
                this.chatWriteService = chatWriteService;
        }

        @GetMapping("/conversations")
        public ResponseEntity<ApiResponseFormatter<List<ConversationMesssageResponseDto>>> getConversations(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) throws NotFoundException {

                if (page < 0 || size <= 0) {
                        throw new ValidationException(ApiMessage.PAGE_VALIDATION_ERROR);
                }

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                Map<String, Object> result = chatReadService.getConversations(encryptToken, page, size);

                List<ConversationMesssageResponseDto> data = (List<ConversationMesssageResponseDto>) result.get("data");
                PaginationData pagination = (PaginationData) result.get("pagination");

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.CONVERSATION_FOUND,
                                data,
                                pagination));
        }

        @GetMapping("/conversations/{conversationId}")
        public ResponseEntity<ApiResponseFormatter<ConversationResponseDto>> getConversationInfoById(
                        @PathVariable(required = true) String conversationId)
                        throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                ConversationResponseDto response = chatReadService.getConversationInfoById(encryptToken,
                                conversationId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.CONVERSATION_FOUND,
                                response));
        }

        @DeleteMapping("/conversations/{conversationId}")
        public ResponseEntity<ApiResponseFormatter<Void>> deleteConversation(
                        @PathVariable(required = true) String conversationId,
                        @RequestParam(required = true) String deleteType)
                        throws NotFoundException, AlreadyExistsException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                if (!DeleteType.isValid(deleteType)) {
                        throw new ValidationException("Invalid delete type");
                }
                chatWriteService.deleteConversation(encryptToken, conversationId, deleteType);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.CONVERSATION_DELETED));
        }

        @GetMapping("/conversations/{conversationId}/messages")
        public ResponseEntity<ApiResponseFormatter<List<MessageResponseDto>>> getConversationMessages(
                        @PathVariable(required = true) String conversationId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "50") int size,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeDate)
                        throws NotFoundException {

                if (page < 0 || size <= 0) {
                        throw new ValidationException(ApiMessage.PAGE_VALIDATION_ERROR);
                }

                String sortBy="sendAt";
                String sortDir="desc";

                if (!SortDirection.isValid(sortDir)) {
                        throw new ValidationException("Invalid sort direction");
                }

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                Map<String, Object> result;
                if (beforeDate != null) {
                        result = chatReadService.getMessagesBeforeDate(encryptToken, conversationId, beforeDate, sortBy,
                                        sortDir,page,size);
                } else {
                        result = chatReadService.getMessages(encryptToken, conversationId, sortBy, sortDir,page,size);
                }

                List<MessageResponseDto> data = (List<MessageResponseDto>) result.get("data");
                PaginationData pagination = (PaginationData) result.get("pagination");

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.MESSAGE_FOUND,
                                data,
                                pagination));
        }

}