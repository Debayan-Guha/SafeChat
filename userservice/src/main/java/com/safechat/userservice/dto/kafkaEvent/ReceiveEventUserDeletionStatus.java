package com.safechat.userservice.dto.kafkaEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiveEventUserDeletionStatus {
    
    private String userId;
    private String status;
}
