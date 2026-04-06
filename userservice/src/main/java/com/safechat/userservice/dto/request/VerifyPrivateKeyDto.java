package com.safechat.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPrivateKeyDto {

    @NotBlank
    private String privateKey;
}
