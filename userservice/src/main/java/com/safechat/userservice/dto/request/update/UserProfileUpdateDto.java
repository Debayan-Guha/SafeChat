package com.safechat.userservice.dto.request.update;

import jakarta.validation.Valid;
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
public class UserProfileUpdateDto {

    private String userName;
    private String displayName;

    @Valid
    private EmailUpdate emailUpdate;

    @Valid
    private PasswordUpdate passwordUpdate;

    // Inner class for email update
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailUpdate {
        @NotBlank(message = "Email cannot be blank")
        @Email
        private String email;

        @Digits(integer = 6, fraction = 0, message = "OTP must be 6 digits")
        private int otp;
    }

    // Inner class for password update
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordUpdate {
        @NotBlank(message = "Old password is required")
        private String oldPassword;

        @NotBlank(message = "New password is required")
        private String newPassword;
    }

}