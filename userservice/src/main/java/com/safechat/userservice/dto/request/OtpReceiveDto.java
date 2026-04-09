package com.safechat.userservice.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpReceiveDto {

    @NotBlank
    private String email;

    @Digits(integer = 6, fraction = 0, message = "OTP must be 6 digits")
    private int otp;
}
