package com.safechat.userservice.service.userService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.safechat.userservice.dto.request.OtpReceiveDto;
import com.safechat.userservice.dto.request.create.UserAccountCreateDto;
import com.safechat.userservice.dto.request.update.UserProfileUpdateDto;
import com.safechat.userservice.dto.response.UserResponseDto;
import com.safechat.userservice.entity.PendingUserDeletionEntity;
import com.safechat.userservice.entity.UserEntity;
import com.safechat.userservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.userservice.exception.ApplicationException.NotFoundException;
import com.safechat.userservice.exception.ApplicationException.ValidationException;
import com.safechat.userservice.jwt.JwtUtils;
import com.safechat.userservice.mapper.toDto.UserToDto;
import com.safechat.userservice.service.CachedService;
import com.safechat.userservice.service.EmailService;
import com.safechat.userservice.service.dbService.PendingUserDeletionDbService;
import com.safechat.userservice.service.dbService.UserDbService;
import com.safechat.userservice.utility.OperationExecutor;
import com.safechat.userservice.utility.Enumeration.OtpType;
import com.safechat.userservice.utility.Enumeration.Status;
import com.safechat.userservice.utility.Enumeration.UserDeletionStatus;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.encryption.AesEncryption;
import com.safechat.userservice.utility.encryption.BcryptEncoder;
import com.safechat.userservice.utility.encryption.Pbkdf2Encoder;

import jakarta.transaction.Transactional;

@Service
public class UserWriteService {

    private final String SERVICE_NAME = "UserWriteService";
    private static final Logger log = LoggerFactory.getLogger(UserWriteService.class);

    private final UserDbService userDbService;
    private final AesEncryption aesEncryption;
    private final Pbkdf2Encoder pbkdf2Encoder;
    private final BcryptEncoder bcryptEncoder;
    private final UserReadService userReadService;
    private final CachedService cachedService;
    private final EmailService emailService;
    private final PendingUserDeletionDbService pendingUserDeletionDbService;
    private final JwtUtils jwtUtils;

    public UserWriteService(UserDbService userDbService,
            AesEncryption aesEncryption, Pbkdf2Encoder pbkdf2Encoder,
            BcryptEncoder bcryptEncoder,
            CachedService cachedService, JwtUtils jwtUtils, EmailService emailService,
            UserReadService userReadService,
            PendingUserDeletionDbService pendingUserDeletionDbService) {
        this.userDbService = userDbService;
        this.aesEncryption = aesEncryption;
        this.bcryptEncoder = bcryptEncoder;
        this.pbkdf2Encoder = pbkdf2Encoder;
        this.cachedService = cachedService;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.userReadService = userReadService;
        this.pendingUserDeletionDbService = pendingUserDeletionDbService;
    }

    @Transactional
    public void createAccount(UserAccountCreateDto requestDto) throws NotFoundException, AlreadyExistsException {

        final String METHOD_NAME = "createAccount";
        final String cacheKey = String.format("user:otp:%s:ACCOUNT_CREATION", requestDto.getEmail().toLowerCase());

        log.debug("{} - Verifying OTP for email: {}", METHOD_NAME, requestDto.getEmail().toLowerCase());

        Integer cachedOtp = Optional.ofNullable(cachedService.getFromCache(cacheKey, Integer.class))
                .orElseThrow(() -> {
                    log.warn("{} - OTP expired or not found for email: {}", METHOD_NAME, requestDto.getEmail().toLowerCase());
                    return new NotFoundException("OTP expired or not found");
                });

        if (!cachedOtp.equals(requestDto.getOtp())) {
            log.warn("{} - OTP mismatch for email: {}", METHOD_NAME, requestDto.getEmail().toLowerCase());
            throw new ValidationException("Otp mismatch");
        }

        log.debug("{} - OTP verified, removing from cache", METHOD_NAME);
        OperationExecutor.redisRemove(() -> cachedService.deleteCacheByKey(cacheKey), SERVICE_NAME, METHOD_NAME);

        userReadService.isDisplayNameExists(requestDto.getDisplayName());
        userReadService.isEmailExists(requestDto.getEmail().toLowerCase());

        log.debug("{} - Generating unique userId", METHOD_NAME);
        String userId;
        boolean idExists;
        do {
            userId = "USR_" + UUID.randomUUID().toString();

            final String finalUserId = userId;

            Specification<UserEntity> checkUserSpec = (root, query, cb) -> cb.equal(root.get("id"), finalUserId);
            idExists = userDbService.exists(checkUserSpec);

            if (!idExists) {
                Specification<PendingUserDeletionEntity> checkPendingSpec = (root, query, cb) -> cb
                        .equal(root.get("userId"), finalUserId);
                idExists = pendingUserDeletionDbService.exists(checkPendingSpec);
            }

        } while (idExists);

        log.debug("{} - Unique userId generated: {}", METHOD_NAME, userId);

        UserEntity userEntity = UserEntity.builder()
                .id(userId)
                .userName(requestDto.getUserName())
                .displayName(requestDto.getDisplayName())
                .email(requestDto.getEmail().toLowerCase())
                .publicKey(requestDto.getPublicKey())
                .encryptedPrivateKey(pbkdf2Encoder.pbkd2Encoder().encode(requestDto.getPrivateKey()))
                .status(Status.ACTIVE)
                .password(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getPassword())).build();

        log.debug("{} - Saving new user to DB, userId: {}", METHOD_NAME, userId);
        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);

        log.info("{} - Account created successfully for userId: {}, email: {}", METHOD_NAME, userId, requestDto.getEmail().toLowerCase());

        emailService.sendWelcomeEmail(requestDto.getEmail().toLowerCase(), requestDto.getUserName());
        log.debug("{} - Welcome email sent to: {}", METHOD_NAME, requestDto.getEmail().toLowerCase());
    }

    @Transactional
    public UserResponseDto updateProfile(String encryptToken, UserProfileUpdateDto requestDto)
            throws NotFoundException, AlreadyExistsException, ValidationException {

        final String METHOD_NAME = "updateProfile";
        Function<String, String> cacheKeyBuilder = (email) -> String.format("user:otp:%s:ACCOUNT_UPDATION",
                email.toLowerCase());

        log.debug("{} - Decrypting token", METHOD_NAME);
        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
        log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        log.debug("{} - Querying DB for userId: {}", METHOD_NAME, userId);
        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> {
                    log.error("{} - User not found in DB for userId: {}", METHOD_NAME, userId);
                    return new NotFoundException("User not found with ID: " + userId);
                });

        if (requestDto.getEmailUpdate() != null
                && !requestDto.getEmailUpdate().getEmail().equalsIgnoreCase(userEntity.getEmail())) {

            String newEmail = requestDto.getEmailUpdate().getEmail().toLowerCase();
            int otp = requestDto.getEmailUpdate().getOtp();

            log.debug("{} - Verifying OTP for new email: {}", METHOD_NAME, newEmail);

            Integer cachedOtp = Optional
                    .ofNullable(cachedService.getFromCache(cacheKeyBuilder.apply(newEmail), Integer.class))
                    .orElseThrow(() -> {
                        log.warn("{} - OTP expired or not found for new email: {}", METHOD_NAME, newEmail);
                        return new NotFoundException("OTP expired or not found for new email");
                    });

            if (!cachedOtp.equals(otp)) {
                log.warn("{} - OTP mismatch for new email: {}", METHOD_NAME, newEmail);
                throw new ValidationException("OTP mismatch for email update");
            }

            userReadService.isEmailExists(newEmail);
            userEntity.setEmail(newEmail);
            cachedService.deleteCacheByKey(cacheKeyBuilder.apply(newEmail));
            log.debug("{} - Email updated to: {}", METHOD_NAME, newEmail);
        }

        if (requestDto.getDisplayName() != null && !requestDto.getDisplayName().isBlank()) {
            if (!requestDto.getDisplayName().equals(userEntity.getDisplayName())) {
                userReadService.isDisplayNameExists(requestDto.getDisplayName());
            }
            userEntity.setDisplayName(requestDto.getDisplayName());
            log.debug("{} - DisplayName updated to: {}", METHOD_NAME, requestDto.getDisplayName());
        }

        if (requestDto.getUserName() != null && !requestDto.getUserName().isBlank()) {
            userEntity.setUserName(requestDto.getUserName());
            log.debug("{} - UserName updated to: {}", METHOD_NAME, requestDto.getUserName());
        }

        if (requestDto.getPasswordUpdate() != null) {

            String oldPassword = requestDto.getPasswordUpdate().getOldPassword();
            String newPassword = requestDto.getPasswordUpdate().getNewPassword();

            log.debug("{} - Verifying old password for userId: {}", METHOD_NAME, userId);

            if (!bcryptEncoder.bCryptPasswordEncoder().matches(oldPassword, userEntity.getPassword())) {
                log.warn("{} - Incorrect old password for userId: {}", METHOD_NAME, userId);
                throw new ValidationException("Incorrect old password");
            }
            userEntity.setPassword(bcryptEncoder.bCryptPasswordEncoder().encode(newPassword));
            log.debug("{} - Password updated for userId: {}", METHOD_NAME, userId);
        }

        log.debug("{} - Saving updated user to DB, userId: {}", METHOD_NAME, userId);
        UserEntity updatedUser = OperationExecutor.dbSaveAndReturn(() -> userDbService.save(userEntity), SERVICE_NAME,
                METHOD_NAME);

        log.info("{} - Profile updated successfully for userId: {}", METHOD_NAME, userId);

        emailService.sendProfileUpdateConfirmation(updatedUser.getEmail(), updatedUser.getUserName());
        log.debug("{} - Profile update confirmation email sent to: {}", METHOD_NAME, updatedUser.getEmail());

        return OperationExecutor.map(() -> UserToDto.convert(updatedUser), SERVICE_NAME, METHOD_NAME);
    }

    @Transactional
    public void cancelDeletionRequest(String encryptToken) throws NotFoundException, ValidationException {
        final String METHOD_NAME = "cancelDeletionRequest";

        log.debug("{} - Decrypting token", METHOD_NAME);
        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
        log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        log.debug("{} - Querying DB for userId: {}", METHOD_NAME, userId);
        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> {
                    log.error("{} - User not found in DB for userId: {}", METHOD_NAME, userId);
                    return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                });

        if (!userEntity.isDeletionScheduled()) {
            log.warn("{} - No deletion request found to cancel for userId: {}", METHOD_NAME, userId);
            throw new ValidationException("No deletion request found to cancel");
        }

        userEntity.setDeletionScheduled(false);
        userEntity.setDeletionScheduledRequestAt(null);
        userEntity.setDeletionScheduledFor(null);

        log.debug("{} - Saving cancellation to DB for userId: {}", METHOD_NAME, userId);
        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);

        log.info("{} - Deletion request cancelled successfully for userId: {}", METHOD_NAME, userId);
    }

    @Transactional
    public void requestAccountDeletion(String encryptToken, OtpReceiveDto requestDto)
            throws NotFoundException, ValidationException {
        final String METHOD_NAME = "requestAccountDeletion";
        Function<String, String> cacheOtpKey = (email) -> String.format("user:otp:%s:%s", email.toLowerCase(),
                OtpType.ACCOUNT_DELETION_REQUEST);

        log.debug("{} - Decrypting token", METHOD_NAME);
        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
        log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        log.debug("{} - Querying DB for userId: {}", METHOD_NAME, userId);
        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> {
                    log.error("{} - User not found in DB for userId: {}", METHOD_NAME, userId);
                    return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                });

        log.debug("{} - Verifying OTP for email: {}", METHOD_NAME, userEntity.getEmail().toLowerCase());
        Integer cachedOtp = Optional
                .ofNullable(cachedService.getFromCache(cacheOtpKey.apply(userEntity.getEmail().toLowerCase()),
                        Integer.class))
                .orElseThrow(() -> {
                    log.warn("{} - OTP expired or not found for userId: {}", METHOD_NAME, userId);
                    return new NotFoundException("OTP expired or not found");
                });

        if (!cachedOtp.equals(requestDto.getOtp())) {
            log.warn("{} - OTP mismatch for userId: {}", METHOD_NAME, userId);
            throw new ValidationException("Invalid OTP");
        }

        userEntity.setDeletionScheduled(true);
        userEntity.setDeletionScheduledRequestAt(LocalDateTime.now());
        userEntity.setDeletionScheduledFor(LocalDateTime.now().plusHours(24));

        log.debug("{} - Saving deletion schedule to DB for userId: {}", METHOD_NAME, userId);
        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);

        log.info("{} - Account deletion scheduled for userId: {}, scheduledFor: {}",
                METHOD_NAME, userId, userEntity.getDeletionScheduledFor());

        OperationExecutor.redisRemove(
                () -> cachedService.deleteCacheByKey(cacheOtpKey.apply(userEntity.getEmail().toLowerCase())),
                SERVICE_NAME, METHOD_NAME);
        log.debug("{} - OTP removed from cache for userId: {}", METHOD_NAME, userId);
    }

    @Transactional
    public void instantAccountDeletion(String encryptToken, OtpReceiveDto requestDto)
            throws NotFoundException, ValidationException {
        final String METHOD_NAME = "instantAccountDeletion";
        Function<String, String> cacheOtpKey = (email) -> String.format("user:otp:%s:%s",
                requestDto.getEmail().toLowerCase(), OtpType.ACCOUNT_DELETION_INSTANT);

        log.debug("{} - Decrypting token", METHOD_NAME);
        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");
        log.debug("{} - Extracted userId: {}", METHOD_NAME, userId);

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        log.debug("{} - Querying DB for userId: {}", METHOD_NAME, userId);
        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> {
                    log.error("{} - User not found in DB for userId: {}", METHOD_NAME, userId);
                    return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                });

        if (!userEntity.isDeletionScheduled()) {
            log.warn("{} - No deletion request found for userId: {}", METHOD_NAME, userId);
            throw new ValidationException("No deletion request found. Please request deletion first.");
        }

        log.debug("{} - Verifying OTP for email: {}", METHOD_NAME, userEntity.getEmail());
        Integer cachedOtp = Optional
                .ofNullable(cachedService.getFromCache(cacheOtpKey.apply(userEntity.getEmail()), Integer.class))
                .orElseThrow(() -> {
                    log.warn("{} - OTP expired or not found for userId: {}", METHOD_NAME, userId);
                    return new NotFoundException("OTP expired or not found");
                });

        if (!cachedOtp.equals(requestDto.getOtp())) {
            log.warn("{} - OTP mismatch for userId: {}", METHOD_NAME, userId);
            throw new ValidationException("Invalid OTP");
        }

        OperationExecutor.redisRemove(() -> cachedService.deleteCacheByKey(cacheOtpKey.apply(userEntity.getEmail())),
                SERVICE_NAME, METHOD_NAME);
        log.debug("{} - OTP removed from cache for userId: {}", METHOD_NAME, userId);

        PendingUserDeletionEntity pendingEntity = PendingUserDeletionEntity.builder()
                .userId(userId)
                .status(UserDeletionStatus.PENDING)
                .retryCount(0)
                .build();

        log.debug("{} - Saving to pending deletion table for userId: {}", METHOD_NAME, userId);
        OperationExecutor.dbSave(() -> pendingUserDeletionDbService.save(pendingEntity), SERVICE_NAME, METHOD_NAME);

        log.debug("{} - Deleting user from DB, userId: {}", METHOD_NAME, userId);
        OperationExecutor.dbRemove(() -> userDbService.delete(userEntity), SERVICE_NAME, METHOD_NAME);

        log.info("{} - Account deleted instantly for userId: {}", METHOD_NAME, userId);

        emailService.sendAfterDeletionEmail(userEntity.getEmail());
        log.debug("{} - Deletion confirmation email sent to: {}", METHOD_NAME, userEntity.getEmail());
    }

    public void sendOtp(String email, String otpType) throws NotFoundException {
        final String METHOD_NAME = "sendOtp";
        final String cacheKey = String.format("user:otp:%s:%s", email.toLowerCase(), otpType);

        log.debug("{} - OTP requested for email: {}, otpType: {}", METHOD_NAME, email, otpType);

        if (!otpType.equals(OtpType.ACCOUNT_CREATION)) {
            Specification<UserEntity> emailSpec = (root, query, cb) -> cb.equal(root.get("email"), email.toLowerCase());
            if (!userDbService.exists(emailSpec)) {
                log.warn("{} - Email not registered: {}", METHOD_NAME, email);
                throw new NotFoundException(ApiMessage.EMAIL_NOT_REGISTERED);
            }
        }

        int otp = 100000 + new Random().nextInt(900000);

        log.debug("{} - OTP generated, saving to cache for email: {}", METHOD_NAME, email);
        OperationExecutor.redisSave(() -> cachedService.saveResponse(cacheKey, otp, Duration.ofMinutes(1)),
                SERVICE_NAME, METHOD_NAME);

        try {
            switch (otpType) {
                case OtpType.ACCOUNT_CREATION:
                    emailService.sendAccountCreationOtp(email, otp);
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
                case OtpType.ACCOUNT_UPDATION:
                    emailService.sendAccountCreationOtp(email, otp);
                    break;
            }
            log.info("{} - OTP sent successfully to email: {}, otpType: {}", METHOD_NAME, email, otpType);
        } catch (Exception e) {
            log.error("{} - Failed to send OTP to email: {}, otpType: {}, rolling back cache", METHOD_NAME, email, otpType);
            cachedService.deleteCacheByKey(cacheKey);
            throw new RuntimeException("Failed to send OTP. Please try again.", e);
        }
    }

    @Transactional
    public void blockUserByAdmin(String userIdToBlock) throws NotFoundException, AlreadyExistsException {
        final String METHOD_NAME = "blockUserByAdmin";

        log.debug("{} - Admin blocking userId: {}", METHOD_NAME, userIdToBlock);

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userIdToBlock);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> {
                    log.error("{} - User not found for userId: {}", METHOD_NAME, userIdToBlock);
                    return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                });

        if (Status.BLOCK.equals(userEntity.getStatus())) {
            log.warn("{} - User already blocked, userId: {}", METHOD_NAME, userIdToBlock);
            throw new AlreadyExistsException("User is already blocked");
        }

        userEntity.setStatus(Status.BLOCK);

        log.debug("{} - Saving blocked status for userId: {}", METHOD_NAME, userIdToBlock);
        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);

        log.info("{} - User blocked successfully, userId: {}", METHOD_NAME, userIdToBlock);
    }

    @Transactional
    public void unblockUserByAdmin(String userIdToUnblock) throws NotFoundException {
        final String METHOD_NAME = "unblockUserByAdmin";

        log.debug("{} - Admin unblocking userId: {}", METHOD_NAME, userIdToUnblock);

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userIdToUnblock);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> {
                    log.error("{} - User not found for userId: {}", METHOD_NAME, userIdToUnblock);
                    return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                });

        if (!Status.BLOCK.equals(userEntity.getStatus())) {
            log.warn("{} - User is not blocked, userId: {}", METHOD_NAME, userIdToUnblock);
            throw new ValidationException("User is not blocked");
        }

        userEntity.setStatus(Status.ACTIVE);

        log.debug("{} - Saving active status for userId: {}", METHOD_NAME, userIdToUnblock);
        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);

        log.info("{} - User unblocked successfully, userId: {}", METHOD_NAME, userIdToUnblock);
    }

    @Transactional
    public void deleteUserByAdmin(String userId) throws NotFoundException {
        final String METHOD_NAME = "deleteUserByAdmin";

        log.debug("{} - Admin deleting userId: {}", METHOD_NAME, userId);

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> {
                    log.error("{} - User not found for userId: {}", METHOD_NAME, userId);
                    return new NotFoundException(ApiMessage.USER_NOT_FOUND);
                });

        OperationExecutor.dbRemove(() -> userDbService.delete(userEntity), SERVICE_NAME, METHOD_NAME);

        log.info("{} - User hard deleted by admin, userId: {}", METHOD_NAME, userId);
    }

}