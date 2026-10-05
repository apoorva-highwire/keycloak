package org.highwire.iam.web.response;

public record PaginationInfo(int page, int size, long totalElements, int totalPages) {

    public static PaginationInfo of(int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PaginationInfo(page, size, totalElements, totalPages);
    }
}
