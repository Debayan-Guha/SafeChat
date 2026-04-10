package com.safechat.userservice.utility;

import java.util.stream.Stream;

public interface Enumeration {

    public interface Role {
        String USER = "user";

    }

    public interface Status {
        String ACTIVE = "active";
        String BLOCK = "block";

        static boolean isValid(String value) {
            return Stream
                    .of(ACTIVE, BLOCK)
                    .anyMatch(type -> type.equals(value));
        }
    }

    public interface OtpType {
        String ACCOUNT_CREATION = "ACCOUNT_CREATION";
        String ACCOUNT_DELETION_REQUEST = "ACCOUNT_DELETION_REQUEST";
        String PASSWORD_RESET = "PASSWORD_RESET";
        String ACCOUNT_UPDATION = "ACCOUNT_UPDATION";
        String ACCOUNT_DELETION_INSTANT = "ACCOUNT_DELETION_INSTANT";

        // Added validation method
        static boolean isValid(String value) {
            return Stream
                    .of(ACCOUNT_CREATION, ACCOUNT_DELETION_REQUEST, PASSWORD_RESET, ACCOUNT_UPDATION,
                            ACCOUNT_DELETION_INSTANT)
                    .anyMatch(type -> type.equals(value));
        }
    }

    public interface ScheduledTaskType {
        String DELETE_EXPIRED_ACCOUNTS = "DELETE_EXPIRED_ACCOUNTS";
        String RETRY_FAILED_KAFKA_DELETIONS = "RETRY_FAILED_KAFKA_DELETIONS";

        static boolean isValid(String value) {
            return Stream.of(
                    DELETE_EXPIRED_ACCOUNTS,RETRY_FAILED_KAFKA_DELETIONS).anyMatch(type -> type.equals(value));
        }
    }

    public interface UserDeletionStatus {
        String KAFKA_SENT = "KAFKA_SENT"; 
        String KAFKA_SENT_FAILED = "KAFKA_SENT_FAILED";  
        String CHAT_SUCCESS = "CHAT_SUCCESS";
        String CHAT_FAILED = "CHAT_FAILED"; 
    }

}