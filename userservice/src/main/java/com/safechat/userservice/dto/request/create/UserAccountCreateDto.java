package com.safechat.userservice.dto.request.create;

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
public class UserAccountCreateDto {
    
    private String userName;
    private String displayName;
    private String email;
    private String password;
    private String publicKey;
    private String privateKey;
    private int otp;
}
