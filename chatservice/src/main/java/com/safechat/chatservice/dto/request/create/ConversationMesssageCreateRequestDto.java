package com.safechat.chatservice.dto.request.create;

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

        @NotNull
        private List<String> participantsId;
    }

    @Getter
    @Setter
    public static class MessageCreate {

        @NotBlank
        private String encryptedMessage;

        private Integer expirySeconds;
    }
}
