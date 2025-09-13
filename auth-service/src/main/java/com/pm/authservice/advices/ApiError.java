package com.pm.authservice.advices;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API Error response structure for the Patient Service
 * Used to provide consistent error information to API clients
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    /**
     * HTTP status code (e.g., 400, 404, 500)
     */
    private int status;

    /**
     * Brief error type or category (e.g., "VALIDATION_ERROR", "NOT_FOUND", "INTERNAL_ERROR")
     */
    private String error;

    /**
     * Human-readable error message describing what went wrong
     */
    private String message;

    /**
     * Additional detailed error information or developer message
     */
    private String details;

    /**
     * API endpoint path where the error occurred
     */
    private String path;

    /**
     * Timestamp when the error occurred
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * List of validation errors (used for field validation failures)
     * Each entry contains field name and corresponding error message
     */
    private List<FieldError> fieldErrors;

    /**
     * Additional metadata or context information about the error
     */
    private Map<String, Object> metadata;

    /**
     * Represents a field-specific validation error
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        /**
         * Name of the field that failed validation
         */
        private String field;

        /**
         * Error message for the specific field
         */
        private String message;

        /**
         * The rejected value that caused the validation error
         */
        private Object rejectedValue;
    }

    /**
     * Static factory method for creating basic API errors
     */
    public static ApiError of(int status, String error, String message, String path) {
        return ApiError.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Static factory method for creating validation API errors
     */
    public static ApiError validationError(String message, String path, List<FieldError> fieldErrors) {
        return ApiError.builder()
                .status(400)
                .error("VALIDATION_ERROR")
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }

    /**
     * Static factory method for creating not found errors
     */
    public static ApiError notFound(String message, String path) {
        return ApiError.builder()
                .status(404)
                .error("NOT_FOUND")
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Static factory method for creating internal server errors
     */
    public static ApiError internalError(String message, String path) {
        return ApiError.builder()
                .status(500)
                .error("INTERNAL_SERVER_ERROR")
                .message(message)
                .path(path)
                .build();
    }
}