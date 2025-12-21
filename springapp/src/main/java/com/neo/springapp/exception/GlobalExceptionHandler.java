package com.neo.springapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        System.err.println("==========================================");
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] HttpMessageNotReadableException caught");
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] Message: " + e.getMessage());
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] Root cause: " + (e.getRootCause() != null ? e.getRootCause().getMessage() : "null"));
        System.err.println("[GLOBAL-EXCEPTION-HANDLER] Most specific cause: " + (e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "null"));
        e.printStackTrace();
        System.err.println("==========================================");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Invalid request format. Please ensure Content-Type is application/json and request body is valid JSON.");
        response.put("error", "Request body parsing failed");
        response.put("details", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage());
        
        return ResponseEntity.badRequest().body(response);
    }
}
