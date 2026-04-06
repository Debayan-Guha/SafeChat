package com.safechat.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.safechat.userservice.dto.response.UserResponseDto;
import com.safechat.userservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.userservice.utility.api.ApiResponseFormatter;
import com.safechat.userservice.utility.api.PaginationData;

public class AdminController {
    
   
String later="""
        
            // ==================== BLOCK MANAGEMENT ====================

    /**
     * Block user
     * POST /api/v1/users/block/{userIdToBlock}
     */
    @PostMapping("/block/{userIdToBlock}")
    public ResponseEntity<ApiResponseFormatter<Void>> blockUser(
            @PathVariable String userIdToBlock)
            throws NotFoundException, AlreadyExistsException {

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();

        userWriteService.blockUser(userId, userIdToBlock);

        return ResponseEntity.ok(ApiResponseFormatter.formatter(
                HttpStatus.OK.value(),
                ApiMessage.USER_BLOCKED));
    }

    /**
     * Unblock user
     * DELETE /api/v1/users/block/{userIdToUnblock}
     */
    @DeleteMapping("/block/{userIdToUnblock}")
    public ResponseEntity<ApiResponseFormatter<Void>> unblockUser(
            @PathVariable String userIdToUnblock)
            throws NotFoundException {

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();

        userWriteService.unblockUser(userId, userIdToUnblock);

        return ResponseEntity.ok(ApiResponseFormatter.formatter(
                HttpStatus.OK.value(),
                ApiMessage.USER_UNBLOCKED));
    }

    /**
     * Get blocked users list
     * GET /api/v1/users/blocked?page=0&size=20
     */
    @GetMapping("/blocked")
    public ResponseEntity<ApiResponseFormatter<List<UserResponseDto>>> getBlockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size <= 0) {
            throw new ValidationException(ApiMessage.PAGE_VALIDATION_ERROR);
        }

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();

        Map<String, Object> result = userReadService.getBlockedUsers(userId, page, size);

        List<UserResponseDto> data = (List<UserResponseDto>) result.get("data");
        PaginationData pagination = (PaginationData) result.get("pagination");

        return ResponseEntity.ok(ApiResponseFormatter.formatter(
                HttpStatus.OK.value(),
                ApiMessage.BLOCKED_USERS_FOUND,
                data,
                pagination));
    }

    """;
}
