package com.neo.springapp.controller;

import com.neo.springapp.model.LoanAgreement;
import com.neo.springapp.service.LoanAgreementService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/loan-agreements")
public class LoanAgreementController {
    private final LoanAgreementService agreementService;

    public LoanAgreementController(LoanAgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<?> shareDetails(@PathVariable String token) {
        try {
            return ResponseEntity.ok(agreementService.shareDetails(token));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/share/{token}/pdf")
    public ResponseEntity<?> sharePdf(@PathVariable String token) {
        try {
            return pdf(agreementService.sharePdf(token), "neobank-loan-agreement.pdf");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/share/{token}/captcha")
    public ResponseEntity<?> captcha(@PathVariable String token) {
        try {
            return ResponseEntity.ok(agreementService.createCaptcha(token));
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/share/{token}/sign")
    public ResponseEntity<?> sign(@PathVariable String token, @RequestBody SignRequest request,
                                  jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            boolean signed = agreementService.sign(token, request.challengeId(), request.captchaAnswer(),
                    request.signedName(), request.consent(), httpRequest.getRemoteAddr());
            if (!signed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "CAPTCHA is incorrect or expired. Refresh the CAPTCHA and try again."));
            }
            return ResponseEntity.ok(Map.of("success", true, "status", "SIGNED",
                    "message", "Your demo signature was recorded. The bank must still complete final approval."));
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> adminSummaries(Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return ResponseEntity.ok(agreementService.adminSummaries());
    }

    @PostMapping("/{agreementId}/share-link")
    public ResponseEntity<?> renewShareLink(@PathVariable Long agreementId, Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        try {
            return ResponseEntity.ok(agreementService.renewShareLink(agreementId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{agreementId}/pdf")
    public ResponseEntity<?> adminPdf(@PathVariable Long agreementId, Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        try {
            return pdf(agreementService.adminPdf(agreementId), "neobank-signed-loan-agreement-" + agreementId + ".pdf");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private ResponseEntity<ByteArrayResource> pdf(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)) {
            return false;
        }
        return "ADMIN".equals(principal.scope()) || "MANAGER".equals(principal.scope());
    }

    public record SignRequest(String challengeId, String captchaAnswer, String signedName, boolean consent) {}
}
