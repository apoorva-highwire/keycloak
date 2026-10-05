package org.highwire.contentsync.web;

import java.util.List;

import org.highwire.authsdk.logging.AppLogger;
import org.highwire.authsdk.logging.Loggers;
import org.highwire.contentsync.service.IngestValidationException;
import org.highwire.contentsync.web.response.ApiError;
import org.highwire.contentsync.web.response.ApiResponse;
import org.highwire.contentsync.web.response.MessageInformationFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Same role as iam-service's GlobalExceptionHandler — every error returns the same ApiResponse envelope. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final AppLogger log = Loggers.of(GlobalExceptionHandler.class);

    private final MessageInformationFactory messageInformationFactory;

    public GlobalExceptionHandler(MessageInformationFactory messageInformationFactory) {
        this.messageInformationFactory = messageInformationFactory;
    }

    @ExceptionHandler(IngestValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(IngestValidationException e) {
        log.warn("Ingest validation failed: {}", e.errors());
        List<ApiError> errors = e.errors().stream()
                .map(msg -> new ApiError(null, "VALIDATION_FAILED", msg))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                List.of(new ApiError(null, "INTERNAL_ERROR", e.getMessage())));
    }

    private ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, String message, List<ApiError> errors) {
        ApiResponse<Void> body = ApiResponse.failure(message, errors, messageInformationFactory.create());
        return ResponseEntity.status(status).body(body);
    }
}
