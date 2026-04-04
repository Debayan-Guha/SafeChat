package com.safechat.chatservice.dto.request.create;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationMesssageCreateRequestDto {

    @NotNull
    @Valid
    private ConversationCreate conversationCreate;

    @NotNull
    @Valid
    private MessageCreate messageCreate;

    @Getter
    @Setter
    public static class ConversationCreate {

        @NotBlank
        private String creatorId;

        @NotNull
        private List<String> participantsId;
    }

    @Getter
    @Setter
    public static class MessageCreate {

        @NotBlank
        private String senderId;

        @NotBlank
        private String encryptedMessage;

        private Integer expirySeconds;
    }
}
