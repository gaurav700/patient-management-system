package com.pm.patientservice.advices;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * Global response handler for the Patient Service
 * Automatically wraps successful responses in ApiResponse format
 * Provides consistent response structure across all controllers
 */
@Slf4j
@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    /**
     * Determine whether to process the response body
     * Skip processing for error responses (ApiError) and already wrapped responses (ApiResponse)
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip if response is already an ApiError (handled by GlobalExceptionHandler)
        if (ApiError.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }

        // Skip if response is already wrapped in ApiResponse
        if (ApiResponse.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }

        // Skip if response is a ResponseEntity containing ApiError or ApiResponse
        if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
            return false; // Let ResponseEntity handle its own wrapping
        }

        // Skip for actuator endpoints
        String className = returnType.getContainingClass().getName();
        if (className.contains("actuator") || className.contains("management")) {
            return false;
        }

        // Skip for Swagger/OpenAPI endpoints
        if (className.contains("springdoc") ||
                className.contains("swagger") ||
                className.contains("openapi") ||
                className.contains("ApiDocumentationController") ||
                className.contains("OpenApiWebMvcResource")) {
            return false;
        }

        // Also check the request path to exclude Swagger UI paths
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String requestPath = request.getRequestURI();
                if (requestPath != null && (
                        requestPath.contains("/swagger-ui") ||
                                requestPath.contains("/v3/api-docs") ||
                                requestPath.contains("/swagger-resources") ||
                                requestPath.contains("/webjars"))) {
                    return false;
                }
            }
        } catch (Exception e) {
            log.debug("Could not check request path for Swagger exclusion", e);
        }

        // Process all other responses
        return true;
    }

    /**
     * Process the response body before writing it
     * Wrap successful responses in ApiResponse format
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        try {
            String path = request.getURI().getPath();
            String method = request.getMethod().name();
            int statusCode = getStatusCode(response);

            log.debug("Wrapping response for {} {}", method, path);

            // Handle different response scenarios
            return wrapResponse(body, method, path, statusCode);

        } catch (Exception e) {
            log.error("Error occurred while wrapping response", e);
            // Return original body if wrapping fails
            return body;
        }
    }

    /**
     * Wrap the response body in ApiResponse format based on HTTP method and status
     */
    private Object wrapResponse(Object body, String method, String path, int statusCode) {

        // Handle null responses (typically for DELETE operations)
        if (body == null) {
            if ("DELETE".equals(method)) {
                return ApiResponse.noContent("Resource deleted successfully", path);
            }
            return ApiResponse.success("Operation completed successfully", path);
        }

        // Determine appropriate message and status based on HTTP method and current status
        String message = generateMessage(method, body);

        // Handle different HTTP status codes
        switch (statusCode) {
            case 201:
                return ApiResponse.created(body, message, path);
            case 204:
                return ApiResponse.noContent(message, path);
            case 200:
            default:
                return ApiResponse.success(body, message, path);
        }
    }

    /**
     * Generate appropriate success message based on HTTP method and response body
     */
    private String generateMessage(String method, Object body) {
        switch (method) {
            case "GET":
                if (body instanceof List<?> list) {
                    return list.isEmpty() ? "No records found" :
                            "Retrieved " + list.size() + " record(s) successfully";
                }
                return "Record retrieved successfully";

            case "POST":
                return "Record created successfully";

            case "PUT":
                return "Record updated successfully";

            case "PATCH":
                return "Record partially updated successfully";

            case "DELETE":
                return "Record deleted successfully";

            default:
                return "Operation completed successfully";
        }
    }

    /**
     * Extract status code from ServerHttpResponse
     * Falls back to 200 if unable to determine
     */
    private int getStatusCode(ServerHttpResponse response) {
        try {
            // Try to get status code from response
            if (response instanceof org.springframework.http.server.ServletServerHttpResponse servletResponse) {
                return servletResponse.getServletResponse().getStatus();
            }

            // Try to get from request context
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object statusObj = request.getAttribute("jakarta.servlet.error.status_code");
                if (statusObj instanceof Integer status) {
                    return status;
                }
            }

            // Default to 200 OK
            return HttpStatus.OK.value();

        } catch (Exception e) {
            log.debug("Could not determine status code, defaulting to 200", e);
            return HttpStatus.OK.value();
        }
    }

    /**
     * Check if the response should be treated as a list response
     */
    private boolean isList(Object body) {
        return body instanceof List<?>;
    }

    /**
     * Check if the response indicates a creation operation
     */
    private boolean isCreatedResponse(String method, int statusCode) {
        return "POST".equals(method) || statusCode == 201;
    }

    /**
     * Check if the response indicates a no-content operation
     */
    private boolean isNoContentResponse(Object body, String method, int statusCode) {
        return body == null || "DELETE".equals(method) || statusCode == 204;
    }
}