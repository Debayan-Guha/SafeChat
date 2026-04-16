package com.safechat.chatservice.externalApiCall;

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
