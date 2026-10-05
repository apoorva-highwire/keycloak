package org.highwire.iam.web.response;

import java.time.Instant;
import java.util.List;

/**
 * The one response shape every endpoint in this service returns, success
 * or failure — a caller never has to branch on "is this a different JSON
 * shape" depending on what happened, only on {@code success}.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        List<ApiError> errors,
        MessageInformation messageInformation,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data, String message, MessageInformation messageInformation) {
        return new ApiResponse<>(true, data, message, List.of(), messageInformation, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, List<ApiError> errors,
            MessageInformation messageInformation) {
        return new ApiResponse<>(false, null, message, errors, messageInformation, Instant.now());
    }
}
