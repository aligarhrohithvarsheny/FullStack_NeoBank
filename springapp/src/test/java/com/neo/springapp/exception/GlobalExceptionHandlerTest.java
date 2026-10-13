package com.neo.springapp.exception;

import jakarta.servlet.ServletException;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void clientAbortShouldBeIgnored() {
        ResponseEntity<?> response = handler.handleClientDisconnect(
                new AsyncRequestNotUsableException("ServletOutputStream failed to write: java.io.IOException: Broken pipe"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void clientAbortExceptionShouldBeIgnored() {
        ResponseEntity<?> response = handler.handleClientDisconnect(
                new ClientAbortException("Broken pipe"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void responseStatusExceptionShouldPreserveClientErrorStatusAndMessage() {
        String message = "A product version already exists for this code and effective date.";
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.CONFLICT, message));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals(message, response.getBody().get("message"));
        assertEquals("CONFLICT", response.getBody().get("error"));
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().get("status"));
    }
}
