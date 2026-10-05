package org.highwire.iam.web.response;

import java.time.Instant;
import java.util.List;

/**
 * Same shape as {@link ApiResponse}, plus pagination — a separate type
 * rather than a nullable field on ApiResponse, so a non-paginated
 * endpoint's response never carries a dangling {@code "pagination": null}
 * that means nothing for that endpoint.
 */
public record PagedApiResponse<T>(
        boolean success,
        List<T> data,
        String message,
        List<ApiError> errors,
        MessageInformation messageInformation,
        PaginationInfo pagination,
        Instant timestamp) {

    public static <T> PagedApiResponse<T> success(List<T> data, PaginationInfo pagination,
            MessageInformation messageInformation) {
        return new PagedApiResponse<>(true, data, null, List.of(), messageInformation, pagination, Instant.now());
    }
}
