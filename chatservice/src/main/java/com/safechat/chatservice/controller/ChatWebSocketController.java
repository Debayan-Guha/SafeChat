package com.safechat.chatservice.controller;

import com.safechat.chatservice.dto.request.create.ConversationMesssageCreateRequestDto;
import com.safechat.chatservice.dto.request.create.MessageCreateRequestDto;
import com.safechat.chatservice.dto.request.update.MessageUpdateRequestDto;
import com.safechat.chatservice.dto.response.ConversationMesssageResponseDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import com.safechat.chatservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.chatservice.exception.ApplicationException.NotFoundException;
import com.safechat.chatservice.exception.ApplicationException.ValidationException;
import com.safechat.chatservice.kafka.ChatKafkaProducer;
import com.safechat.chatservice.service.chatService.ChatWebSocketService;
import com.safechat.chatservice.utility.Enumeration.DeleteType;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class ChatWebSocketController {

        private final ChatWebSocketService chatWebSocketService;
        private final ChatKafkaProducer chatKafkaProducer;

        public ChatWebSocketController(ChatWebSocketService chatWebSocketService,
                        ChatKafkaProducer chatKafkaProducer) {
                this.chatWebSocketService = chatWebSocketService;
                this.chatKafkaProducer = chatKafkaProducer;
        }

        @MessageMapping("/chat.conversation.create")
        public void createConversation(@Payload @Valid ConversationMesssageCreateRequestDto requestDto)
                        throws NotFoundException, AlreadyExistsException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                ConversationMesssageResponseDto response = chatWebSocketService.createConversation(encryptToken,
                                requestDto);

                chatKafkaProducer.publishConversationCreated(response);
        }

        @MessageMapping("/chat.message.normal.send/{conversationId}")
        public void sendMessage(
                        @DestinationVariable String conversationId,
                        @Payload @Valid MessageCreateRequestDto requestDto) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.sendMessage(encryptToken, conversationId,
                                requestDto);

                chatKafkaProducer.publishMessage(response);
        }

        @MessageMapping("/chat.message.privacy.send/{conversationId}")
        public void sendPrivacyMessage(
                        @DestinationVariable String conversationId,
                        @Payload @Valid MessageCreateRequestDto requestDto) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.sendPrivacyMessage(encryptToken, conversationId,
                                requestDto);

                chatKafkaProducer.publishPrivacyMessage(response);
        }

        @MessageMapping("/chat.message.delivered/{messageId}")
        public void markAsDelivered(
                        @DestinationVariable String messageId) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.markMessageAsDelivered(encryptToken, messageId);

                chatKafkaProducer.publishDeliveryStatus(response);
        }

        @MessageMapping("/chat.message.read/{messageId}")
        public void markAsRead(
                        @DestinationVariable String messageId) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.markMessageAsRead(encryptToken, messageId);

                chatKafkaProducer.publishDeliveryStatus(response);
        }

        @MessageMapping("/chat.message.typing/{conversationId}")
        public void typingIndicator(
                        @DestinationVariable String conversationId,
                        @Payload Boolean isTyping) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                Map<String, Object> response = chatWebSocketService.typingIndicator(encryptToken, conversationId,
                                isTyping);

                chatKafkaProducer.publishTypingStatus(response);
        }

        @MessageMapping("/chat.message.edit/{messageId}")
        public void editMessage(
                        @DestinationVariable String messageId,
                        @Payload @Valid MessageUpdateRequestDto requestDto) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                MessageResponseDto response = chatWebSocketService.editMessage(encryptToken, messageId,
                                requestDto.getEncryptedMessages());

                chatKafkaProducer.publishMessageEdit(response);
        }

        @MessageMapping("/chat.message.delete/{messageId}")
        public void deleteMessage(
                        @DestinationVariable String messageId, @Payload String deleteType) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                if (!DeleteType.isValid(deleteType)) {
                        throw new ValidationException("Invalid delete type");
                }

                Map<String, Object> response = chatWebSocketService.deleteMessage(encryptToken, messageId, deleteType);

                if (DeleteType.EVERYONE.equals(deleteType)) {

                        chatKafkaProducer.publishMessageDeletion(response);
                }
        }

        @SuppressWarnings("unchecked")
        @MessageMapping("/chat.message.delete/batch")
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

                        chatKafkaProducer.publishMessageDeletion(response);
                }
        }

        @MessageMapping("/chat.conversation.delete/{conversationId}")
        public void deleteConversation(
                        @DestinationVariable String conversationId,
                        @Payload String deleteType) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                if (!DeleteType.isValid(deleteType)) {
                        throw new ValidationException("Invalid delete type");
                }

                Map<String, Object> response = chatWebSocketService.deleteConversation(encryptToken, conversationId,
                                deleteType);

                if (DeleteType.EVERYONE.equals(deleteType)) {

                        chatKafkaProducer.publishConversationDeleted(response);
                }

        }

        @SuppressWarnings("unchecked")
        @MessageMapping("/chat.conversation.delete/batch")
        public void deleteConversations(
                        @Payload Map<String, Object> payload) throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                List<String> conversationIdList = (List<String>) payload.get("conversationIdList");
                String deleteType = (String) payload.get("deleteType");

                if (!DeleteType.isValid(deleteType)) {
                        throw new ValidationException("Invalid delete type");
                }

                Map<String, Object> response = chatWebSocketService.deleteConversations(encryptToken,
                                conversationIdList,
                                deleteType);

                if (DeleteType.EVERYONE.equals(deleteType)) {
                        chatKafkaProducer.publishConversationDeleted(response);
                }
        }
}