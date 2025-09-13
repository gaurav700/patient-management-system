package com.pm.authservice.advices;

import com.pm.authservice.exception.EmailAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler for the Patient Service
 * Provides consistent error responses across all controllers
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation error on path: {}", request.getRequestURI(), ex);

        List<ApiError.FieldError> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                fieldErrors.add(ApiError.FieldError.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .rejectedValue(fieldError.getRejectedValue())
                        .build());
            }
        });

        ApiError apiError = ApiError.validationError(
                "Validation failed for one or more fields",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handle constraint violation exceptions (e.g., from @Valid on method parameters)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        log.warn("Constraint violation on path: {}", request.getRequestURI(), ex);

        List<ApiError.FieldError> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            fieldErrors.add(ApiError.FieldError.builder()
                    .field(fieldName)
                    .message(violation.getMessage())
                    .rejectedValue(violation.getInvalidValue())
                    .build());
        }

        ApiError apiError = ApiError.validationError(
                "Constraint validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request) {

        log.warn("Resource not found on path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.notFound(
                ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    /**
     * Handle 404 - No Handler Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.warn("No handler found for path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.notFound(
                "Endpoint not found: " + ex.getRequestURL(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    /**
     * Handle HTTP method not supported
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Method not supported on path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.of(
                405,
                "METHOD_NOT_ALLOWED",
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
                request.getRequestURI()
        );
        apiError.setDetails("Supported methods: " + String.join(", ", ex.getSupportedMethods()));

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(apiError);
    }

    /**
     * Handle malformed JSON or request body issues
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed request body on path: {}", request.getRequestURI(), ex);

        String message = "Malformed JSON request";
        if (ex.getCause() instanceof DateTimeParseException) {
            message = "Invalid date format. Please use YYYY-MM-DD format";
        }

        ApiError apiError = ApiError.of(
                400,
                "BAD_REQUEST",
                message,
                request.getRequestURI()
        );
        apiError.setDetails("Please check your request body format");

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        log.warn("Missing request parameter on path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.of(
                400,
                "BAD_REQUEST",
                "Missing required parameter: " + ex.getParameterName(),
                request.getRequestURI()
        );
        apiError.setDetails("Parameter '" + ex.getParameterName() + "' of type '" + ex.getParameterType() + "' is required");

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handle method argument type mismatch (e.g., invalid UUID format)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        log.warn("Type mismatch on path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.of(
                400,
                "BAD_REQUEST",
                "Invalid parameter format: " + ex.getName(),
                request.getRequestURI()
        );
        apiError.setDetails("Expected type: " + ex.getRequiredType().getSimpleName() +
                ", but received: " + ex.getValue());

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handle database integrity violations (e.g., unique constraint violations)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Data integrity violation on path: {}", request.getRequestURI(), ex);

        String message = "Data integrity violation";
        if (ex.getMessage() != null && ex.getMessage().contains("unique")) {
            message = "A resource with this information already exists";
        }

        ApiError apiError = ApiError.of(
                409,
                "CONFLICT",
                message,
                request.getRequestURI()
        );
        apiError.setDetails("Please check for duplicate values in unique fields");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    /**
     * Handle date/time parsing errors
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiError> handleDateTimeParse(
            DateTimeParseException ex,
            HttpServletRequest request) {

        log.warn("Date parsing error on path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.of(
                400,
                "BAD_REQUEST",
                "Invalid date format",
                request.getRequestURI()
        );
        apiError.setDetails("Please use YYYY-MM-DD format for dates");

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument on path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.of(
                400,
                "BAD_REQUEST",
                ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided",
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error on path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.internalError(
                "An unexpected error occurred",
                request.getRequestURI()
        );
        apiError.setDetails("Please try again later or contact support if the problem persists");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    /**
     * Handle email already exists exception
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Email already exists on path: {}", request.getRequestURI(), ex);

        ApiError apiError = ApiError.of(
                409,
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

}