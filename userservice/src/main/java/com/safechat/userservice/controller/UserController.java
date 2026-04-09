package com.safechat.userservice.controller;

import com.safechat.userservice.dto.request.OtpReceiveDto;
import com.safechat.userservice.dto.request.VerifyPrivateKeyDto;
import com.safechat.userservice.dto.request.create.UserAccountCreateDto;
import com.safechat.userservice.dto.request.update.UserProfileUpdateDto;
import com.safechat.userservice.dto.response.*;
import com.safechat.userservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.userservice.exception.ApplicationException.NotFoundException;
import com.safechat.userservice.exception.ApplicationException.ValidationException;
import com.safechat.userservice.service.userService.UserReadService;
import com.safechat.userservice.service.userService.UserWriteService;
import com.safechat.userservice.utility.UrlEncoderUtil;
import com.safechat.userservice.utility.Enumeration.OtpType;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.api.ApiResponseFormatter;
import com.safechat.userservice.utility.api.PaginationData;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/userservice/users")
@Tag(name = "User Management", description = "APIs for user account management, profile operations, key management, and account deletion")
public class UserController {

        private final UserReadService userReadService;
        private final UserWriteService userWriteService;

        public UserController(UserReadService userReadService, UserWriteService userWriteService) {
                this.userReadService = userReadService;
                this.userWriteService = userWriteService;
        }

        @Operation(summary = "Create new user account", description = "Creates a new user account. Requires OTP verification. User must first request OTP via /otp/{email}/send endpoint with ACCOUNT_CREATION type.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Validation failed or OTP mismatch", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "OTP expired or not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "409", description = "Email or Display name already exists", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/account")
        public ResponseEntity<ApiResponseFormatter<Void>> createAccount(
                        @RequestBody @Valid UserAccountCreateDto requestDto)
                        throws AlreadyExistsException, NotFoundException {

                userWriteService.createAccount(requestDto);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseFormatter.formatter(
                                                HttpStatus.CREATED.value(),
                                                ApiMessage.ACCOUNT_CREATED));
        }

        @Operation(summary = "Check display name availability", description = "Checks if a display name is already taken by another user. Returns 200 if available, 409 if already exists.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Display name available", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "409", description = "Display name already exists", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/check-displayname")
        public ResponseEntity<ApiResponseFormatter<Void>> checkDisplayNameExists(
                        @Parameter(description = "Display name to check", required = true) @RequestParam String displayName)
                        throws NotFoundException, AlreadyExistsException {

                String decodedDisplayName = UrlEncoderUtil.decode(displayName);
                userReadService.isDisplayNameExists(decodedDisplayName);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                "No user exists with this Display Name"));
        }

        @Operation(summary = "Check email availability", description = "Checks if an email is already registered. Returns 200 if available, 409 if already registered.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Email available", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/check-email")
        public ResponseEntity<ApiResponseFormatter<Void>> checkEmailExists(
                        @Parameter(description = "Email to check", required = true) @RequestParam String email)
                        throws NotFoundException, AlreadyExistsException {

                String decodedEmail = UrlEncoderUtil.decode(email);
                userReadService.isEmailExists(decodedEmail);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.EMAIL_NOT_REGISTERED));
        }

        @Operation(summary = "Get user by ID", description = "Retrieves user information by user ID. Returns public user data including display name and public key.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/{userId}")
        public ResponseEntity<ApiResponseFormatter<UserResponseDto>> getUserById(
                        @Parameter(description = "User ID", required = true) @PathVariable String userId)
                        throws NotFoundException {

                String decodedUserId = UrlEncoderUtil.decode(userId);
                UserResponseDto response = userReadService.getUserById(decodedUserId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_FOUND,
                                response));
        }

        @Operation(summary = "Update user profile", description = "Updates user profile information. Supports partial updates. Only provided fields will be updated.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Validation failed or OTP mismatch for email update", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "409", description = "Email or Display name already exists", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PatchMapping("/profile")
        public ResponseEntity<ApiResponseFormatter<UserResponseDto>> updateProfile(
                        @RequestBody @Valid UserProfileUpdateDto requestDto)
                        throws NotFoundException, AlreadyExistsException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                UserResponseDto response = userWriteService.updateProfile(encryptToken, requestDto);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.PROFILE_UPDATED,
                                response));
        }

        @Operation(summary = "Get my profile", description = "Retrieves the authenticated user's complete profile information including email, status, and keys.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Profile found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/profile")
        public ResponseEntity<ApiResponseFormatter<UserResponseDto>> getMyProfile()
                        throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                UserResponseDto response = userReadService.getMyProfile(encryptToken);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.PROFILE_FOUND,
                                response));
        }

        @Operation(summary = "Search users", description = "Searches users by display name with pagination. Returns exact match results.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Search completed", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/search")
        public ResponseEntity<ApiResponseFormatter<List<UserResponseDto>>> searchUsers(
                        @Parameter(description = "Display name to search", required = true, example = "john_doe") @RequestParam String displayName,
                        @Parameter(description = "Page number (0-indexed)", example = "1") @RequestParam(defaultValue = "1") int page,
                        @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size) {

                if (page < 0 || size <= 0) {
                        throw new ValidationException(ApiMessage.PAGE_VALIDATION_ERROR);
                }

                String decodedDisplayName = UrlEncoderUtil.decode(displayName);
                Map<String, Object> result = userReadService.searchUsers(decodedDisplayName, page, size);

                List<UserResponseDto> data = (List<UserResponseDto>) result.get("data");
                PaginationData pagination = (PaginationData) result.get("pagination");

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_FOUND,
                                data,
                                pagination));
        }

        @Operation(summary = "Verify private key", description = "Verifies if the provided private key matches the stored hash. Used for authentication before sensitive operations.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Private key verified", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid private key", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/keys/verify/private-key")
        public ResponseEntity<ApiResponseFormatter<Void>> verifyPrivateKey(
                        @RequestBody @Valid VerifyPrivateKeyDto requestDto)
                        throws NotFoundException, ValidationException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                userReadService.verifyPrivateKey(encryptToken, requestDto.getPrivateKey());

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                "Private key verified successfully"));
        }

        @Operation(summary = "Request account deletion", description = "Initiates account deletion request with 24-hour grace period. User can cancel within 24 hours. OTP verification required.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Deletion request submitted", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid OTP", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User or OTP not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/account/delete-request")
        public ResponseEntity<ApiResponseFormatter<Void>> requestAccountDeletion(
                        @RequestBody @Valid OtpReceiveDto requestDto)
                        throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                userWriteService.requestAccountDeletion(encryptToken, requestDto);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.DELETION_REQUEST_SUBMITTED));
        }

        @Operation(summary = "Instant account deletion", description = "Permanently deletes account immediately after OTP verification. This action is irreversible.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Account deleted", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid OTP", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User or OTP not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/account/delete-instant")
        public ResponseEntity<ApiResponseFormatter<Void>> instantAccountDeletion(
                        @RequestBody @Valid OtpReceiveDto requestDto)
                        throws NotFoundException, ValidationException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                userWriteService.instantAccountDeletion(encryptToken, requestDto);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.ACCOUNT_DELETED));
        }

        @Operation(summary = "Cancel deletion request", description = "Cancels the scheduled account deletion request. Only applicable for requests made via /account/delete-request.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Deletion request cancelled", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "No deletion request found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/account/delete-cancel")
        public ResponseEntity<ApiResponseFormatter<Void>> cancelDeletionRequest()
                        throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                userWriteService.cancelDeletionRequest(encryptToken);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.DELETION_REQUEST_CANCELLED));
        }

        @Operation(summary = "Send OTP", description = "Sends One-Time Password to user's email for verification. OTP type determines which email template to use.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "OTP sent successfully", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid OTP type", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "Email not registered (for non-creation types)", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/otp/{email}/send")
        public ResponseEntity<ApiResponseFormatter<Void>> sendOtp(
                        @Parameter(description = "Email address", required = true) @PathVariable String email,
                        @Parameter(description = "OTP type: ACCOUNT_CREATION, ACCOUNT_DELETION_REQUEST, ACCOUNT_DELETION_INSTANT, PASSWORD_RESET, ACCOUNT_UPDATION", required = true) @RequestParam String otpType)
                        throws NotFoundException {

                String decodedEmail = UrlEncoderUtil.decode(email);

                if (!OtpType.isValid(otpType)) {
                        throw new ValidationException("Otp type mismatch");
                }
                userWriteService.sendOtp(decodedEmail, otpType);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                "OTP sent successfully to " + decodedEmail));
        }
}