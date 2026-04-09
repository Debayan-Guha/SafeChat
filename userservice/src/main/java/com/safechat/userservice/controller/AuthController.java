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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/userservice/auth")
@Tag(name = "Authentication", description = "APIs for user and admin authentication, login and logout operations")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "User login", description = "Authenticates a user using email/displayName and password. Returns encrypted JWT token on success.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "400", description = "Email or DisplayName is required", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "401", description = "Incorrect password", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
    })
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

    @Operation(summary = "User logout", description = "Invalidates the user's JWT token. Token will no longer be accepted for authenticated requests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormatter<Void>> logout() {

        String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
        authService.logout(token);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseFormatter.formatter(
                        HttpStatus.OK.value(),
                        ApiMessage.USER_LOGOUT_SUCCESS));
    }

    @Operation(summary = "Admin login", description = "Authenticates an admin using email and password. Returns encrypted JWT token with ADMIN role on success.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin login successful", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "400", description = "Email is required", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "401", description = "Incorrect password", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
            @ApiResponse(responseCode = "404", description = "Admin not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
    })
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

    @Operation(summary = "Admin logout", description = "Invalidates the admin's JWT token. Token will no longer be accepted for admin authenticated requests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin logout successful", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
    })
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