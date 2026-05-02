package com.safechat.userservice.service.authService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.dto.request.AdminLoginDto;
import com.safechat.userservice.dto.request.UserLoginDto;
import com.safechat.userservice.entity.AdminEntity;
import com.safechat.userservice.entity.UserEntity;
import com.safechat.userservice.exception.ApplicationException.CredentialMisMatchException;
import com.safechat.userservice.exception.ApplicationException.NotFoundException;
import com.safechat.userservice.exception.ApplicationException.ValidationException;
import com.safechat.userservice.jwt.JwtUtils;
import com.safechat.userservice.service.CachedService;
import com.safechat.userservice.service.dbService.AdminDbService;
import com.safechat.userservice.service.dbService.UserDbService;
import com.safechat.userservice.utility.OperationExecutor;
import com.safechat.userservice.utility.Enumeration.Status;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.encryption.AesEncryption;
import com.safechat.userservice.utility.encryption.BcryptEncoder;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import jakarta.persistence.criteria.Predicate;

@Service
public class AuthService {

    private final String SERVICE_NAME = "AuthService";
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDbService userDbService;
    private final AdminDbService adminDbService;
    private final AesEncryption aesEncryption;
    private final BcryptEncoder bcryptEncoder;
    private final JwtUtils jwtUtils;
    private final CachedService cachedService;

    public AuthService(UserDbService userDbService, AdminDbService adminDbService,
            AesEncryption aesEncryption,
            BcryptEncoder bcryptEncoder,
            JwtUtils jwtUtils,
            CachedService cachedService) {
        this.userDbService = userDbService;
        this.adminDbService = adminDbService;
        this.aesEncryption = aesEncryption;
        this.bcryptEncoder = bcryptEncoder;
        this.jwtUtils = jwtUtils;
        this.cachedService = cachedService;
    }

    public String tokenCreation(UserLoginDto credentials) throws NotFoundException, CredentialMisMatchException {

        final String METHOD_NAME = "tokenCreation";
        final Function<String, String> cacheKeyBuilder = (userId) -> String.format("user:auth:token:jti:uid:%s",
                userId);

        log.debug("{} - Login attempt, email: {}, displayName: {}", METHOD_NAME,
                credentials.getEmail(), credentials.getDisplayName());

        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (credentials.getEmail() != null && !credentials.getEmail().isBlank()) {
                predicates.add(cb.equal(root.get("email"), credentials.getEmail().toLowerCase()));
            } else if (credentials.getDisplayName() != null && !credentials.getDisplayName().isBlank()) {
                predicates.add(cb.equal(root.get("displayName"), credentials.getDisplayName()));
            } else {
                throw new ValidationException("Email or DisplayName is required");
            }

            predicates.add(cb.equal(root.get("status"), Status.ACTIVE));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        log.debug("{} - Querying DB for active user", METHOD_NAME);

        Optional<UserEntity> userEntity = OperationExecutor.dbGet(() -> userDbService.getUser(spec), SERVICE_NAME,
                METHOD_NAME);

        if (!userEntity.isPresent()) {
            log.warn("{} - User not found or not active for email: {}", METHOD_NAME, credentials.getEmail());
            throw new NotFoundException(ApiMessage.USER_NOT_FOUND);
        }

        log.debug("{} - User found, verifying password for userId: {}", METHOD_NAME, userEntity.get().getId());

        if (!bcryptEncoder.bCryptPasswordEncoder().matches(credentials.getPassword(), userEntity.get().getPassword())) {
            log.warn("{} - Incorrect password for userId: {}", METHOD_NAME, userEntity.get().getId());
            throw new CredentialMisMatchException("Incorrect password");
        }

        // Check for existing JTI in cache
        log.debug("{} - Checking existing JTI in cache for userId: {}", METHOD_NAME, userEntity.get().getId());

        Optional<String> cachedJti = Optional.ofNullable(OperationExecutor.redisGet(
                () -> cachedService.getFromCache(cacheKeyBuilder.apply(userEntity.get().getId()), String.class),
                SERVICE_NAME, METHOD_NAME));

        String jti = cachedJti.orElseGet(() -> UUID.randomUUID().toString());

        log.debug("{} - JTI {} for userId: {}", METHOD_NAME,
                cachedJti.isPresent() ? "reused from cache" : "generated new", userEntity.get().getId());

        // Generate token (no expiry - valid until logout)
        String token = jwtUtils.generateToken(userEntity.get().getId(), userEntity.get().getDisplayName(), "USER", jti);

        if (!cachedJti.isPresent()) {
            OperationExecutor.redisSave(
                    () -> cachedService.saveResponse(cacheKeyBuilder.apply(userEntity.get().getId()), jti,
                            Duration.ofDays(365)),
                    SERVICE_NAME, METHOD_NAME);
            log.debug("{} - JTI saved to cache for userId: {}", METHOD_NAME, userEntity.get().getId());
        }

        log.info("{} - Login successful for userId: {}", METHOD_NAME, userEntity.get().getId());

        return aesEncryption.encrypt(token);
    }

    @Transactional
    public void logout(String encryptedToken) {
        final String METHOD_NAME = "logout";
        final Function<String, String> cacheKeyBuilder = (userId) -> String.format("user:auth:token:jti:uid:%s",
                userId);

        log.debug("{} - Logout initiated", METHOD_NAME);

        try {
            String decryptToken = aesEncryption.decrypt(encryptedToken);
            Claims claims = jwtUtils.extractAllClaims(decryptToken);
            String userId = (String) claims.get("uid");

            log.debug("{} - Extracted userId: {}, removing JTI from cache", METHOD_NAME, userId);

            // Remove JTI from Redis (invalidates token)
            OperationExecutor.redisRemove(
                    () -> cachedService.deleteCacheByKey(cacheKeyBuilder.apply(userId)),
                    SERVICE_NAME,
                    METHOD_NAME);

            log.info("{} - Logout successful for userId: {}", METHOD_NAME, userId);

        } catch (Exception e) {
            // Token might be invalid, still proceed with logout
            log.warn("{} - Logout attempted with invalid or unreadable token", METHOD_NAME);
        }
    }

    public String adminTokenCreation(AdminLoginDto credentials) throws NotFoundException, CredentialMisMatchException {
        final String METHOD_NAME = "adminTokenCreation";
        final Function<String, String> cacheKeyBuilder = (adminId) -> String.format("user:auth:token:jti:admin:uid:%s",
                adminId);// aid=admin id

        log.debug("{} - Admin login attempt for email: {}", METHOD_NAME, credentials.getEmail());

        Specification<AdminEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("email"), credentials.getEmail().toLowerCase()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        log.debug("{} - Querying DB for admin", METHOD_NAME);

        Optional<AdminEntity> adminEntity = OperationExecutor.dbGet(() -> adminDbService.getAdmin(spec), SERVICE_NAME,
                METHOD_NAME);

        if (!adminEntity.isPresent()) {
            log.warn("{} - Admin not found for email: {}", METHOD_NAME, credentials.getEmail());
            throw new NotFoundException("Admin not found ");
        }

        log.debug("{} - Admin found, verifying password for adminId: {}", METHOD_NAME, adminEntity.get().getId());

        if (!bcryptEncoder.bCryptPasswordEncoder().matches(credentials.getPassword(),
                adminEntity.get().getPassword())) {
            log.warn("{} - Incorrect password for adminId: {}", METHOD_NAME, adminEntity.get().getId());
            throw new CredentialMisMatchException("Incorrect password");
        }

        // Check for existing JTI in cache
        log.debug("{} - Checking existing JTI in cache for adminId: {}", METHOD_NAME, adminEntity.get().getId());

        Optional<String> cachedJti = Optional.ofNullable(OperationExecutor.redisGet(
                () -> cachedService.getFromCache(cacheKeyBuilder.apply(adminEntity.get().getId()), String.class),
                SERVICE_NAME, METHOD_NAME));

        String jti = cachedJti.orElseGet(() -> UUID.randomUUID().toString());

        log.debug("{} - JTI {} for adminId: {}", METHOD_NAME,
                cachedJti.isPresent() ? "reused from cache" : "generated new", adminEntity.get().getId());

        // Generate token for admin
        String token = jwtUtils.generateToken(adminEntity.get().getId(), adminEntity.get().getName(), "ADMIN", jti);

        if (!cachedJti.isPresent()) {
            OperationExecutor.redisSave(
                    () -> cachedService.saveResponse(cacheKeyBuilder.apply(adminEntity.get().getId()), jti,
                            Duration.ofDays(365)),
                    SERVICE_NAME, METHOD_NAME);
            log.debug("{} - JTI saved to cache for adminId: {}", METHOD_NAME, adminEntity.get().getId());
        }

        log.info("{} - Admin login successful for adminId: {}", METHOD_NAME, adminEntity.get().getId());

        return aesEncryption.encrypt(token);
    }

    @Transactional
    public void adminLogout(String encryptedToken) {
        final String METHOD_NAME = "adminLogout";
        final Function<String, String> cacheKeyBuilder = (adminId) -> String.format("user:auth:token:jti:admin:uid:%s",
                adminId);

        log.debug("{} - Admin logout initiated", METHOD_NAME);

        try {
            String decryptToken = aesEncryption.decrypt(encryptedToken);
            Claims claims = jwtUtils.extractAllClaims(decryptToken);
            String adminId = (String) claims.get("uid");

            log.debug("{} - Extracted adminId: {}, removing JTI from cache", METHOD_NAME, adminId);

            // Remove JTI from Redis (invalidates token)
            OperationExecutor.redisRemove(
                    () -> cachedService.deleteCacheByKey(cacheKeyBuilder.apply(adminId)),
                    SERVICE_NAME,
                    METHOD_NAME);

            log.info("{} - Admin logout successful for adminId: {}", METHOD_NAME, adminId);

        } catch (Exception e) {
            // Token might be invalid, still proceed with logout
            log.warn("{} - Admin logout attempted with invalid or unreadable token", METHOD_NAME);
        }
    }

    public boolean verifyAndValidateToken(String encryptToken) {
        final String METHOD_NAME = "verifyAndValidateToken";

        log.debug("{} - Token verification initiated", METHOD_NAME);

        String decryptToken = aesEncryption.decrypt(encryptToken);

        if (!jwtUtils.tokenVerification(decryptToken)) {
            log.warn("{} - Token failed JWT verification", METHOD_NAME);
            return false;
        }

        Claims claims = jwtUtils.extractAllClaims(decryptToken);
        String userId = (String) claims.get("uid");
        String jti = (String) claims.get("jti");
        String role = (String) claims.get("role");

        log.debug("{} - Token claims extracted, userId: {}, role: {}", METHOD_NAME, userId, role);

        // Use different cache key based on role
        String cacheKey;
        if ("ADMIN".equals(role)) {
            cacheKey = String.format("user:auth:token:jti:admin:uid:%s", userId);
        } else {
            cacheKey = String.format("user:auth:token:jti:uid:%s", userId);
        }

        log.debug("{} - Checking JTI in cache for userId: {}", METHOD_NAME, userId);

        String cachedJti = OperationExecutor.redisGet(
                () -> cachedService.getFromCache(cacheKey, String.class),
                SERVICE_NAME, METHOD_NAME);

        boolean isValid = cachedJti != null && cachedJti.equals(jti);

        log.debug("{} - Token validation result: {} for userId: {}", METHOD_NAME, isValid, userId);

        return isValid;
    }
}