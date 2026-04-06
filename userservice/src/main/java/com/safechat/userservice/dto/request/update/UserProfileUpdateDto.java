package com.safechat.userservice.dto.request.update;

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
public class UserProfileUpdateDto {
    
    private String userName;
    private String displayName;
    private String email;
    private int otp;//for email update
    private String oldPassword;
    private String newPassword;
    private String publicKey;
    private String privateKey;
}
