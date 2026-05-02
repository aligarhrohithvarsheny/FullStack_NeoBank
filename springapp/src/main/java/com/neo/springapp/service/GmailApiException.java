package com.neo.springapp.service;

/**
 * Gmail OAuth2 or Gmail REST API failure.
 */
public class GmailApiException extends RuntimeException {

    private final String oauthError;

    public GmailApiException(String oauthError, String message) {
        super(message);
        this.oauthError = oauthError;
    }

    public GmailApiException(String oauthError, String message, Throwable cause) {
        super(message, cause);
        this.oauthError = oauthError;
    }

    public String getOAuthError() {
        return oauthError;
    }
}
