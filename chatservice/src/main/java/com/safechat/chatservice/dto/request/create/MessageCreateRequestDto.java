package com.safechat.chatservice.dto.request.create;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageCreateRequestDto {

    private String conversationId;
    private String encryptedMessage;
    private Integer expirySeconds;
}
