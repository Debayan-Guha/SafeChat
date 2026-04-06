package com.safechat.userservice.utility.api;

public interface ApiMessage {

    String ACCOUNT_CREATED = "User account created successfully";
    String PROFILE_UPDATED = "User profile updated successfully";
    String PROFILE_FOUND = "User profile found";
    String USER_FOUND = "User found";
    String USER_NOT_FOUND = "User not found";
    String EMAIL_NOT_REGISTERED="Email is not registered";
    String PUBLIC_KEY_UPDATED = "User public key updated successfully";
    String PUBLIC_KEY_FOUND = "User public key found";
    String DELETION_REQUEST_SUBMITTED = "Account deletion request submitted.Account will be deleted wihin 24 hours of raised request";
    String ACCOUNT_DELETED = "Account deleted successfully";
    String DELETION_REQUEST_CANCELLED = "Account deletion request cancelled successfully";

    String PAGE_VALIDATION_ERROR = "Page and Size must be greater than 0";
    String DATA_PROCESSING_FAILED = "Data Processing Failed";
    String DATABASE_CONNECTION_FAILED = "Database Connection Failed";
    String USER_SESSION_EXPIRED = "User Session Expired";
    String USER_TOKEN_SUCCESS = null;
    String USER_LOGOUT_SUCCESS = null;

}
