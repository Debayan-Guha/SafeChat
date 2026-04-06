package com.safechat.userservice.exception;

public class ApplicationException {

    public static class NotFoundException extends Exception {
        public NotFoundException(String msg) {
            super(msg);
        }
    }

    public static class CredentialMisMatchException extends Exception {
        public CredentialMisMatchException(String msg) {
            super(msg);
        }
    }

    public static class DataProcessingException extends RuntimeException {
        public DataProcessingException() {
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class DatabaseException extends RuntimeException {
        public DatabaseException() {
        }
    }

    public static class UserSessionExpiredException extends Exception {
        public UserSessionExpiredException() {
        }
    }

    public static class AlreadyExistsException extends Exception {
        public AlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class ExternalApiException extends RuntimeException {
        public ExternalApiException(String message) {
            super(message);
        }

    }

}
