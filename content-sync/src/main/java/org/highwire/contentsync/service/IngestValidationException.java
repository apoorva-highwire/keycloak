package org.highwire.contentsync.service;

import java.util.List;

/** Carries field-level errors through to GlobalExceptionHandler, same role as iam-service's validation failures. */
public class IngestValidationException extends RuntimeException {

    private final List<String> errors;

    public IngestValidationException(List<String> errors) {
        super("Validation failed: " + String.join("; ", errors));
        this.errors = errors;
    }

    public List<String> errors() {
        return errors;
    }
}
