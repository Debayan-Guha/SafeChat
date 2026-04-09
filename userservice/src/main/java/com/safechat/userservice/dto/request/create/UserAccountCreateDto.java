package com.safechat.userservice.dto.request.create;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    
    @NotBlank
    private String userName;

    @NotBlank
    private String displayName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String publicKey;

    @NotBlank
    private String privateKey;

    @Digits(integer = 6, fraction = 0, message = "OTP must be 6 digits")
    private int otp;
}
