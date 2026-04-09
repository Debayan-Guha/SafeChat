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
import com.safechat.userservice.utility.Enumeration.Status;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.api.ApiResponseFormatter;
import com.safechat.userservice.utility.api.PaginationData;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

        private final UserReadService userReadService;
        private final UserWriteService userWriteService;
        private final AdminService adminService;

        public AdminController(UserReadService userReadService, UserWriteService userWriteService,AdminService adminService) {
                this.userReadService = userReadService;
                this.userWriteService = userWriteService;
                this.adminService=adminService;
        }

        /**
         * Create new admin account
         * POST /api/v1/admin/accounts
         * Body: { name, email, password, description }
         */
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

        /**
         * Update admin account
         * PUT /api/v1/admin/accounts/{adminId}
         * Body: { name, description, password }
         */
        @PutMapping("/accounts/{adminId}")
        public ResponseEntity<ApiResponseFormatter<Void>> updateAdmin(
                        @PathVariable String adminId,
                        @RequestBody @Valid AdminUpdateDto requestDto)
                        throws NotFoundException {

                adminService.updateAdmin(adminId, requestDto);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.ADMIN_UPDATED));
        }

        // ==================== BLOCK MANAGEMENT ====================

        /**
         * Block user
         * POST /api/v1/admin/users/block/{userIdToBlock}
         */
        @PostMapping("/users/block/{userIdToBlock}")
        public ResponseEntity<ApiResponseFormatter<Void>> blockUser(
                        @PathVariable String userIdToBlock)
                        throws NotFoundException, AlreadyExistsException {

                userWriteService.blockUserByAdmin(userIdToBlock);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_BLOCKED));
        }

        /**
         * Unblock user
         * DELETE /api/v1/admin/users/block/{userIdToUnblock}
         */
        @DeleteMapping("/users/block/{userIdToUnblock}")
        public ResponseEntity<ApiResponseFormatter<Void>> unblockUser(
                        @PathVariable String userIdToUnblock)
                        throws NotFoundException {

                userWriteService.unblockUserByAdmin(userIdToUnblock);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_UNBLOCKED));
        }

        // ==================== USER MANAGEMENT ====================

        /**
         * Get all users (admin only)
         * GET /api/v1/admin/users?page=0&size=20&status=ACTIVE
         */
        @GetMapping("/users")
        public ResponseEntity<ApiResponseFormatter<List<UserResponseDto>>> getUsers(
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) String status) {

                if (page < 0 || size <= 0) {
                        throw new ValidationException(ApiMessage.PAGE_VALIDATION_ERROR);
                }

                if (!Status.isValid(status)) {
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

        /**
         * Get user by ID (admin only)
         * GET /api/v1/admin/users/{userId}
         */
        @GetMapping("/users/{userId}")
        public ResponseEntity<ApiResponseFormatter<UserResponseDto>> getUserById(
                        @PathVariable String userId)
                        throws NotFoundException {

                UserResponseDto response = userReadService.getUserByIdByAdmin(userId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_FOUND,
                                response));
        }

        /**
         * Delete user (admin only - hard delete)
         * DELETE /api/v1/admin/users/{userId}
         */
        @DeleteMapping("/users/{userId}")
        public ResponseEntity<ApiResponseFormatter<Void>> deleteUser(
                        @PathVariable String userId)
                        throws NotFoundException {

                userWriteService.deleteUserByAdmin(userId);

                return ResponseEntity.ok(ApiResponseFormatter.formatter(
                                HttpStatus.OK.value(),
                                ApiMessage.USER_DELETED));
        }
}