package com.safechat.userservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpReceiveDto {
    
    private String email;
    private int otp;
}
