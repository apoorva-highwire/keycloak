package org.highwire.iam.web.response;

/** One structured error — field is null for a non-field-specific error (e.g. a Keycloak operation failure). */
public record ApiError(String field, String code, String message) {
}
