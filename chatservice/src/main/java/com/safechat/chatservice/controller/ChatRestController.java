package com.safechat.chatservice.controller;

import com.safechat.chatservice.dto.response.ConversationMesssageResponseDto;
import com.safechat.chatservice.dto.response.ConversationResponseDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import com.safechat.chatservice.exception.ApplicationException.NotFoundException;
import com.safechat.chatservice.exception.ApplicationException.ValidationException;
import com.safechat.chatservice.externalApiCall.UserDeletionStatusDto;
import com.safechat.chatservice.service.chatService.ChatReadService;
import com.safechat.chatservice.service.chatService.ChatWriteService;
import com.safechat.chatservice.utility.api.ApiMessage;
import com.safechat.chatservice.utility.api.ApiResponseFormatter;
import com.safechat.chatservice.utility.api.PaginationData;
import com.safechat.chatservice.utility.Enumeration.SortDirection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chatservice")
@Tag(name = "Chat Management", description = "APIs for conversation and message management")
public class ChatRestController {

        private final ChatReadService chatReadService;
        private final ChatWriteService chatWriteService;

        public ChatRestController(ChatReadService chatReadService, ChatWriteService chatWriteService) {
                this.chatReadService = chatReadService;
                this.chatWriteService = chatWriteService;
        }

        @Operation(summary = "Get all conversations", description = "Retrieves all conversations for the authenticated user with pagination, sorted by last message time.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Conversations found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "No conversations found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/conversations")
        public ResponseEntity<ApiResponseFormatter<List<ConversationMesssageResponseDto>>> getConversations(
                        @Parameter(description = "Page number (1-indexed)", example = "1") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size)
                        throws NotFoundException {

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

        @Operation(summary = "Get conversation by ID", description = "Retrieves detailed information of a specific conversation by its ID. User must be a participant.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Conversation found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/conversations/{conversationId}")
        public ResponseEntity<ApiResponseFormatter<ConversationResponseDto>> getConversationInfoById(
                        @Parameter(description = "Conversation ID", required = true) @PathVariable(required = true) String conversationId)
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

        @Operation(summary = "Get messages in a conversation", description = "Retrieves paginated messages for a conversation. Optionally filter messages before a specific date. Messages are sorted by sendAt descending.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Messages found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "Conversation or messages not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/conversations/{conversationId}/messages")
        public ResponseEntity<ApiResponseFormatter<List<MessageResponseDto>>> getConversationMessages(
                        @Parameter(description = "Conversation ID", required = true) @PathVariable(required = true) String conversationId,
                        @Parameter(description = "Page number (1-indexed)", example = "1") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Number of items per page", example = "50") @RequestParam(defaultValue = "50") int size,
                        @Parameter(description = "Fetch messages before this date (ISO 8601 format). If omitted, fetches latest messages.", example = "2024-01-15T10:30:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeDate)
                        throws NotFoundException {

                if (page < 0 || size <= 0) {
                        throw new ValidationException(ApiMessage.PAGE_VALIDATION_ERROR);
                }

                String sortBy = "sendAt";
                String sortDir = "desc";

                if (!SortDirection.isValid(sortDir)) {
                        throw new ValidationException("Invalid sort direction");
                }

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                Map<String, Object> result;
                if (beforeDate != null) {
                        result = chatReadService.getMessagesBeforeDate(encryptToken, conversationId, beforeDate, sortBy,
                                        sortDir, page, size);
                } else {
                        result = chatReadService.getMessages(encryptToken, conversationId, sortBy, sortDir, page, size);
                }

                List<MessageResponseDto> data = (List<MessageResponseDto>) result.get("data");
                PaginationData pagination = (PaginationData) result.get("pagination");

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.MESSAGE_FOUND,
                                data,
                                pagination));
        }

        @Operation(summary = "Handle user deletion (internal)", description = "Internal endpoint to clean up all conversations and messages associated with deleted users. Called by user service only.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User deletion processed", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/internal/user-deletion")
        public ResponseEntity<ApiResponseFormatter<List<UserDeletionStatusDto>>> handleUserDeletion(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of user IDs to delete", required = true) @RequestBody List<String> userIds) {

                List<UserDeletionStatusDto> statusList = chatWriteService.handleUserDeletion(userIds);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                "User deletion processed",
                                statusList));
        }

}