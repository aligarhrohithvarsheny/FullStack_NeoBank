package com.neo.springapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class UserSessionTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_LIFETIME_SECONDS = 30 * 60;
    private final byte[] secret;

    public UserSessionTokenService(@Value("${app.auth.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.auth.secret must contain at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(Long userId, String accountNumber) {
        return issue(userId, accountNumber, "USER");
    }

    public String issueCard360(Long userId, String accountNumber) {
        return issue(userId, accountNumber, "CARD360");
    }

    public String issueInsuranceGuest(Long guestApplicationId) {
        return issue(guestApplicationId, "GUEST", "INSURANCE_GUEST");
    }

    private String issue(Long userId, String accountNumber, String scope) {
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_LIFETIME_SECONDS;
        String payload = userId + "|" + accountNumber + "|" + expiresAt + "|" + scope;
        String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + encode(sign(encodedPayload));
    }

    public SessionPrincipal verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]), decode(parts[1]))) return null;
            String[] values = new String(decode(parts[0]), StandardCharsets.UTF_8).split("\\|", -1);
            if ((values.length != 3 && values.length != 4)
                    || Long.parseLong(values[2]) < Instant.now().getEpochSecond()) return null;
            String scope = values.length == 4 ? values[3] : "USER";
            if (!"USER".equals(scope) && !"CARD360".equals(scope) && !"INSURANCE_GUEST".equals(scope)) return null;
            return new SessionPrincipal(Long.parseLong(values[0]), values[1], scope);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign session token", exception);
        }
    }

    private String encode(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private byte[] decode(String value) { return Base64.getUrlDecoder().decode(value); }

    public record SessionPrincipal(Long userId, String accountNumber, String scope) {}
}