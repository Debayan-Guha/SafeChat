package com.safechat.userservice.service.userService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.dto.response.UserResponseDto;
import com.safechat.userservice.entity.UserEntity;
import com.safechat.userservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.userservice.exception.ApplicationException.NotFoundException;
import com.safechat.userservice.exception.ApplicationException.ValidationException;
import com.safechat.userservice.jwt.JwtUtils;
import com.safechat.userservice.mapper.toDto.UserToDto;
import com.safechat.userservice.service.dbService.UserDbService;
import com.safechat.userservice.utility.OperationExecutor;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.api.PaginationData;
import com.safechat.userservice.utility.encryption.AesEncryption;
import com.safechat.userservice.utility.encryption.BcryptEncoder;

@Service
public class UserReadService {

    private final String SERVICE_NAME = "UserReadService";

    private final UserDbService userDbService;
    private final AesEncryption aesEncryption;
    private final BcryptEncoder bcryptEncoder;
    private final JwtUtils jwtUtils;

    public UserReadService(UserDbService userDbService, AesEncryption aesEncryption, JwtUtils jwtUtils,BcryptEncoder bcryptEncoder) {
        this.userDbService = userDbService;
        this.aesEncryption = aesEncryption;
        this.jwtUtils = jwtUtils;
        this.bcryptEncoder=bcryptEncoder;
    }

    public void isDisplayNameExists(String displayName) throws NotFoundException, AlreadyExistsException {
        final String METHOD_NAME = "isDisplayNameExists";

        Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("displayName"), displayName);

        boolean exists = OperationExecutor.dbGet(
                () -> userDbService.exists(spec),
                SERVICE_NAME, METHOD_NAME);

        if (exists) {
            throw new AlreadyExistsException("User exists with this Display Name");
        }
    }

    public void isEmailExists(String email) throws NotFoundException, AlreadyExistsException {
        final String METHOD_NAME = "isEmailExists";

        Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("email"), email.toLowerCase());

        boolean exists = OperationExecutor.dbGet(
                () -> userDbService.exists(spec),
                SERVICE_NAME, METHOD_NAME);

        if (exists) {
            throw new AlreadyExistsException("Email is already registered");
        }
    }

    public UserResponseDto getMyProfile(String encryptToken) throws NotFoundException {
        final String METHOD_NAME = "getMyProfile";

        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

        Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(spec), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        return OperationExecutor.map(() -> UserToDto.convert(userEntity), SERVICE_NAME, METHOD_NAME);
    }

    public Map<String, Object> searchUsers(String displayName, int page, int size) {
        final String METHOD_NAME = "searchUsers";

        Pageable pageable = PageRequest.of(page - 1, size);

        Specification<UserEntity> spec = (root, query, cb) -> cb.equal(cb.lower(root.get("displayName")),
                displayName.toLowerCase());

        Page<UserEntity> userPage = OperationExecutor.dbGet(
                () -> userDbService.getUsers(spec, pageable),
                SERVICE_NAME, METHOD_NAME);

        List<UserResponseDto> data = userPage.getContent().stream()
                .map(UserToDto::convert)
                .collect(Collectors.toList());

        PaginationData pagination = PaginationData.builder()
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .currentPageTotalElements(data.size())
                .currentPage(page)
                .build();

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("pagination", pagination);

        return result;
    }

    public UserResponseDto getUserById(String userId) throws NotFoundException {
        final String METHOD_NAME = "getUserById";

        Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(spec), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        return UserResponseDto.builder().displayName(userEntity.getDisplayName()).publicKey(userEntity.getPublicKey())
                .build();
    }

    public void verifyPrivateKey(String encryptToken, String privateKey)
            throws NotFoundException, ValidationException {
        final String METHOD_NAME = "verifyPrivateKey";

        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        // Verify private key matches stored hash
        if (!bcryptEncoder.bCryptPasswordEncoder().matches(privateKey, userEntity.getEncryptedPrivateKey())) {
            throw new ValidationException("Invalid private key");
        }
    }
}