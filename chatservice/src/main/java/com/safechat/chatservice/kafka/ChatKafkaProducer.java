package com.safechat.chatservice.kafka;

import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.safechat.chatservice.dto.response.ConversationMesssageResponseDto;
import com.safechat.chatservice.dto.response.MessageResponseDto;

@Service
public class ChatKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ChatKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishMessage(MessageResponseDto response) {
        kafkaTemplate.send("chat.message.send", response.getConversationId(), response);
    }

    public void publishPrivacyMessage(MessageResponseDto response) {
        kafkaTemplate.send("chat.message.privacy.send", response.getConversationId(), response);
    }

    public void publishMessageEdit(MessageResponseDto response) {
        kafkaTemplate.send("chat.message.edit", response.getConversationId(), response);
    }

    public void publishMessageDeletion(Map<String, Object> response) {
        kafkaTemplate.send("chat.message.delete", (String) response.get("conversationId"), response);
    }

    public void publishDeliveryStatus(MessageResponseDto response) {
        kafkaTemplate.send("chat.message.delivery", response.getConversationId(), response);
    }

    public void publishConversationCreated(ConversationMesssageResponseDto response) {
        kafkaTemplate.send("chat.conversation.create",
                response.getConversationResponseDto().getId(), response);
    }

    public void publishConversationDeleted(Map<String, Object> response) {
        kafkaTemplate.send("chat.conversation.delete", (String) response.get("conversationId"), response);
    }

    public void publishTypingStatus(Map<String, Object> response) {
    kafkaTemplate.send("chat.typing", (String) response.get("conversationId"), response);
}
}