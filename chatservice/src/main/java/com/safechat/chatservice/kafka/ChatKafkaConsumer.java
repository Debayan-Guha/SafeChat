package com.safechat.chatservice.kafka;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.safechat.chatservice.dto.response.ConversationMesssageResponseDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import com.safechat.chatservice.service.chatService.ChatEventPublisher;

@Service
public class ChatKafkaConsumer {

    private final ChatEventPublisher chatEventPublisher;

    public ChatKafkaConsumer(ChatEventPublisher chatEventPublisher) {
        this.chatEventPublisher = chatEventPublisher;
    }

    @KafkaListener(topics = "chat.message.normal.send", groupId = "chat-service")
    public void onMessage(MessageResponseDto response) {
        chatEventPublisher.publishMessage(response);
    }

    @KafkaListener(topics = "chat.message.privacy.send", groupId = "chat-service")
    public void onPrivacyMessage(MessageResponseDto response) {
        chatEventPublisher.publishMessage(response);
    }

    @KafkaListener(topics = "chat.message.edit", groupId = "chat-service")
    public void onMessageEdit(MessageResponseDto response) {
        chatEventPublisher.publishMessageEdit(response);
    }

    @KafkaListener(topics = "chat.message.delete", groupId = "chat-service")
    public void onMessageDeletion(Map<String, Object> response) {
        chatEventPublisher.publishMessageDeletion(response);
    }

    @KafkaListener(topics = "chat.message.delivery", groupId = "chat-service")
    public void onDeliveryStatus(MessageResponseDto response) {
        chatEventPublisher.publishDeliveryStatus(response);
    }

    @KafkaListener(topics = "chat.conversation.create", groupId = "chat-service")
    public void onConversationCreated(ConversationMesssageResponseDto response) {
        chatEventPublisher.publishConversationCreated(response);
    }

    @KafkaListener(topics = "chat.conversation.delete", groupId = "chat-service")
    public void onConversationDeleted(Map<String, Object> response) {
        chatEventPublisher.publishConversationDeleted(response);
    }

    @KafkaListener(topics = "chat.typing", groupId = "chat-service")
    public void onTypingStatus(Map<String, Object> response) {
        chatEventPublisher.publishTypingStatus(response);
    }
}