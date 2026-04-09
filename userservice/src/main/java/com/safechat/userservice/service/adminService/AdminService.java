package com.safechat.userservice.service.adminService;

import java.time.LocalDateTime;
import java.util.UUID;

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

    private final AdminDbService adminDbService;
    private final BcryptEncoder bcryptEncoder;

    public AdminService(AdminDbService adminDbService, BcryptEncoder bcryptEncoder) {
        this.adminDbService = adminDbService;
        this.bcryptEncoder = bcryptEncoder;
    }

    @Transactional
    public void createAdmin(AdminCreateDto requestDto) throws AlreadyExistsException {
        final String METHOD_NAME = "createAdmin";

        // Check if admin already exists with same email
        Specification<AdminEntity> emailSpec = (root, query, cb) -> cb.equal(root.get("email"),
                requestDto.getEmail().toLowerCase());

        boolean exists = OperationExecutor.dbGet(
                () -> adminDbService.exists(emailSpec),
                SERVICE_NAME, METHOD_NAME);

        if (exists) {
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

        OperationExecutor.dbSave(() -> adminDbService.save(admin), SERVICE_NAME, METHOD_NAME);
    }

    @Transactional
    public void updateAdmin(String adminId, AdminUpdateDto requestDto) throws NotFoundException, ValidationException {
        final String METHOD_NAME = "updateAdmin";

        Specification<AdminEntity> getAdminById = (root, query, cb) -> cb.equal(root.get("id"), adminId);

        AdminEntity admin = OperationExecutor
                .dbGet(() -> adminDbService.getAdmin(getAdminById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException("Admin not found with ID: " + adminId));

        // Update name if provided
        if (requestDto.getName() != null && !requestDto.getName().isBlank()) {
            admin.setName(requestDto.getName());
        }

        // Update description if provided
        if (requestDto.getDescription() != null && !requestDto.getDescription().isBlank()) {
            admin.setDescription(requestDto.getDescription());
        }

        // Update password if provided
        if (requestDto.getPassword() != null && !requestDto.getPassword().isBlank()) {
            admin.setPassword(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getPassword()));
        }

        OperationExecutor.dbSave(() -> adminDbService.save(admin), SERVICE_NAME, METHOD_NAME);
    }
}