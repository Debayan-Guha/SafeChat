package com.safechat.chatservice.controller;

import com.safechat.chatservice.dto.request.create.MessageCreateRequestDto;
import com.safechat.chatservice.dto.request.update.MessageUpdateRequestDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import com.safechat.chatservice.exception.ApplicationException.NotFoundException;
import com.safechat.chatservice.exception.ApplicationException.ValidationException;
import com.safechat.chatservice.service.chatService.ChatWebSocketService;
import com.safechat.chatservice.utility.Enumeration.DeleteType;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class ChatWebSocketController {

        private final ChatWebSocketService chatWebSocketService;
        private final SimpMessagingTemplate messagingTemplate;

        public ChatWebSocketController(ChatWebSocketService chatWebSocketService,
                        SimpMessagingTemplate messagingTemplate) {
                this.chatWebSocketService = chatWebSocketService;
                this.messagingTemplate = messagingTemplate;
        }

        @MessageMapping("/chat.send/{conversationId}")
        public void sendMessage(
                        @DestinationVariable String conversationId,
                        @Payload @Valid MessageCreateRequestDto requestDto) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.sendMessage(encryptToken, conversationId,
                                requestDto);

                messagingTemplate.convertAndSend("/topic/messages/" + conversationId, response);
        }

        @MessageMapping("/chat.privacy/{conversationId}")
        public void sendPrivacyMessage(
                        @DestinationVariable String conversationId,
                        @Payload @Valid MessageCreateRequestDto requestDto) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.sendPrivacyMessage(encryptToken, conversationId,
                                requestDto);

                messagingTemplate.convertAndSend("/topic/messages/" + conversationId, response);
        }

        @MessageMapping("/chat.delivered/{messageId}")
        public void markAsDelivered(
                        @DestinationVariable String messageId) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.markMessageAsDelivered(encryptToken, messageId);

                messagingTemplate.convertAndSend("/topic/delivery/" + response.getConversationId(), response);
        }

        @MessageMapping("/chat.read/{messageId}")
        public void markAsRead(
                        @DestinationVariable String messageId) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.markMessageAsRead(encryptToken, messageId);

                messagingTemplate.convertAndSend("/topic/delivery/" + response.getConversationId(), response);
        }

        @MessageMapping("/chat.typing/{conversationId}")
        public void typingIndicator(
                        @DestinationVariable String conversationId,
                        @Payload Boolean isTyping) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                Map<String, Object> response = chatWebSocketService.typingIndicator(encryptToken, conversationId,
                                isTyping);

                messagingTemplate.convertAndSend("/topic/typing/" + conversationId, (Object) response);
        }

        @MessageMapping("/chat.edit/{messageId}")
        public void editMessage(
                        @DestinationVariable String messageId,
                        @Payload @Valid MessageUpdateRequestDto requestDto) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.editMessage(encryptToken, messageId,
                                requestDto.getEncryptedMessage());

                messagingTemplate.convertAndSend("/topic/messages/" + response.getConversationId(), response);
        }

        @MessageMapping("/chat.delete/{messageId}")
        public void deleteMessage(
                        @DestinationVariable String messageId, @Payload String deleteType) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                if (!DeleteType.isValid(deleteType)) {
                        throw new ValidationException("Invalid delete type");
                }

                Map<String, Object> response = chatWebSocketService.deleteMessage(encryptToken, messageId, deleteType);

                if (DeleteType.EVERYONE.equals(deleteType)) {
                        messagingTemplate.convertAndSend("/topic/deletes/" + response.get("conversationId"),
                                        (Object) response);
                }
        }

        @MessageMapping("/chat.delete/batch")
        public void deleteMessages(
                        @Payload Map<String, Object> payload) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                List<String> messageIdList = (List<String>) payload.get("messageIdList");
                String deleteType = (String) payload.get("deleteType");

                if (!DeleteType.isValid(deleteType)) {
                        throw new ValidationException("Invalid delete type");
                }

                Map<String, Object> response = chatWebSocketService.deleteMessages(encryptToken, messageIdList,
                                deleteType);

                if (DeleteType.EVERYONE.equals(deleteType)) {
                        messagingTemplate.convertAndSend("/topic/deletes/" + response.get("conversationId"),
                                        (Object) response);
                }
        }
}