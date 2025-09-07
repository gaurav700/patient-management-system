package com.pm.patientservice.advices;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard API Response wrapper for the Patient Service
 * Provides consistent structure for all successful API responses
 *
 * @param <T> The type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Indicates whether the operation was successful
     */
    @Builder.Default
    private boolean success = true;

    /**
     * HTTP status code (e.g., 200, 201, 204)
     */
    private int status;

    /**
     * Brief message describing the operation result
     */
    private String message;

    /**
     * The actual response data/payload
     */
    private T data;

    /**
     * API endpoint path where the request was processed
     */
    private String path;

    /**
     * Timestamp when the response was generated
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Pagination information (for list responses)
     */
    private PaginationInfo pagination;

    /**
     * Additional metadata about the response
     */
    private Map<String, Object> metadata;

    /**
     * Pagination information for paginated responses
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        /**
         * Current page number (0-based)
         */
        private int page;

        /**
         * Number of items per page
         */
        private int size;

        /**
         * Total number of elements across all pages
         */
        private long totalElements;

        /**
         * Total number of pages
         */
        private int totalPages;

        /**
         * Whether this is the first page
         */
        private boolean first;

        /**
         * Whether this is the last page
         */
        private boolean last;

        /**
         * Whether there is a next page
         */
        private boolean hasNext;

        /**
         * Whether there is a previous page
         */
        private boolean hasPrevious;
    }

    // ===== Static Factory Methods =====

    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(T data, String message, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .path(path)
                .build();
    }

    /**
     * Create a successful response with data and custom status
     */
    public static <T> ApiResponse<T> success(T data, String message, String path, int status) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .data(data)
                .path(path)
                .build();
    }

    /**
     * Create a successful response without data (e.g., for DELETE operations)
     */
    public static <T> ApiResponse<T> success(String message, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Create a successful response for created resources
     */
    public static <T> ApiResponse<T> created(T data, String message, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(201)
                .message(message)
                .data(data)
                .path(path)
                .build();
    }

    /**
     * Create a successful response with pagination info
     */
    public static <T> ApiResponse<List<T>> successWithPagination(
            List<T> data,
            String message,
            String path,
            PaginationInfo pagination) {
        return ApiResponse.<List<T>>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .path(path)
                .pagination(pagination)
                .build();
    }

    /**
     * Create a no content response (204)
     */
    public static <T> ApiResponse<T> noContent(String message, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(204)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Create a response with additional metadata
     */
    public static <T> ApiResponse<T> withMetadata(
            T data,
            String message,
            String path,
            Map<String, Object> metadata) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .path(path)
                .metadata(metadata)
                .build();
    }

    /**
     * Create pagination info from Spring Data Page
     */
    public static PaginationInfo fromPage(org.springframework.data.domain.Page<?> page) {
        return PaginationInfo.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}