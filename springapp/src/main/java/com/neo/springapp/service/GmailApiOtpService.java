package com.neo.springapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

/**
 * Sends plain-text mail via Gmail API (users.messages.send) using OAuth2 refresh-token flow.
 * Configure with env: GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET, GMAIL_REFRESH_TOKEN, GMAIL_FROM_EMAIL.
 */
@Service
public class GmailApiOtpService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${GMAIL_CLIENT_ID:}")
    private String clientId;

    @Value("${GMAIL_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${GMAIL_REFRESH_TOKEN:}")
    private String refreshToken;

    @Value("${GMAIL_FROM_EMAIL:}")
    private String fromEmail;

    private volatile String cachedAccessToken;
    private volatile long accessTokenExpiresAtEpochMs;

    public GmailApiOtpService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(45))
                .build();
    }

    public boolean isConfigured() {
        return hasText(clientId) && hasText(clientSecret) && hasText(refreshToken) && hasText(fromEmail);
    }

    /**
     * Obtains an access token, using the in-memory cache when still valid (60s skew).
     */
    public String fetchAccessToken() {
        if (!isConfigured()) {
            throw new GmailApiException("missing_config", "Gmail API env vars are not fully set");
        }
        long now = System.currentTimeMillis();
        String cached = cachedAccessToken;
        long exp = accessTokenExpiresAtEpochMs;
        if (cached != null && now < exp - 60_000L) {
            return cached;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (cachedAccessToken != null && now < accessTokenExpiresAtEpochMs - 60_000L) {
                return cachedAccessToken;
            }
            TokenResponse token = requestNewAccessToken();
            this.cachedAccessToken = token.accessToken();
            this.accessTokenExpiresAtEpochMs = now + token.expiresInSeconds() * 1000L;
            return token.accessToken();
        }
    }

    private TokenResponse requestNewAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId.trim());
        form.add("client_secret", clientSecret.trim());
        form.add("refresh_token", refreshToken.trim());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    TOKEN_URL,
                    new HttpEntity<>(form, headers),
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GmailApiException("token_http_error", "Token endpoint returned " + response.getStatusCode());
            }
            return parseTokenResponse(response.getBody());
        } catch (HttpClientErrorException e) {
            handleTokenHttpError(e);
            throw new GmailApiException("token_request_failed", e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new GmailApiException("token_network_error", e.getMessage(), e);
        }
    }

    private void handleTokenHttpError(HttpClientErrorException e) {
        String body = e.getResponseBodyAsString();
        String oauthError = extractOAuthError(body);
        invalidateTokenCache();
        if ("invalid_grant".equals(oauthError)) {
            throw new GmailApiException(
                    oauthError,
                    "Refresh token rejected (revoked, expired, or wrong client). Re-authorize OAuth and update GMAIL_REFRESH_TOKEN.",
                    e);
        }
        if ("unauthorized_client".equals(oauthError)) {
            throw new GmailApiException(
                    oauthError,
                    "Client is not allowed to use this grant type or credentials are wrong. Check GMAIL_CLIENT_ID / GMAIL_CLIENT_SECRET.",
                    e);
        }
        if ("invalid_client".equals(oauthError)) {
            throw new GmailApiException(
                    oauthError,
                    "Invalid OAuth client id or secret.",
                    e);
        }
        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
            throw new GmailApiException(
                    oauthError != null ? oauthError : "unauthorized",
                    "Token endpoint returned " + e.getStatusCode() + ": " + body,
                    e);
        }
    }

    private void invalidateTokenCache() {
        this.cachedAccessToken = null;
        this.accessTokenExpiresAtEpochMs = 0L;
    }

    private TokenResponse parseTokenResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.hasNonNull("error")) {
                String err = root.get("error").asText();
                String desc = root.has("error_description") ? root.get("error_description").asText() : json;
                invalidateTokenCache();
                throw new GmailApiException(err, desc);
            }
            JsonNode access = root.get("access_token");
            if (access == null || access.isNull()) {
                throw new GmailApiException("invalid_token_response", "No access_token in response");
            }
            long expiresIn = root.has("expires_in") ? root.get("expires_in").asLong(3600L) : 3600L;
            return new TokenResponse(access.asText(), expiresIn);
        } catch (GmailApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GmailApiException("invalid_token_response", "Could not parse token JSON", e);
        }
    }

    private static String extractOAuthError(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            if (root.hasNonNull("error")) {
                return root.get("error").asText();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    /**
     * Sends a UTF-8 plain-text message via Gmail API.
     */
    public void sendPlainTextEmail(String to, String subject, String textBody) {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(textBody, "textBody");

        if (!isConfigured()) {
            throw new GmailApiException("missing_config", "Gmail API is not configured");
        }

        String access = fetchAccessToken();
        String rawMime = buildRfc2822Message(sanitizeHeader(fromEmail), sanitizeHeader(to), subject, textBody);
        String rawBase64Url = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rawMime.getBytes(StandardCharsets.UTF_8));

        String payload;
        try {
            payload = objectMapper.writeValueAsString(objectMapper.createObjectNode().put("raw", rawBase64Url));
        } catch (Exception e) {
            throw new GmailApiException("encode_error", "Failed to build JSON body", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(access);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GMAIL_SEND_URL,
                    new HttpEntity<>(payload, headers),
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new GmailApiException("send_failed", "Gmail send returned " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                invalidateTokenCache();
                // one retry with fresh token
                access = fetchAccessToken();
                headers.setBearerAuth(access);
                try {
                    ResponseEntity<String> retry = restTemplate.postForEntity(
                            GMAIL_SEND_URL,
                            new HttpEntity<>(payload, headers),
                            String.class);
                    if (!retry.getStatusCode().is2xxSuccessful()) {
                        throw new GmailApiException("send_failed", "Gmail send returned " + retry.getStatusCode(), e);
                    }
                    return;
                } catch (HttpClientErrorException e2) {
                    throw mapGmailSendError(e2);
                }
            }
            throw mapGmailSendError(e);
        } catch (RestClientException e) {
            throw new GmailApiException("send_network_error", e.getMessage(), e);
        }
    }

    private GmailApiException mapGmailSendError(HttpClientErrorException e) {
        String body = e.getResponseBodyAsString();
        if (e.getStatusCode().value() == 403) {
            return new GmailApiException(
                    "forbidden",
                    "Gmail API denied send (scope, quota, or from-address mismatch). Body: " + body,
                    e);
        }
        if (e.getStatusCode().value() == 400) {
            return new GmailApiException(
                    "bad_request",
                    "Invalid message format or encoding. Body: " + body,
                    e);
        }
        return new GmailApiException(
                "send_http_" + e.getStatusCode().value(),
                body,
                e);
    }

    /**
     * RFC 2822 message with base64-encoded UTF-8 body (robust for non-ASCII).
     */
    private static String buildRfc2822Message(String from, String to, String subject, String textBody) {
        String encSubject = rfc2047Subject(subject);
        byte[] bodyBytes = textBody.getBytes(StandardCharsets.UTF_8);
        String bodyB64 = Base64.getEncoder().encodeToString(bodyBytes);

        return "From: " + from + "\r\n"
                + "To: " + to + "\r\n"
                + "Subject: " + encSubject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "\r\n"
                + bodyB64 + "\r\n";
    }

    private static String rfc2047Subject(String subject) {
        String s = sanitizeHeader(Objects.requireNonNull(subject));
        boolean asciiOnly = s.chars().allMatch(c -> c >= 32 && c < 127 && c != '?');
        if (asciiOnly) {
            return s;
        }
        String b64 = Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + b64 + "?=";
    }

    private static String sanitizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private record TokenResponse(String accessToken, long expiresInSeconds) {}
}
