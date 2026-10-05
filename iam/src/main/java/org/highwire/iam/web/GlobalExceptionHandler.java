package org.highwire.iam.web;

import java.util.List;

import org.highwire.authsdk.logging.AppLogger;
import org.highwire.authsdk.logging.Loggers;
import org.highwire.iam.exception.KeycloakOperationException;
import org.highwire.iam.exception.RealmNotFoundException;
import org.highwire.iam.exception.ResourceAlreadyExistsException;
import org.highwire.iam.exception.ResourceNotFoundException;
import org.highwire.iam.exception.UserNotFoundException;
import org.highwire.iam.web.response.ApiError;
import org.highwire.iam.web.response.ApiResponse;
import org.highwire.iam.web.response.MessageInformationFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Every exception the gateway/service layer throws is translated here
 * into the same ApiResponse envelope every successful call returns
 * (success=false, data=null, errors populated) — callers never see a
 * raw Keycloak SDK exception, a stack trace, or a differently-shaped
 * error body depending on which operation failed.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final AppLogger log = Loggers.of(GlobalExceptionHandler.class);

    private final MessageInformationFactory messageInformationFactory;

    public GlobalExceptionHandler(MessageInformationFactory messageInformationFactory) {
        this.messageInformationFactory = messageInformationFactory;
    }

    @ExceptionHandler(RealmNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRealmNotFound(RealmNotFoundException e) {
        log.warn("Realm not found: {}", e.getMessage());
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), List.of(new ApiError(null, "REALM_NOT_FOUND", e.getMessage())));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), List.of(new ApiError(null, "USER_NOT_FOUND", e.getMessage())));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), List.of(new ApiError(null, "NOT_FOUND", e.getMessage())));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceAlreadyExists(ResourceAlreadyExistsException e) {
        log.warn("Resource already exists: {}", e.getMessage());
        return respond(HttpStatus.CONFLICT, e.getMessage(), List.of(new ApiError(null, "ALREADY_EXISTS", e.getMessage())));
    }

    @ExceptionHandler(KeycloakOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleKeycloakOperation(KeycloakOperationException e) {
        log.error("Keycloak operation failed: " + e.getMessage(), e);
        return respond(HttpStatus.BAD_GATEWAY, "Keycloak operation failed",
                List.of(new ApiError(null, "KEYCLOAK_OPERATION_FAILED", e.getMessage())));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String message = e.getReason() != null ? e.getReason() : status.getReasonPhrase();
        log.warn("Request rejected with status {}: {}", status, message);
        return respond(status, message, List.of(new ApiError(null, status.name(), message)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<ApiError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError(fe.getField(), "VALIDATION_FAILED", fieldMessage(fe)))
                .toList();
        log.warn("Request validation failed: {}", errors);
        return respond(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    /**
     * Catches whatever none of the specific handlers above matched — without
     * this, an unexpected RuntimeException fell through to Spring's default
     * error page with no application-level log line at all.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                List.of(new ApiError(null, "INTERNAL_ERROR", e.getMessage())));
    }

    private String fieldMessage(FieldError fe) {
        return fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value";
    }

    private ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, String message, List<ApiError> errors) {
        ApiResponse<Void> body = ApiResponse.failure(message, errors, messageInformationFactory.create());
        return ResponseEntity.status(status).body(body);
    }
}
