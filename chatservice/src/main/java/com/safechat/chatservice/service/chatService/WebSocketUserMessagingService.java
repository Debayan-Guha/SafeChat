package com.safechat.chatservice.service.chatService;

import com.safechat.chatservice.config.WebSocketSessionRegistry;
import com.safechat.chatservice.dto.response.MessageResponseDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketUserMessagingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    public WebSocketUserMessagingService(SimpMessagingTemplate messagingTemplate,
                                         WebSocketSessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Send a message to a specific user.
     * Uses /user/{userId}/queue/messages — only User B receives it.
     */
    public void sendToUser(String targetUserId, MessageResponseDto message) {
        if (sessionRegistry.isUserActive(targetUserId)) {
            messagingTemplate.convertAndSendToUser(
                targetUserId,
                "/queue/messages",   // client subscribes to /user/queue/messages
                message
            );
        }
        // If not active, you can persist for later delivery here
    }

    /**
     * Send a delivery/read receipt to a specific user.
     */
    public void sendDeliveryUpdate(String targetUserId, Map<String, Object> payload) {
        if (sessionRegistry.isUserActive(targetUserId)) {
            messagingTemplate.convertAndSendToUser(
                targetUserId,
                "/queue/delivery",
                payload
            );
        }
    }

    /**
     * Send typing indicator to a specific user.
     */
    public void sendTypingIndicator(String targetUserId, Map<String, Object> payload) {
        if (sessionRegistry.isUserActive(targetUserId)) {
            messagingTemplate.convertAndSendToUser(
                targetUserId,
                "/queue/typing",
                payload
            );
        }
    }

    /**
     * Broadcast to ALL users in a conversation (your existing pattern).
     * Keep this for group/topic messages.
     */
    public void broadcastToConversation(String conversationId, Object payload) {
        messagingTemplate.convertAndSend("/topic/messages/" + conversationId, payload);
    }

    public boolean isUserOnline(String userId) {
        return sessionRegistry.isUserActive(userId);
    }
}