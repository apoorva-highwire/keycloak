package org.highwire.contentsync.web.response;

/** One structured error — field is null for a non-field-specific error. */
public record ApiError(String field, String code, String message) {
}
