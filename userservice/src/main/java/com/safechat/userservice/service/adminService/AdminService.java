package com.safechat.userservice.service.adminService;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.dto.request.create.AdminCreateDto;
import com.safechat.userservice.dto.request.update.AdminUpdateDto;
import com.safechat.userservice.entity.AdminEntity;
import com.safechat.userservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.userservice.exception.ApplicationException.NotFoundException;
import com.safechat.userservice.exception.ApplicationException.ValidationException;
import com.safechat.userservice.service.dbService.AdminDbService;
import com.safechat.userservice.utility.OperationExecutor;
import com.safechat.userservice.utility.encryption.BcryptEncoder;

import jakarta.transaction.Transactional;

@Service
public class AdminService {

    private final String SERVICE_NAME = "AdminService";
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final AdminDbService adminDbService;
    private final BcryptEncoder bcryptEncoder;

    public AdminService(AdminDbService adminDbService, BcryptEncoder bcryptEncoder) {
        this.adminDbService = adminDbService;
        this.bcryptEncoder = bcryptEncoder;
    }

    @Transactional
    public void createAdmin(AdminCreateDto requestDto) throws AlreadyExistsException {
        final String METHOD_NAME = "createAdmin";

        log.debug("{} - Checking if admin exists for email: {}", METHOD_NAME, requestDto.getEmail().toLowerCase());

        // Check if admin already exists with same email
        Specification<AdminEntity> emailSpec = (root, query, cb) -> cb.equal(root.get("email"),
                requestDto.getEmail().toLowerCase());

        boolean exists = OperationExecutor.dbGet(
                () -> adminDbService.exists(emailSpec),
                SERVICE_NAME, METHOD_NAME);

        log.debug("{} - DB result, exists: {}", METHOD_NAME, exists);

        if (exists) {
            log.warn("{} - Admin already exists with email: {}", METHOD_NAME, requestDto.getEmail());
            throw new AlreadyExistsException("Admin already exists with email: " + requestDto.getEmail());
        }

        // Create new admin entity
        AdminEntity admin = AdminEntity.builder()
                .id("ADM_" + UUID.randomUUID().toString())
                .name(requestDto.getName())
                .email(requestDto.getEmail().toLowerCase())
                .password(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getPassword()))
                .description(requestDto.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.debug("{} - Saving new admin to DB, email: {}", METHOD_NAME, requestDto.getEmail().toLowerCase());

        OperationExecutor.dbSave(() -> adminDbService.save(admin), SERVICE_NAME, METHOD_NAME);

        log.info("{} - Admin created successfully for email: {}", METHOD_NAME, requestDto.getEmail().toLowerCase());
    }

    @Transactional
    public void updateAdmin(String adminId, AdminUpdateDto requestDto) throws NotFoundException, ValidationException {
        final String METHOD_NAME = "updateAdmin";

        log.debug("{} - Querying DB for adminId: {}", METHOD_NAME, adminId);

        Specification<AdminEntity> getAdminById = (root, query, cb) -> cb.equal(root.get("id"), adminId);

        AdminEntity admin = OperationExecutor
                .dbGet(() -> adminDbService.getAdmin(getAdminById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> {
                    log.error("{} - Admin not found for adminId: {}", METHOD_NAME, adminId);
                    return new NotFoundException("Admin not found with ID: " + adminId);
                });

        // Update name if provided
        if (requestDto.getName() != null && !requestDto.getName().isBlank()) {
            admin.setName(requestDto.getName());
            log.debug("{} - Name updated for adminId: {}", METHOD_NAME, adminId);
        }

        // Update description if provided
        if (requestDto.getDescription() != null && !requestDto.getDescription().isBlank()) {
            admin.setDescription(requestDto.getDescription());
            log.debug("{} - Description updated for adminId: {}", METHOD_NAME, adminId);
        }

        // Update password if provided
        if (requestDto.getPassword() != null && !requestDto.getPassword().isBlank()) {
            admin.setPassword(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getPassword()));
            log.debug("{} - Password updated for adminId: {}", METHOD_NAME, adminId);
        }

        log.debug("{} - Saving updated admin to DB, adminId: {}", METHOD_NAME, adminId);

        OperationExecutor.dbSave(() -> adminDbService.save(admin), SERVICE_NAME, METHOD_NAME);

        log.info("{} - Admin updated successfully for adminId: {}", METHOD_NAME, adminId);
    }
}