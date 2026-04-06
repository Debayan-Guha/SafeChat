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

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

        private final UserReadService userReadService;
        private final UserWriteService userWriteService;

        public UserController(UserReadService userReadService, UserWriteService userWriteService) {
                this.userReadService = userReadService;
                this.userWriteService = userWriteService;
        }

        // ==================== ACCOUNT MANAGEMENT ====================

        /**
         * Create new user account
         * POST /api/v1/users/account
         * Body: { phone/email, password, displayName, publicKey }
         * 
         * @throws NotFoundException
         */
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

        /**
         * Check if display name already exists
         * GET /api/v1/users/check-displayname?displayName=john_doe
         * @throws AlreadyExistsException 
         * @throws NotFoundException 
         */
        @GetMapping("/check-displayname")
        public ResponseEntity<ApiResponseFormatter<Void>> checkDisplayNameExists(
                        @RequestParam String displayName) throws NotFoundException, AlreadyExistsException {

                userReadService.isDisplayNameExists(displayName);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                "No user exists with this Display Name"));
        }

        /**
         * Check if email already exists
         * @throws AlreadyExistsException 
         * @throws NotFoundException 
         */
        @GetMapping("/check-email")
        public ResponseEntity<ApiResponseFormatter<Void>> checkEmailExists(
                        @RequestParam String email) throws NotFoundException, AlreadyExistsException {

                userReadService.isEmailExists(email);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.EMAIL_NOT_REGISTERED));
        }

        /**
         * Update user account
         * PUT /api/v1/users/account
         * Body: { displayName, avatar, bio, status }
         * 
         * @throws AlreadyExistsException
         */
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

        /**
         * Get my profile
         * GET /api/v1/users/me
         */
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

       

        /**
         * Search users by phone/email/displayName
         * GET /api/v1/users/search?query=john&page=0&size=20
         */
        @GetMapping("/search")
        public ResponseEntity<ApiResponseFormatter<List<UserResponseDto>>> searchUsers(
                        @RequestParam String displayName,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

                if (page < 0 || size <= 0) {
                        throw new ValidationException(ApiMessage.PAGE_VALIDATION_ERROR);
                }

                Map<String, Object> result = userReadService.searchUsers(displayName, page, size);

                List<UserResponseDto> data = (List<UserResponseDto>) result.get("data");
                PaginationData pagination = (PaginationData) result.get("pagination");

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_FOUND,
                                data,
                                pagination));
        }

        // ==================== KEY MANAGEMENT ====================

        // ==================== PRIVATE KEY VERIFICATION ====================

        /**
         * Verify private key
         * POST /api/v1/users/verify-private-key
         * Body: { privateKey }
         */
        @PostMapping("/keys/verify/private-key")
        public ResponseEntity<ApiResponseFormatter<Void>> verifyPrivateKey(
                        @RequestBody @Valid VerifyPrivateKeyDto requestDto)
                        throws NotFoundException, ValidationException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                userWriteService.verifyPrivateKey(encryptToken, requestDto.getPrivateKey());

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                "Private key verified successfully"));
        }

        /**
         * Update public key
         * PUT /api/v1/users/keys/public
         * Body: { publicKey, signedPreKey, oneTimePreKeys }
         */
        @PutMapping("/keys")
        public ResponseEntity<ApiResponseFormatter<Void>> updatePublicKey(
                        @RequestBody UserProfileUpdateDto requestDto)
                        throws NotFoundException {

                String encryptToken = (String) SecurityContextHolder.getContext()
                                .getAuthentication().getCredentials();

                userWriteService.updateKeys(encryptToken, requestDto);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.PUBLIC_KEY_UPDATED));
        }

        /**
         * Get user's public key
         * GET /api/v1/users/{userId}/public-key
         */
        @GetMapping("/keys/{userId}/public-key")
        public ResponseEntity<ApiResponseFormatter<String>> getPublicKey(
                        @PathVariable String userId)
                        throws NotFoundException {

                String response = userReadService.getPublicKey(userId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.PUBLIC_KEY_FOUND,
                                response));
        }

        // ==================== ACCOUNT DELETION ====================

        /**
         * Request account deletion (GDPR style)
         * POST /api/v1/users/account/delete-request
         * Body: { reason, password }
         */
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

        /**
         * Confirm account deletion (with OTP)
         * DELETE /api/v1/users/account/confirm?otp=123456
         */
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

        /**
         * Cancel deletion request
         * POST /api/v1/users/account/delete-cancel
         */
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

        // ==================== SEND OTP ====================

        /**
         * Send OTP to user's contact (email/phone)
         * POST /api/v1/users/otp/send
         * Body: { contact }
         * 
         * @throws NotFoundException
         */
        @PostMapping("/otp/{email}/send")
        public ResponseEntity<ApiResponseFormatter<Void>> sendOtp(@PathVariable String email,
                        @RequestParam String otpType) throws NotFoundException {

                if (!OtpType.isValid(otpType)) {
                        throw new ValidationException("Otp type mismatch");
                }
                userWriteService.sendOtp(email, otpType);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                "OTP sent successfully to " + email));
        }

}