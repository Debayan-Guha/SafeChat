package com.safechat.userservice.service.userService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.dto.request.OtpReceiveDto;
import com.safechat.userservice.dto.request.create.UserAccountCreateDto;
import com.safechat.userservice.dto.request.update.UserProfileUpdateDto;
import com.safechat.userservice.dto.response.UserResponseDto;
import com.safechat.userservice.entity.UserEntity;
import com.safechat.userservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.userservice.exception.ApplicationException.NotFoundException;
import com.safechat.userservice.jwt.JwtUtils;
import com.safechat.userservice.mapper.toDto.UserToDto;
import com.safechat.userservice.service.CachedService;
import com.safechat.userservice.service.EmailService;
import com.safechat.userservice.service.dbService.UserDbService;
import com.safechat.userservice.utility.OperationExecutor;
import com.safechat.userservice.utility.Enumeration.OtpType;
import com.safechat.userservice.utility.Enumeration.Status;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.encryption.AesEncryption;
import com.safechat.userservice.utility.encryption.BcryptEncoder;

import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@Service
public class UserWriteService {

    private final String SERVICE_NAME = "UserWriteService";

    private final UserDbService userDbService;
    private final AesEncryption aesEncryption;
    private final BcryptEncoder bcryptEncoder;
    private final CachedService cachedService;
    private final EmailService emailService;
    private final JwtUtils jwtUtils;

    public UserWriteService(UserDbService userDbService,
            AesEncryption aesEncryption,
            BcryptEncoder bcryptEncoder,
            CachedService cachedService, JwtUtils jwtUtils, EmailService emailService) {
        this.userDbService = userDbService;
        this.aesEncryption = aesEncryption;
        this.bcryptEncoder = bcryptEncoder;
        this.cachedService = cachedService;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
    }

    @Transactional
    public void createAccount(UserAccountCreateDto requestDto) throws NotFoundException, AlreadyExistsException {

        final String METHOD_NAME = "createAccount";
        final String cacheKey = String.format("user:otp:%s", requestDto.getEmail().toLowerCase());

        Integer cachedOtp = Optional.ofNullable(cachedService.getFromCache(cacheKey, Integer.class))
                .orElseThrow(() -> new NotFoundException("OTP expired or not found"));

        if (!cachedOtp.equals(requestDto.getOtp())) {
            throw new ValidationException("Otp mismatch");
        }

        OperationExecutor.redisRemove(() -> cachedService.deleteCacheByKey(cacheKey), SERVICE_NAME, METHOD_NAME);

        Specification<UserEntity> displayNameSpec = (root, query, cb) -> cb.equal(root.get("displayName"),
                requestDto.getDisplayName());

        if (userDbService.exists(displayNameSpec)) {
            throw new AlreadyExistsException("Display Name already exists");
        }

        Specification<UserEntity> emailSpec = (root, query, cb) -> cb.equal(root.get("email"),
                requestDto.getEmail().toLowerCase());

        if (userDbService.exists(emailSpec)) {
            throw new AlreadyExistsException("Email is already registered");
        }

        UserEntity userEntity = UserEntity.builder()
                .id("USR_" + UUID.randomUUID().toString())
                .userName(requestDto.getUserName())
                .displayName(requestDto.getDisplayName())
                .email(requestDto.getEmail().toLowerCase())
                .publicKey(requestDto.getPublicKey())
                .encryptedPrivateKey(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getPrivateKey()))
                .status(Status.ACTIVE)
                .password(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getPassword())).build();

        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);

    }

    @Transactional
    public UserResponseDto updateProfile(String encryptToken, UserProfileUpdateDto requestDto)
            throws NotFoundException, AlreadyExistsException {

        final String METHOD_NAME = "updateProfile";
        Function<String, String> cache1 = (email) -> String.format("user:otp:%s", email.toLowerCase());

        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"),
                userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (requestDto.getEmail() != null && !requestDto.getEmail().equalsIgnoreCase(userEntity.getEmail())
                && !requestDto.getEmail().isBlank()) {

            Integer cachedOtp = Optional
                    .ofNullable(cachedService.getFromCache(cache1.apply(requestDto.getEmail().toLowerCase()),
                            Integer.class))
                    .orElseThrow(() -> new NotFoundException("OTP expired or not found for new email"));

            if (!cachedOtp.equals(requestDto.getOtp())) {
                throw new ValidationException("OTP mismatch for email update");
            }
            Specification<UserEntity> emailSpec = (root, query, cb) -> cb.equal(root.get("email"),
                    requestDto.getEmail().toLowerCase());

            if (userDbService.exists(emailSpec)) {
                throw new AlreadyExistsException("Email is already registered");
            }
            userEntity.setEmail(requestDto.getEmail().toLowerCase());
            cachedService.deleteCacheByKey(cache1.apply(requestDto.getEmail().toLowerCase()));
        }

        if (requestDto.getDisplayName() != null && !requestDto.getDisplayName().isBlank()) {
            if (!requestDto.getDisplayName().equals(userEntity.getDisplayName())) {
                Specification<UserEntity> nameSpec = (root, query, cb) -> cb.equal(root.get("displayName"),
                        requestDto.getDisplayName());
                if (userDbService.exists(nameSpec)) {
                    throw new AlreadyExistsException("Display Name already exists");
                }
            }
            userEntity.setDisplayName(requestDto.getDisplayName());
        }

        if (requestDto.getUserName() != null && !requestDto.getUserName().isBlank()) {
            userEntity.setUserName(requestDto.getUserName());
        }

        if (requestDto.getOldPassword() != null && !requestDto.getOldPassword().isBlank()
                && requestDto.getNewPassword() != null && !requestDto.getNewPassword().isBlank()) {

            if (!bcryptEncoder.bCryptPasswordEncoder().matches(requestDto.getOldPassword(), userEntity.getPassword())) {
                throw new ValidationException("Incorrect old password");
            }
            userEntity.setPassword(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getNewPassword()));
        }

        // 5. Save and Return
        UserEntity updatedUser = OperationExecutor.dbSaveAndReturn(() -> userDbService.save(userEntity), SERVICE_NAME,
                METHOD_NAME);

        return OperationExecutor.map(() -> UserToDto.convert(userEntity), SERVICE_NAME, METHOD_NAME);
    }

    @Transactional
    public void updateKeys(String encryptToken, UserProfileUpdateDto requestDto)
            throws NotFoundException, ValidationException {
        final String METHOD_NAME = "updateKeys";

        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        // Check if both keys are provided (can't update just one)
        if ((requestDto.getPublicKey() != null && !requestDto.getPublicKey().isBlank())
                && (requestDto.getPrivateKey() != null && !requestDto.getPrivateKey().isBlank())) {

            // Update Public Key
            userEntity.setPublicKey(requestDto.getPublicKey());

            // Update Private Key (store only the HASH)
            userEntity.setEncryptedPrivateKey(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getPrivateKey()));

        } else {
            throw new ValidationException("Both public key and private key must be provided together");
        }

        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);
    }

    @Transactional
    public void cancelDeletionRequest(String encryptToken) throws NotFoundException, ValidationException {
        final String METHOD_NAME = "cancelDeletionRequest";

        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        // Check if deletion is actually scheduled in database
        if (!userEntity.isDeletionScheduled()) {
            throw new ValidationException("No deletion request found to cancel");
        }

        // Cancel deletion
        userEntity.setDeletionScheduled(false);
        userEntity.setDeletionScheduledRequestAt(null);
        userEntity.setDeletionScheduledFor(null);

        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);
    }

    @Transactional
    public void requestAccountDeletion(String encryptToken, OtpReceiveDto requestDto)
            throws NotFoundException, ValidationException {
        final String METHOD_NAME = "requestAccountDeletion";
        Function<String, String> cacheOtpKey = (email) -> String.format("user:otp:%s:%s", email.toLowerCase(),
                OtpType.ACCOUNT_DELETION_REQUEST);

        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        // Verify OTP
        Integer cachedOtp = Optional
                .ofNullable(cachedService.getFromCache(cacheOtpKey.apply(userEntity.getEmail().toLowerCase()),
                        Integer.class))
                .orElseThrow(() -> new NotFoundException("OTP expired or not found"));

        if (!cachedOtp.equals(requestDto.getOtp())) {
            throw new ValidationException("Invalid OTP");
        }

        // Update user entity
        userEntity.setDeletionScheduled(true);
        userEntity.setDeletionScheduledRequestAt(LocalDateTime.now());
        userEntity.setDeletionScheduledFor(LocalDateTime.now().plusHours(24)); // Add this field to UserEntity

        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);

        // Clean up OTP
        OperationExecutor.redisRemove(
                () -> cachedService.deleteCacheByKey(cacheOtpKey.apply(userEntity.getEmail().toLowerCase())),
                SERVICE_NAME, METHOD_NAME);

    }

    @Transactional
    public void instantAccountDeletion(String encryptToken, OtpReceiveDto requestDto)
            throws NotFoundException, ValidationException {
        final String METHOD_NAME = "instantAccountDeletion";
        Function<String, String> cacheOtpKey = (email) -> String.format("user:otp:%s:%s",
                requestDto.getEmail().toLowerCase(), OtpType.ACCOUNT_DELETION_INSTANT);

        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        Integer cachedOtp = Optional
                .ofNullable(cachedService.getFromCache(cacheOtpKey.apply(userEntity.getEmail()), Integer.class))
                .orElseThrow(() -> new NotFoundException("OTP expired or not found"));

        if (!cachedOtp.equals(requestDto.getOtp())) {
            throw new ValidationException("Invalid OTP");
        }

        // Clean up OTP
        OperationExecutor.redisRemove(() -> cachedService.deleteCacheByKey(cacheOtpKey.apply(userEntity.getEmail())),
                SERVICE_NAME, METHOD_NAME);

        Specification<UserEntity> hasIdSpec = (root, query, cb) -> cb.equal(root.get("id"), userId);
        OperationExecutor.dbRemove(() -> userDbService.delete(hasIdSpec), SERVICE_NAME, METHOD_NAME);

        // Send instant deletion confirmation email
        emailService.sendAfterDeletionEmail(userEntity.getEmail());
    }

    public void sendOtp(String email, String otpType) throws NotFoundException {
        final String METHOD_NAME = "sendOtp";
        final String cacheKey = String.format("user:otp:%s:%s", email.toLowerCase(), otpType);

        // Check if email is registered for certain OTP types (not required for account
        // creation)
        if (!otpType.equals(OtpType.ACCOUNT_CREATION)) {
            Specification<UserEntity> emailSpec = (root, query, cb) -> cb.equal(root.get("email"), email.toLowerCase());
            if (!userDbService.exists(emailSpec)) {
                throw new NotFoundException(ApiMessage.EMAIL_NOT_REGISTERED);
            }
        }

        // Generate 6-digit OTP
        int otp = 100000 + new Random().nextInt(900000);

        // Store OTP in Redis with 1 minute expiry
        OperationExecutor.redisSave(() -> cachedService.saveResponse(cacheKey, otp, Duration.ofMinutes(1)),
                SERVICE_NAME, METHOD_NAME);

        // Send OTP via email based on type
        switch (otpType) {
            case OtpType.ACCOUNT_CREATION:
                emailService.sendOtpEmail(email, otp);
                break;
            case OtpType.ACCOUNT_DELETION_REQUEST:
                emailService.sendAccountDeletionRequestOtp(email, otp);
                break;
            case OtpType.ACCOUNT_DELETION_INSTANT:
                emailService.sendAccountDeletionInstantOtp(email, otp);
                break;
            case OtpType.PASSWORD_RESET:
                emailService.sendPasswordResetOtp(email, otp);
                break;
            case OtpType.EMAIL_UPDATE:
                emailService.sendOtpEmail(email, otp);
                break;
        }
    }

    @Transactional
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
