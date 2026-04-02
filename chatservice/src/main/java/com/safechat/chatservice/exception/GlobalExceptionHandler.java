package com.safechat.chatservice.exception;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.safechat.chatservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.chatservice.exception.ApplicationException.CredentialMisMatchException;
import com.safechat.chatservice.exception.ApplicationException.DataProcessingException;
import com.safechat.chatservice.exception.ApplicationException.DatabaseException;
import com.safechat.chatservice.exception.ApplicationException.ExternalApiException;
import com.safechat.chatservice.exception.ApplicationException.NotFoundException;
import com.safechat.chatservice.exception.ApplicationException.UserSessionExpiredException;
import com.safechat.chatservice.exception.ApplicationException.ValidationException;
import com.safechat.chatservice.utility.api.ApiMessage;
import com.safechat.chatservice.utility.api.ApiResponseFormatter;

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponseFormatter> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseFormatter.formatter(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseFormatter<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseFormatter.formatter(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation failed", errors));
    }

    @ExceptionHandler(CredentialMisMatchException.class)
    public ResponseEntity<ApiResponseFormatter> handleCredentialMismatchException(CredentialMisMatchException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseFormatter.formatter(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
    }

    @ExceptionHandler(DataProcessingException.class)
    public ResponseEntity<ApiResponseFormatter> handleDataProcessingException() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseFormatter
                .formatter(HttpStatus.INTERNAL_SERVER_ERROR.value(), ApiMessage.DATA_PROCESSING_FAILED));
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ApiResponseFormatter> handleDatabaseException() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseFormatter
                .formatter(HttpStatus.INTERNAL_SERVER_ERROR.value(), ApiMessage.DATABASE_CONNECTION_FAILED));
    }

    @ExceptionHandler(UserSessionExpiredException.class)
    public ResponseEntity<ApiResponseFormatter> handleUserSessionExpiredException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseFormatter.formatter(HttpStatus.UNAUTHORIZED.value(), ApiMessage.USER_SESSION_EXPIRED));
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiResponseFormatter> handleAlreadyExistsException(AlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseFormatter.formatter(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponseFormatter> handleValidationException(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseFormatter.formatter(HttpStatus.BAD_REQUEST.value(), msg));
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponseFormatter<Void>> externalApiException(ExternalApiException message) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseFormatter.formatter(HttpStatus.SERVICE_UNAVAILABLE.value(), message.getMessage()));
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiResponseFormatter<Void>> timeoutExceptionException() {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiResponseFormatter.formatter(HttpStatus.GATEWAY_TIMEOUT.value(),
                        "External Api Request Timeout"));
    }

    @ExceptionHandler({ ConnectException.class, StreamReadException.class, DecodingException.class })
    public ResponseEntity<ApiResponseFormatter<Void>> connectException() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseFormatter.formatter(HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "External service connection refused"));
    }

}