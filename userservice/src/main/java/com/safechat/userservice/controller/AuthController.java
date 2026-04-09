package com.safechat.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safechat.userservice.dto.request.AdminLoginDto;
import com.safechat.userservice.dto.request.UserLoginDto;
import com.safechat.userservice.exception.ApplicationException.CredentialMisMatchException;
import com.safechat.userservice.exception.ApplicationException.NotFoundException;
import com.safechat.userservice.service.authService.AuthService;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.api.ApiResponseFormatter;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseFormatter<String>> login(@Valid @RequestBody UserLoginDto credentials)
            throws NotFoundException, CredentialMisMatchException {

        String encryptedToken = authService.tokenCreation(credentials);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseFormatter.formatter(
                        HttpStatus.OK.value(),
                        ApiMessage.USER_TOKEN_SUCCESS,
                        encryptedToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormatter<Void>> logout() {

        String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
        authService.logout(token);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseFormatter.formatter(
                        HttpStatus.OK.value(),
                        ApiMessage.USER_LOGOUT_SUCCESS));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponseFormatter<String>> adminLogin(@Valid @RequestBody AdminLoginDto credentials)
            throws NotFoundException, CredentialMisMatchException {

        String encryptedToken = authService.adminTokenCreation(credentials);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseFormatter.formatter(
                        HttpStatus.OK.value(),
                        "Admin token created successfully",
                        encryptedToken));
    }

    @PostMapping("/admin/logout")
    public ResponseEntity<ApiResponseFormatter<Void>> adminLogout() {

        String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
        authService.adminLogout(token);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseFormatter.formatter(
                        HttpStatus.OK.value(),
                        "Admin logout successfully"));
    }

}