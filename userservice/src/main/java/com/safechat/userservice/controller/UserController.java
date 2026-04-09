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

        @GetMapping("/check-displayname")
        public ResponseEntity<ApiResponseFormatter<Void>> checkDisplayNameExists(
                        @RequestParam String displayName) throws NotFoundException, AlreadyExistsException {

                String decodedDisplayName = UrlEncoderUtil.decode(displayName);
                userReadService.isDisplayNameExists(decodedDisplayName);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                "No user exists with this Display Name"));
        }

        @GetMapping("/check-email")
        public ResponseEntity<ApiResponseFormatter<Void>> checkEmailExists(
                        @RequestParam String email) throws NotFoundException, AlreadyExistsException {

                String decodedEmail = UrlEncoderUtil.decode(email);
                userReadService.isEmailExists(decodedEmail);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.EMAIL_NOT_REGISTERED));
        }

        @GetMapping("/{userId}")
        public ResponseEntity<ApiResponseFormatter<UserResponseDto>> getUserById(
                        @PathVariable String userId)
                        throws NotFoundException {

                String decodedUserId = UrlEncoderUtil.decode(userId);
                UserResponseDto response = userReadService.getUserById(decodedUserId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_FOUND,
                                response));
        }

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

        @GetMapping("/search")
        public ResponseEntity<ApiResponseFormatter<List<UserResponseDto>>> searchUsers(
                        @RequestParam String displayName,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

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

        // ==================== KEY MANAGEMENT ====================

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

        // ==================== ACCOUNT DELETION ====================

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

        @PostMapping("/otp/{email}/send")
        public ResponseEntity<ApiResponseFormatter<Void>> sendOtp(@PathVariable String email,
                        @RequestParam String otpType) throws NotFoundException {

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