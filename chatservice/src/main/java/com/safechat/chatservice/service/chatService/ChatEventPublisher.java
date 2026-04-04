package com.safechat.chatservice.service.chatService;

import com.safechat.chatservice.document.ConversationDocument;
import com.safechat.chatservice.dto.response.ConversationMesssageResponseDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import com.safechat.chatservice.service.dbService.ConversationDbService;
import com.safechat.chatservice.utility.OperationExecutor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChatEventPublisher {

        private final String SERVICE_NAME = "ChatEventPublisher";

        private final SimpMessagingTemplate messagingTemplate;
        private final ConversationDbService conversationDbService;

        public ChatEventPublisher(SimpMessagingTemplate messagingTemplate,
                        ConversationDbService conversationDbService) {
                this.messagingTemplate = messagingTemplate;
                this.conversationDbService = conversationDbService;
        }

        private Set<String> fetchParticipants(String conversationId) {
                final String METHOD_NAME = "fetchParticipants";

                Query query = new Query();
                query.addCriteria(Criteria.where("id").is(conversationId));

                return OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(query),
                                SERVICE_NAME, METHOD_NAME)
                                .map(ConversationDocument::getParticipants)
                                .orElse(Set.of());
        }

        public void publishMessage(MessageResponseDto response) {
                // For users inside the open conversation
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + response.getConversationId(), response);

                // For all participants except sender — sidebar last msg preview + unread badge
                fetchParticipants(response.getConversationId())
                                .stream()
                                .filter(userId -> !userId.equals(response.getSenderId()))
                                .forEach(userId -> messagingTemplate.convertAndSend("/topic/users/notify/" + userId,
                                                response));
        }

        public void publishPrivacyMessage(MessageResponseDto response) {
                // Only inside open conversation — no sidebar notify, no persistence trace
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + response.getConversationId(), response);
        }

        public void publishDeliveryStatus(MessageResponseDto response) {
                // Only inside open chat — sidebar doesn't care about ticks
                messagingTemplate.convertAndSend(
                                "/topic/delivery/" + response.getConversationId(), response);
        }

        public void publishTypingStatus(Map<String, Object> typingStatus) {
                // Only inside open chat — sidebar doesn't care about typing
                String conversationId = (String) typingStatus.get("conversationId");
                messagingTemplate.convertAndSend(
                                "/topic/typing/" + conversationId, (Boolean) typingStatus.get("isTyping"));
        }

        public void publishMessageEdit(MessageResponseDto response) {
                // Inside open chat — update message bubble
                messagingTemplate.convertAndSend(
                                "/topic/messages/" + response.getConversationId(), response);

                // For all participants except sender — sidebar preview update if last message
                fetchParticipants(response.getConversationId())
                                .stream()
                                .filter(userId -> !userId.equals(response.getSenderId()))
                                .forEach(userId -> messagingTemplate.convertAndSend("/topic/users/notify/" + userId,
                                                response));
        }

        @SuppressWarnings("unchecked")
        public void publishMessageDeletion(Map<String, Object> deletionResponse) {
                String conversationId = (String) deletionResponse.get("conversationId");
                String senderId = (String) deletionResponse.get("senderId");

                Object payload = deletionResponse.containsKey("messageId")
                                ? (String) deletionResponse.get("messageId")
                                : (List<String>) deletionResponse.get("messageIds");

                // Inside open chat — remove message bubble
                messagingTemplate.convertAndSend("/topic/deletes/messages/" + conversationId, payload);

                // For all participants except sender — sidebar update only when EVERYONE
                // DELETE_FOR_ME is personal, others should not know
                fetchParticipants(conversationId)
                                .stream()
                                .filter(userId -> !userId.equals(senderId))
                                .forEach(userId -> messagingTemplate.convertAndSend("/topic/users/notify/" + userId,
                                                (Object) deletionResponse));

        }

        public void publishConversationCreated(ConversationMesssageResponseDto response) {
                // Notify all participants via personal topic — client gets conversationId from
                // here
                // then dynamically subscribes to /topic/messages/{conversationId}
                response.getConversationResponseDto().getParticipants()
                                .forEach(userId -> messagingTemplate.convertAndSend(
                                                "/topic/users/notify/" + userId, response));
        }

        @SuppressWarnings("unchecked")
        public void publishConversationDeleted(Map<String, Object> deletionResponse) {
                String conversationId = (String) deletionResponse.get("conversationId");
                String senderId = (String) deletionResponse.get("senderId");

                Object payload = deletionResponse.containsKey("conversationId")
                                ? (String) deletionResponse.get("conversationId")
                                : (List<String>) deletionResponse.get("conversationIds");

                // Inside open chat — close/remove conversation view
                messagingTemplate.convertAndSend("/topic/deletes/conversations/" + conversationId, payload);

                fetchParticipants(conversationId)
                                .stream()
                                .filter(userId -> !userId.equals(senderId))
                                .forEach(userId -> messagingTemplate.convertAndSend(
                                                "/topic/users/notify/" + userId, (Object) deletionResponse));
        }

}