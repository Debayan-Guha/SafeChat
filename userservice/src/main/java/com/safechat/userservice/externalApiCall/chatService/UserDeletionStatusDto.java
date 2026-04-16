package com.safechat.userservice.externalApiCall.chatService;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserDeletionStatusDto {
    
    private String userId;
    private String status;
}
