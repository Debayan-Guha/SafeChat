package com.safechat.chatservice.dto.request.create;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageCreateRequestDto {

    private String conversationId;

    @NotNull
    private Map<@NotBlank String,@NotBlank String> encryptedMessages;

    private Integer expirySeconds;
}
