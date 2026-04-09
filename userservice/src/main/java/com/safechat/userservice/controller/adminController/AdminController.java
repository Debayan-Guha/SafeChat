package com.safechat.userservice.controller.adminController;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.safechat.userservice.dto.request.create.AdminCreateDto;
import com.safechat.userservice.dto.request.update.AdminUpdateDto;
import com.safechat.userservice.dto.response.UserResponseDto;
import com.safechat.userservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.userservice.exception.ApplicationException.NotFoundException;
import com.safechat.userservice.exception.ApplicationException.ValidationException;
import com.safechat.userservice.service.adminService.AdminService;
import com.safechat.userservice.service.userService.UserReadService;
import com.safechat.userservice.service.userService.UserWriteService;
import com.safechat.userservice.utility.UrlEncoderUtil;
import com.safechat.userservice.utility.Enumeration.Status;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.api.ApiResponseFormatter;
import com.safechat.userservice.utility.api.PaginationData;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/userservice/admin")
@Tag(name = "Admin Management", description = "APIs for admin operations including user management, block/unblock, and admin account management")
public class AdminController {

        private final UserReadService userReadService;
        private final UserWriteService userWriteService;
        private final AdminService adminService;

        public AdminController(UserReadService userReadService, UserWriteService userWriteService,
                        AdminService adminService) {
                this.userReadService = userReadService;
                this.userWriteService = userWriteService;
                this.adminService = adminService;
        }

        @Operation(summary = "Create admin account", description = "Creates a new admin account. Only existing admins can perform this operation.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Admin account created", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "409", description = "Admin already exists with this email", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/accounts")
        public ResponseEntity<ApiResponseFormatter<Void>> createAdmin(
                        @RequestBody @Valid AdminCreateDto requestDto)
                        throws AlreadyExistsException {

                adminService.createAdmin(requestDto);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseFormatter.formatter(
                                                HttpStatus.CREATED.value(),
                                                ApiMessage.ADMIN_CREATED));
        }

        @Operation(summary = "Update admin account", description = "Updates an existing admin account. Only existing admins can perform this operation.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Admin account updated", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "Admin not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PutMapping("/accounts/{adminId}")
        public ResponseEntity<ApiResponseFormatter<Void>> updateAdmin(
                        @Parameter(description = "Admin ID", required = true) @PathVariable String adminId,
                        @RequestBody @Valid AdminUpdateDto requestDto)
                        throws NotFoundException {

                String decodedAdminId = UrlEncoderUtil.decode(adminId);
                adminService.updateAdmin(decodedAdminId, requestDto);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.ADMIN_UPDATED));
        }

        @Operation(summary = "Block user", description = "Blocks a user account. Blocked users cannot log in or use the application.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User blocked", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "409", description = "User already blocked", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @PostMapping("/users/block/{userIdToBlock}")
        public ResponseEntity<ApiResponseFormatter<Void>> blockUser(
                        @Parameter(description = "ID of the user to block", required = true) @PathVariable String userIdToBlock)
                        throws NotFoundException, AlreadyExistsException {

                String decodedUserId = UrlEncoderUtil.decode(userIdToBlock);
                userWriteService.blockUserByAdmin(decodedUserId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_BLOCKED));
        }

        @Operation(summary = "Unblock user", description = "Unblocks a previously blocked user account.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User unblocked", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "User is not blocked", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @DeleteMapping("/users/block/{userIdToUnblock}")
        public ResponseEntity<ApiResponseFormatter<Void>> unblockUser(
                        @Parameter(description = "ID of the user to unblock", required = true) @PathVariable String userIdToUnblock)
                        throws NotFoundException {

                String decodedUserId = UrlEncoderUtil.decode(userIdToUnblock);
                userWriteService.unblockUserByAdmin(decodedUserId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_UNBLOCKED));
        }

        @Operation(summary = "Get all users", description = "Retrieves a paginated list of all users. Can filter by status.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Users retrieved", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or status", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/users")
        public ResponseEntity<ApiResponseFormatter<List<UserResponseDto>>> getUsers(
                        @Parameter(description = "Page number (1-indexed)", example = "1") @RequestParam(defaultValue = "1") int page,
                        @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size,
                        @Parameter(description = "Filter by user status (ACTIVE, BLOCK)") @RequestParam(required = false) String status) {

                if (page < 0 || size <= 0) {
                        throw new ValidationException(ApiMessage.PAGE_VALIDATION_ERROR);
                }

                if (status != null && !Status.isValid(status)) {
                        throw new ValidationException("Invalid status");
                }

                Map<String, Object> result = userReadService.getUsersByAdmin(page, size, status);

                List<UserResponseDto> data = (List<UserResponseDto>) result.get("data");
                PaginationData pagination = (PaginationData) result.get("pagination");

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_FOUND,
                                data,
                                pagination));
        }

        @Operation(summary = "Get user by ID", description = "Retrieves detailed user information including email, status, and timestamps.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @GetMapping("/users/{userId}")
        public ResponseEntity<ApiResponseFormatter<UserResponseDto>> getUserById(
                        @Parameter(description = "User ID", required = true) @PathVariable String userId)
                        throws NotFoundException {

                String decodedUserId = UrlEncoderUtil.decode(userId);
                UserResponseDto response = userReadService.getUserByIdByAdmin(decodedUserId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_FOUND,
                                response));
        }

        @Operation(summary = "Delete user", description = "Permanently deletes a user account. This action is irreversible.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User deleted", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class))),
                        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponseFormatter.class)))
        })
        @DeleteMapping("/users/{userId}")
        public ResponseEntity<ApiResponseFormatter<Void>> deleteUser(
                        @Parameter(description = "User ID", required = true) @PathVariable String userId)
                        throws NotFoundException {

                String decodedUserId = UrlEncoderUtil.decode(userId);
                userWriteService.deleteUserByAdmin(decodedUserId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_DELETED));
        }
}