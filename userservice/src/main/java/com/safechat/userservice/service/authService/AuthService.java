package com.safechat.userservice.service.authService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

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

        Optional<UserEntity> userEntity = OperationExecutor.dbGet(() -> userDbService.getUser(spec), SERVICE_NAME,
                METHOD_NAME);

        if (!userEntity.isPresent()) {
            throw new NotFoundException(ApiMessage.USER_NOT_FOUND);
        }

        if (!bcryptEncoder.bCryptPasswordEncoder().matches(credentials.getPassword(), userEntity.get().getPassword())) {
            throw new CredentialMisMatchException("Incorrect password");
        }

        // Check for existing JTI in cache
        Optional<String> cachedJti = Optional.ofNullable(OperationExecutor.redisGet(
                () -> cachedService.getFromCache(cacheKeyBuilder.apply(userEntity.get().getId()), String.class),
                SERVICE_NAME, METHOD_NAME));

        String jti = cachedJti.orElseGet(() -> UUID.randomUUID().toString());

        // Generate token (no expiry - valid until logout)
        String token = jwtUtils.generateToken(userEntity.get().getId(), userEntity.get().getDisplayName(), "USER", jti);

        if (!cachedJti.isPresent()) {
            OperationExecutor.redisSave(
                    () -> cachedService.saveResponse(cacheKeyBuilder.apply(userEntity.get().getId()), jti,
                            Duration.ofDays(365)),
                    SERVICE_NAME, METHOD_NAME);
        }

        return aesEncryption.encrypt(token);
    }

    @Transactional
    public void logout(String encryptedToken) {
        final String METHOD_NAME = "logout";
        final Function<String, String> cacheKeyBuilder = (userId) -> String.format("user:auth:token:jti:uid:%s",
                userId);

        try {
            String decryptToken = aesEncryption.decrypt(encryptedToken);
            Claims claims = jwtUtils.extractAllClaims(decryptToken);
            String userId = (String) claims.get("uid");

            // Remove JTI from Redis (invalidates token)
            OperationExecutor.redisRemove(
                    () -> cachedService.deleteCacheByKey(cacheKeyBuilder.apply(userId)),
                    SERVICE_NAME,
                    METHOD_NAME);

        } catch (Exception e) {
            // Token might be invalid, still proceed with logout
        }
    }

    public String adminTokenCreation(AdminLoginDto credentials) throws NotFoundException, CredentialMisMatchException {
        final String METHOD_NAME = "adminTokenCreation";
        final Function<String, String> cacheKeyBuilder = (adminId) -> String.format("user:auth:token:jti:admin:uid:%s",
                adminId);// aid=admin id

        Specification<AdminEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("email"), credentials.getEmail().toLowerCase()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Optional<AdminEntity> adminEntity = OperationExecutor.dbGet(() -> adminDbService.getAdmin(spec), SERVICE_NAME,
                METHOD_NAME);

        if (!adminEntity.isPresent()) {
            throw new NotFoundException("Admin not found ");
        }

        if (!bcryptEncoder.bCryptPasswordEncoder().matches(credentials.getPassword(),
                adminEntity.get().getPassword())) {
            throw new CredentialMisMatchException("Incorrect password");
        }

        // Check for existing JTI in cache
        Optional<String> cachedJti = Optional.ofNullable(OperationExecutor.redisGet(
                () -> cachedService.getFromCache(cacheKeyBuilder.apply(adminEntity.get().getId()), String.class),
                SERVICE_NAME, METHOD_NAME));

        String jti = cachedJti.orElseGet(() -> UUID.randomUUID().toString());

        // Generate token for admin
        String token = jwtUtils.generateToken(adminEntity.get().getId(), adminEntity.get().getName(), "ADMIN", jti);

        if (!cachedJti.isPresent()) {
            OperationExecutor.redisSave(
                    () -> cachedService.saveResponse(cacheKeyBuilder.apply(adminEntity.get().getId()), jti,
                            Duration.ofDays(365)),
                    SERVICE_NAME, METHOD_NAME);
        }

        return aesEncryption.encrypt(token);
    }

    @Transactional
    public void adminLogout(String encryptedToken) {
        final String METHOD_NAME = "adminLogout";
        final Function<String, String> cacheKeyBuilder = (adminId) -> String.format("user:auth:token:jti:admin:uid:%s",
                adminId);

        try {
            String decryptToken = aesEncryption.decrypt(encryptedToken);
            Claims claims = jwtUtils.extractAllClaims(decryptToken);
            String adminId = (String) claims.get("uid");

            // Remove JTI from Redis (invalidates token)
            OperationExecutor.redisRemove(
                    () -> cachedService.deleteCacheByKey(cacheKeyBuilder.apply(adminId)),
                    SERVICE_NAME,
                    METHOD_NAME);

        } catch (Exception e) {
            // Token might be invalid, still proceed with logout
        }
    }

    public boolean verifyAndValidateToken(String encryptToken) {
        final String METHOD_NAME = "verifyAndValidateToken";

        String decryptToken = aesEncryption.decrypt(encryptToken);

        if (!jwtUtils.tokenVerification(decryptToken)) {
            return false;
        }

        Claims claims = jwtUtils.extractAllClaims(decryptToken);
        String userId = (String) claims.get("uid");
        String jti = (String) claims.get("jti");
        String role = (String) claims.get("role");

        // Use different cache key based on role
        String cacheKey;
        if ("ADMIN".equals(role)) {
            cacheKey = String.format("user:auth:token:jti:admin:uid:%s", userId);
        } else {
            cacheKey = String.format("user:auth:token:jti:uid:%s", userId);
        }

        String cachedJti = OperationExecutor.redisGet(
                () -> cachedService.getFromCache(cacheKey, String.class),
                SERVICE_NAME, METHOD_NAME);

        return cachedJti != null && cachedJti.equals(jti);

    }
}