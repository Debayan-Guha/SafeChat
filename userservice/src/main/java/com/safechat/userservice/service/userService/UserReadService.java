package com.safechat.userservice.service.userService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.safechat.userservice.utility.encryption.Pbkdf2Encoder;

@Service
public class UserReadService {

        private final String SERVICE_NAME = "UserReadService";
        private static final Logger log = LoggerFactory.getLogger(UserReadService.class);

        private final UserDbService userDbService;
        private final AesEncryption aesEncryption;
        private final Pbkdf2Encoder pbkdf2Encoder;
        private final JwtUtils jwtUtils;

        public UserReadService(UserDbService userDbService, AesEncryption aesEncryption, JwtUtils jwtUtils,
                        Pbkdf2Encoder pbkdf2Encoder) {
                this.userDbService = userDbService;
                this.aesEncryption = aesEncryption;
                this.jwtUtils = jwtUtils;
                this.pbkdf2Encoder = pbkdf2Encoder;
        }

        public void isDisplayNameExists(String displayName) throws NotFoundException, AlreadyExistsException {
                final String METHOD_NAME = "isDisplayNameExists";

                log.debug("{} - Checking displayName: {}", METHOD_NAME, displayName);

                Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("displayName"), displayName);

                boolean exists = OperationExecutor.dbGet(
                                () -> userDbService.exists(spec),
                                SERVICE_NAME, METHOD_NAME);

                log.debug("{} - DB result, exists: {}", METHOD_NAME, exists);

                if (exists) {
                        log.warn("{} - DisplayName already taken: {}", METHOD_NAME, displayName);
                        throw new AlreadyExistsException("User exists with this Display Name");
                }

                log.debug("{} - DisplayName is available: {}", METHOD_NAME, displayName);
        }

        public void isEmailExists(String email) throws NotFoundException, AlreadyExistsException {
                final String METHOD_NAME = "isEmailExists";

                log.debug("{} - Checking email: {}", METHOD_NAME, email);

                Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("email"), email.toLowerCase());

                boolean exists = OperationExecutor.dbGet(
                                () -> userDbService.exists(spec),
                                SERVICE_NAME, METHOD_NAME);

                log.debug("{} - DB result, exists: {}", METHOD_NAME, exists);

                if (exists) {
                        log.warn("{} - Email already registered: {}", METHOD_NAME, email);
                        throw new AlreadyExistsException("Email is already registered");
                }

                log.debug("{} - Email is available: {}", METHOD_NAME, email);
        }

        public UserResponseDto getMyProfile(String encryptToken) throws NotFoundException {
                final String METHOD_NAME = "getMyProfile";

                log.debug("{} - Method entered, token prefix: {}...", METHOD_NAME,
                                encryptToken.substring(0, Math.min(10, encryptToken.length())));

                log.debug("{} - Decrypting token", METHOD_NAME);
                String decryptToken = aesEncryption.decrypt(encryptToken);
                log.debug("{} - Token decrypted, extracting userId from JWT", METHOD_NAME);

                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
                log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

                Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("id"), userId);

                log.debug("{} - Querying DB for userId: {}", METHOD_NAME, userId);

                UserEntity userEntity = OperationExecutor
                                .dbGet(() -> userDbService.getUser(spec), SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> {
                                        log.error("{} - User not found in DB for userId: {}", METHOD_NAME, userId);
                                        return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                                });

                log.debug("{} - User found, mapping to DTO", METHOD_NAME);

                UserResponseDto response = OperationExecutor.map(
                                () -> UserToDto.convert(userEntity), SERVICE_NAME, METHOD_NAME);

                log.info("{} - Profile served for userId: {}", METHOD_NAME, userId);

                return response;
        }

        public Map<String, Object> searchUsers(String displayName, int page, int size) throws NotFoundException {
                final String METHOD_NAME = "searchUsers";

                log.debug("{} - displayName: {}, page: {}, size: {}", METHOD_NAME, displayName, page, size);

                Pageable pageable = PageRequest.of(page - 1, size);

                Specification<UserEntity> spec = (root, query, cb) -> cb.equal(cb.lower(root.get("displayName")),
                                displayName.toLowerCase());

                log.debug("{} - Querying DB with case-insensitive displayName: {}", METHOD_NAME,
                                displayName.toLowerCase());

                Page<UserEntity> userPage = OperationExecutor.dbGet(
                                () -> userDbService.getUsers(spec, pageable),
                                SERVICE_NAME, METHOD_NAME);

                log.debug("{} - DB result -> totalElements: {}, totalPages: {}, currentPageElements: {}",
                                METHOD_NAME, userPage.getTotalElements(), userPage.getTotalPages(),
                                userPage.getNumberOfElements());

                if (userPage.isEmpty()) {
                        log.warn("{} - No users found for displayName: {}", METHOD_NAME, displayName);
                        throw new NotFoundException(ApiMessage.USER_NOT_FOUND);
                }

                log.debug("{} - Mapping {} user(s) to DTO", METHOD_NAME, userPage.getNumberOfElements());

                List<UserResponseDto> data = userPage.getContent().stream()
                                .map(user -> UserResponseDto.builder()
                                                .id(user.getId())
                                                .displayName(user.getDisplayName())
                                                .build())
                                .collect(Collectors.toList());

                PaginationData pagination = PaginationData.builder()
                                .totalPages(userPage.getTotalPages())
                                .totalElements(userPage.getTotalElements())
                                .currentPageTotalElements(userPage.getNumberOfElements())
                                .currentPage(page)
                                .build();

                log.info("{} - Search completed for displayName: {}, totalElements: {}, currentPage: {}",
                                METHOD_NAME, displayName, userPage.getTotalElements(), page);

                Map<String, Object> result = new HashMap<>();
                result.put("data", data);
                result.put("pagination", pagination);

                return result;
        }

        public UserResponseDto getUserById(String userId) throws NotFoundException {
                final String METHOD_NAME = "getUserById";

                log.debug("{} - Fetching user for userId: {}", METHOD_NAME, userId);

                Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("id"), userId);

                UserEntity userEntity = OperationExecutor
                                .dbGet(() -> userDbService.getUser(spec), SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> {
                                        log.error("{} - User not found for userId: {}", METHOD_NAME, userId);
                                        return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                                });

                log.debug("{} - User found -> displayName: {}, publicKey present: {}",
                                METHOD_NAME, userEntity.getDisplayName(), userEntity.getPublicKey() != null);

                UserResponseDto response = UserResponseDto.builder()
                                .displayName(userEntity.getDisplayName())
                                .publicKey(userEntity.getPublicKey())
                                .build();

                log.info("{} - Public key served for userId: {}", METHOD_NAME, userId);

                return response;
        }

        public void verifyPrivateKey(String encryptToken, String privateKey)
                        throws NotFoundException, ValidationException {
                final String METHOD_NAME = "verifyPrivateKey";

                log.info("{} - Private key verification initiated, token prefix: {}...", METHOD_NAME,
                                encryptToken.substring(0, Math.min(10, encryptToken.length())));

                log.debug("{} - Decrypting token", METHOD_NAME);
                String decryptToken = aesEncryption.decrypt(encryptToken);
                log.debug("{} - Token decrypted, extracting userId", METHOD_NAME);

                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
                log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

                Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

                log.debug("{} - Querying DB for userId: {}", METHOD_NAME, userId);

                UserEntity userEntity = OperationExecutor
                                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> {
                                        log.error("{} - User not found during key verification for userId: {}",
                                                        METHOD_NAME, userId);
                                        return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                                });

                log.debug("{} - User found, matching private key hash for userId: {}", METHOD_NAME, userId);

                if (!pbkdf2Encoder.pbkd2Encoder().matches(privateKey, userEntity.getEncryptedPrivateKey())) {
                        log.warn("{} - Private key mismatch for userId: {}", METHOD_NAME, userId);
                        throw new ValidationException("Invalid private key");
                }

                log.info("{} - Private key verified successfully for userId: {}", METHOD_NAME, userId);
        }

        public Map<String, Object> getUsersByAdmin(int page, int size, String status) {
                final String METHOD_NAME = "getUsersByAdmin";

                log.debug("{} - page: {}, size: {}, status: {}", METHOD_NAME, page, size, status);

                Pageable pageable = PageRequest.of(page - 1, size);

                Specification<UserEntity> spec = (root, query, cb) -> {
                        if (status != null && !status.isBlank()) {
                                log.debug("{} - Applying status filter: {}", METHOD_NAME, status);
                                return cb.equal(root.get("status"), status);
                        }
                        log.debug("{} - No status filter, fetching all users", METHOD_NAME);
                        return cb.conjunction();
                };

                log.debug("{} - Querying DB", METHOD_NAME);

                Page<UserEntity> userPage = OperationExecutor.dbGet(
                                () -> userDbService.getUsers(spec, pageable),
                                SERVICE_NAME, METHOD_NAME);

                log.debug("{} - DB result -> totalElements: {}, totalPages: {}, currentPageElements: {}",
                                METHOD_NAME, userPage.getTotalElements(), userPage.getTotalPages(),
                                userPage.getNumberOfElements());

                if (userPage.isEmpty()) {
                        log.warn("{} - No users found for status: {}", METHOD_NAME, status);
                }

                List<UserResponseDto> data = userPage.getContent().stream()
                                .map(UserToDto::convert)
                                .collect(Collectors.toList());

                PaginationData pagination = PaginationData.builder()
                                .totalPages(userPage.getTotalPages())
                                .totalElements(userPage.getTotalElements())
                                .currentPageTotalElements(data.size())
                                .currentPage(page)
                                .build();

                log.info("{} - Admin user list served -> totalElements: {}, currentPage: {}, statusFilter: {}",
                                METHOD_NAME, userPage.getTotalElements(), page, status);

                Map<String, Object> result = new HashMap<>();
                result.put("data", data);
                result.put("pagination", pagination);

                return result;
        }

        public UserResponseDto getUserByIdByAdmin(String userId) throws NotFoundException {
                final String METHOD_NAME = "getUserByIdByAdmin";

                log.debug("{} - Admin fetching userId: {}", METHOD_NAME, userId);

                Specification<UserEntity> spec = (root, query, cb) -> cb.equal(root.get("id"), userId);

                log.debug("{} - Querying DB for userId: {}", METHOD_NAME, userId);

                UserEntity userEntity = OperationExecutor
                                .dbGet(() -> userDbService.getUser(spec), SERVICE_NAME, METHOD_NAME)
                                .orElseThrow(() -> {
                                        log.error("{} - User not found for userId: {}", METHOD_NAME, userId);
                                        return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                                });

                log.debug("{} - User found -> displayName: {}, mapping full DTO for admin",
                                METHOD_NAME, userEntity.getDisplayName());

                UserResponseDto response = OperationExecutor.map(
                                () -> UserToDto.convert(userEntity), SERVICE_NAME, METHOD_NAME);

                log.info("{} - Admin fetched full profile for userId: {}", METHOD_NAME, userId);

                return response;
        }
}