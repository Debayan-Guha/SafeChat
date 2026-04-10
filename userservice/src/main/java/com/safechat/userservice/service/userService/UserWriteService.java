package com.safechat.userservice.service.userService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
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
import com.safechat.userservice.kafka.KafkaProducer;
import com.safechat.userservice.mapper.toDto.UserToDto;
import com.safechat.userservice.service.CachedService;
import com.safechat.userservice.service.EmailService;
import com.safechat.userservice.service.dbService.PendingUserDeletionDbService;
import com.safechat.userservice.service.dbService.UserDbService;
import com.safechat.userservice.utility.OperationExecutor;
import com.safechat.userservice.utility.Enumeration.OtpType;
import com.safechat.userservice.utility.Enumeration.Status;
import com.safechat.userservice.utility.api.ApiMessage;
import com.safechat.userservice.utility.encryption.AesEncryption;
import com.safechat.userservice.utility.encryption.BcryptEncoder;
import com.safechat.userservice.utility.encryption.Pbkdf2Encoder;

import jakarta.transaction.Transactional;

@Service
public class UserWriteService {

    private final String SERVICE_NAME = "UserWriteService";

    private static final int BATCH_SIZE = 1000;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final UserDbService userDbService;
    private final AesEncryption aesEncryption;
    private final Pbkdf2Encoder pbkdf2Encoder;
    private final BcryptEncoder bcryptEncoder;
    private final UserReadService userReadService;
    private final CachedService cachedService;
    private final EmailService emailService;
    private final PendingUserDeletionDbService pendingUserDeletionDbService;
    private final KafkaProducer kafkaProducer;
    private final JwtUtils jwtUtils;

    public UserWriteService(UserDbService userDbService,
            AesEncryption aesEncryption, Pbkdf2Encoder pbkdf2Encoder,
            BcryptEncoder bcryptEncoder,
            CachedService cachedService, JwtUtils jwtUtils, EmailService emailService,
            UserReadService userReadService, KafkaProducer kafkaProducer,
            PendingUserDeletionDbService pendingUserDeletionDbService) {
        this.userDbService = userDbService;
        this.aesEncryption = aesEncryption;
        this.bcryptEncoder = bcryptEncoder;
        this.pbkdf2Encoder = pbkdf2Encoder;
        this.cachedService = cachedService;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.userReadService = userReadService;
        this.kafkaProducer = kafkaProducer;
        this.pendingUserDeletionDbService = pendingUserDeletionDbService;
    }

    @Transactional
    public void createAccount(UserAccountCreateDto requestDto) throws NotFoundException, AlreadyExistsException {

        final String METHOD_NAME = "createAccount";
        final String cacheKey = String.format("user:otp:%s:ACCOUNT_CREATION", requestDto.getEmail().toLowerCase());

        Integer cachedOtp = Optional.ofNullable(cachedService.getFromCache(cacheKey, Integer.class))
                .orElseThrow(() -> new NotFoundException("OTP expired or not found"));

        if (!cachedOtp.equals(requestDto.getOtp())) {
            throw new ValidationException("Otp mismatch");
        }

        OperationExecutor.redisRemove(() -> cachedService.deleteCacheByKey(cacheKey), SERVICE_NAME, METHOD_NAME);

        userReadService.isDisplayNameExists(requestDto.getDisplayName());
        userReadService.isEmailExists(requestDto.getEmail().toLowerCase());

        // Generate unique ID with collision check
        String userId;
        boolean idExists;
        do {
            userId = "USR_" + UUID.randomUUID().toString();

            final String finalUserId = userId; // Create effectively final variable

            // Check if ID already exists in users table
            Specification<UserEntity> checkUserSpec = (root, query, cb) -> cb.equal(root.get("id"), finalUserId);
            idExists = userDbService.exists(checkUserSpec);

            // Check if ID already exists in pending_deletions table
            if (!idExists) {
                Specification<PendingUserDeletionEntity> checkPendingSpec = (root, query, cb) -> cb
                        .equal(root.get("userId"), finalUserId);
                idExists = pendingUserDeletionDbService.exists(checkPendingSpec);
            }

        } while (idExists);

        UserEntity userEntity = UserEntity.builder()
                .id(userId)
                .userName(requestDto.getUserName())
                .displayName(requestDto.getDisplayName())
                .email(requestDto.getEmail().toLowerCase())
                .publicKey(requestDto.getPublicKey())
                .encryptedPrivateKey(pbkdf2Encoder.pbkd2Encoder().encode(requestDto.getPrivateKey()))
                .status(Status.ACTIVE)
                .password(bcryptEncoder.bCryptPasswordEncoder().encode(requestDto.getPassword())).build();

        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);

        emailService.sendWelcomeEmail(requestDto.getEmail().toLowerCase(), requestDto.getUserName());
    }

    @Transactional
    public UserResponseDto updateProfile(String encryptToken, UserProfileUpdateDto requestDto)
            throws NotFoundException, AlreadyExistsException, ValidationException {

        final String METHOD_NAME = "updateProfile";
        Function<String, String> cacheKeyBuilder = (email) -> String.format("user:otp:%s:ACCOUNT_UPDATION",
                email.toLowerCase());

        String decryptToken = aesEncryption.decrypt(encryptToken);
        String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        // Update Email
        if (requestDto.getEmailUpdate() != null
                && !requestDto.getEmailUpdate().getEmail().equalsIgnoreCase(userEntity.getEmail())) {

            String newEmail = requestDto.getEmailUpdate().getEmail().toLowerCase();
            int otp = requestDto.getEmailUpdate().getOtp();

            Integer cachedOtp = Optional
                    .ofNullable(cachedService.getFromCache(cacheKeyBuilder.apply(newEmail), Integer.class))
                    .orElseThrow(() -> new NotFoundException("OTP expired or not found for new email"));

            if (cachedOtp != otp) {
                throw new ValidationException("OTP mismatch for email update");
            }

            userReadService.isEmailExists(newEmail);
            userEntity.setEmail(newEmail);
            cachedService.deleteCacheByKey(cacheKeyBuilder.apply(newEmail));
        }

        // Update Display Name
        if (requestDto.getDisplayName() != null && !requestDto.getDisplayName().isBlank()) {
            if (!requestDto.getDisplayName().equals(userEntity.getDisplayName())) {
                userReadService.isDisplayNameExists(requestDto.getDisplayName());
            }
            userEntity.setDisplayName(requestDto.getDisplayName());
        }

        // Update User Name
        if (requestDto.getUserName() != null && !requestDto.getUserName().isBlank()) {
            userEntity.setUserName(requestDto.getUserName());
        }

        // Update Password
        if (requestDto.getPasswordUpdate() != null) {

            String oldPassword = requestDto.getPasswordUpdate().getOldPassword();
            String newPassword = requestDto.getPasswordUpdate().getNewPassword();

            if (!bcryptEncoder.bCryptPasswordEncoder().matches(oldPassword, userEntity.getPassword())) {
                throw new ValidationException("Incorrect old password");
            }
            userEntity.setPassword(bcryptEncoder.bCryptPasswordEncoder().encode(newPassword));
        }

        // Update Keys (both must be provided together)
        if (requestDto.getKeysUpdate() != null) {

            userEntity.setPublicKey(requestDto.getKeysUpdate().getPublicKey());
            userEntity
                    .setEncryptedPrivateKey(
                            pbkdf2Encoder.pbkd2Encoder().encode(requestDto.getKeysUpdate().getPrivateKey()));
        }

        // Save and Return
        UserEntity updatedUser = OperationExecutor.dbSaveAndReturn(() -> userDbService.save(userEntity), SERVICE_NAME,
                METHOD_NAME);

        emailService.sendProfileUpdateConfirmation(updatedUser.getEmail(), updatedUser.getUserName());

        return OperationExecutor.map(() -> UserToDto.convert(updatedUser), SERVICE_NAME, METHOD_NAME);
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

        if (!userEntity.isDeletionScheduled()) {
            throw new ValidationException("No deletion request found. Please request deletion first.");
        }

        Integer cachedOtp = Optional
                .ofNullable(cachedService.getFromCache(cacheOtpKey.apply(userEntity.getEmail()), Integer.class))
                .orElseThrow(() -> new NotFoundException("OTP expired or not found"));

        if (!cachedOtp.equals(requestDto.getOtp())) {
            throw new ValidationException("Invalid OTP");
        }

        // Clean up OTP
        OperationExecutor.redisRemove(() -> cachedService.deleteCacheByKey(cacheOtpKey.apply(userEntity.getEmail())),
                SERVICE_NAME, METHOD_NAME);

        OperationExecutor.dbRemove(() -> userDbService.delete(userEntity), SERVICE_NAME, METHOD_NAME);

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

        try {
            // Send OTP via email based on type
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
        } catch (Exception e) {
            // STEP 4: If email fails, rollback - delete the OTP from Redis
            cachedService.deleteCacheByKey(cacheKey);
            throw new RuntimeException("Failed to send OTP. Please try again.", e);
        }
    }

    public void deleteExpiredAccounts() {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            int page = 0;

            while (true) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);

                Specification<UserEntity> spec = (root, query, cb) -> {
                    Predicate isScheduled = cb.isTrue(root.get("isDeletionScheduled"));
                    Predicate timePassed = cb.lessThanOrEqualTo(
                            root.get("deletionScheduledFor"),
                            LocalDateTime.now());
                    return cb.and(isScheduled, timePassed);
                };

                List<UserEntity> usersToDelete = userDbService.getUsers(spec, pageable).getContent();

                if (usersToDelete.isEmpty()) {
                    break;
                }

                // Collect user IDs from Set (automatically removes duplicates)
                Set<String> userIds = usersToDelete.stream()
                        .map(UserEntity::getId)
                        .collect(Collectors.toSet());

                // Delete the batch
                deleteBatch(usersToDelete);

                // AFTER successful deletion, send Kafka event
                // kafkaProducer.sendUserDeletionEvent(userIds);

                kafkaProducer.sendUserDeletionEventBatch(new ArrayList<>(userIds));

                page++;
            }
        } finally {
            isRunning.set(false);
        }
    }

    @Transactional
    public void deleteBatch(List<UserEntity> usersToDelete) {
        userDbService.deleteAll(usersToDelete);
    }

    @Transactional
    public void blockUserByAdmin(String userIdToBlock) throws NotFoundException, AlreadyExistsException {
        final String METHOD_NAME = "blockUserByAdmin";

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userIdToBlock);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        if (Status.BLOCK.equals(userEntity.getStatus())) {
            throw new AlreadyExistsException("User is already blocked");
        }

        userEntity.setStatus(Status.BLOCK);

        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);
    }

    @Transactional
    public void unblockUserByAdmin(String userIdToUnblock) throws NotFoundException {
        final String METHOD_NAME = "unblockUserByAdmin";

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userIdToUnblock);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        if (!Status.BLOCK.equals(userEntity.getStatus())) {
            throw new ValidationException("User is not blocked");
        }

        userEntity.setStatus(Status.ACTIVE);

        OperationExecutor.dbSave(() -> userDbService.save(userEntity), SERVICE_NAME, METHOD_NAME);
    }

    @Transactional
    public void deleteUserByAdmin(String userId) throws NotFoundException {
        final String METHOD_NAME = "deleteUserByAdmin";

        Specification<UserEntity> getUserById = (root, query, cb) -> cb.equal(root.get("id"), userId);

        UserEntity userEntity = OperationExecutor
                .dbGet(() -> userDbService.getUser(getUserById), SERVICE_NAME, METHOD_NAME)
                .orElseThrow(() -> new NotFoundException(ApiMessage.USER_NOT_FOUND));

        // Hard delete the user
        OperationExecutor.dbRemove(() -> userDbService.delete(userEntity), SERVICE_NAME, METHOD_NAME);
    }

}
