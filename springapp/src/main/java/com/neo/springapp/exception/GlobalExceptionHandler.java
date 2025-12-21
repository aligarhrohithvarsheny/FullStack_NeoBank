package com.neo.springapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * 
 * Returns JSON responses for all API exceptions.
 * Ensures API endpoints always return JSON, never HTML.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle JSON parsing errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] HttpMessageNotReadableException: " + e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Invalid request format. Please ensure Content-Type is application/json and request body is valid JSON.");
        response.put("error", "BAD_REQUEST");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("details", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Handle method not allowed (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] Method not supported: " + e.getMethod());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "HTTP method " + e.getMethod() + " is not supported for this endpoint.");
        response.put("error", "METHOD_NOT_ALLOWED");
        response.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        if (e.getSupportedMethods() != null) {
            response.put("supportedMethods", e.getSupportedMethods());
        }
        
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Handle 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException e) {
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] No handler found: " + e.getRequestURL());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Endpoint not found: " + e.getRequestURL());
        response.put("error", "NOT_FOUND");
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("path", e.getRequestURL());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("error", "VALIDATION_ERROR");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        response.put("errors", errors);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Required parameter '" + e.getParameterName() + "' is missing");
        response.put("error", "MISSING_PARAMETER");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("parameter", e.getParameterName());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Handle type mismatch errors
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Map<String, Object> response = new HashMap<>();
        String expectedType = "unknown";
        if (e.getRequiredType() != null) {
            expectedType = e.getRequiredType().getSimpleName();
        }
        response.put("success", false);
        response.put("message", "Invalid parameter type for '" + e.getName() + "'. Expected: " + expectedType);
        response.put("error", "TYPE_MISMATCH");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] Unhandled exception: " + e.getClass().getSimpleName());
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] Message: " + e.getMessage());
        e.printStackTrace();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "An error occurred processing your request");
        response.put("error", "INTERNAL_SERVER_ERROR");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        // Only include error details in development
        if (System.getProperty("spring.profiles.active") == null || 
            System.getProperty("spring.profiles.active").contains("dev")) {
            response.put("details", e.getMessage());
        }
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
