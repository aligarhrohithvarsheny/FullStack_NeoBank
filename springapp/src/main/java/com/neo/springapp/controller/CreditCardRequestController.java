package com.neo.springapp.controller;

import com.neo.springapp.model.CreditCardRequest;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.UserRepository;
import com.neo.springapp.service.CreditCardRequestService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credit-card-requests")
public class CreditCardRequestController {

    private final CreditCardRequestService service;
    private final UserRepository userRepository;
    private final SalaryAccountRepository salaryAccountRepository;

    public CreditCardRequestController(CreditCardRequestService service, UserRepository userRepository,
                                       SalaryAccountRepository salaryAccountRepository) {
        this.service = service;
        this.userRepository = userRepository;
        this.salaryAccountRepository = salaryAccountRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestBody ApplicationSubmission body,
            Authentication authentication) {
        SessionPrincipal principal = userPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Application details are required."));
        }
        CreditCardRequest request = applicationForAccount(principal.accountNumber(), body);
        if (request == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only approved, active customer accounts can apply for a credit card."));
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.saveRequest(request, body.annualIncome()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping(value = "/guest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createGuest(
            @RequestParam("userName") String userName,
            @RequestParam("userEmail") String userEmail,
            @RequestParam("mobileNumber") String mobileNumber,
            @RequestParam("pan") String pan,
            @RequestParam("aadhaarLastFour") String aadhaarLastFour,
            @RequestParam("dateOfBirth") String dateOfBirth,
            @RequestParam("annualIncome") Double annualIncome,
            @RequestParam("employmentType") String employmentType,
            @RequestParam("employerName") String employerName,
            @RequestParam("designation") String designation,
            @RequestParam("address") String address,
            @RequestParam(value = "consent", defaultValue = "false") boolean consent,
            @RequestParam("panDocument") MultipartFile panDocument,
            @RequestParam("aadhaarDocument") MultipartFile aadhaarDocument,
            @RequestParam("selfie") MultipartFile selfie) {
        CreditCardRequest request = new CreditCardRequest();
        request.setUserName(userName);
        request.setUserEmail(userEmail);
        request.setPan(pan);
        request.setAnnualIncome(annualIncome);
        try {
            CreditCardRequest saved = service.createGuestApplication(request, aadhaarLastFour, address,
                    mobileNumber, dateOfBirth, employmentType, employerName, designation,
                    consent, panDocument, aadhaarDocument, selfie);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "applicationId", saved.getApplicationId(),
                    "status", saved.getStatus(),
                    "requestDate", saved.getRequestDate(),
                    "predictedCibil", saved.getPredictedCibil(),
                    "rejectionReason", saved.getRejectionReason() == null ? "" : saved.getRejectionReason(),
                    "scoreNotice", "Simulated estimate based on PAN pattern and declared income; not a bureau score."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/track")
    public ResponseEntity<?> trackGuest(@RequestBody TrackingRequest body) {
        if (body == null) return ResponseEntity.badRequest().build();
        CreditCardRequest request = service.trackGuestApplication(body.applicationId(), body.pan(), body.mobileNumber());
        if (request == null) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Application not found. Check the application ID, PAN, and mobile number."));
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("applicationId", request.getApplicationId());
        status.put("status", request.getStatus());
        status.put("requestDate", request.getRequestDate());
        status.put("processedDate", request.getProcessedDate());
        status.put("approvedLimit", request.getApprovedLimit());
        status.put("predictedCibil", request.getPredictedCibil());
        status.put("rejectionReason", request.getRejectionReason());
        status.put("scoreNotice", "Simulated estimate based on PAN pattern and declared income; not a bureau score.");
        boolean approved = "Approved".equalsIgnoreCase(request.getStatus());
        status.put("cardIssued", approved && (request.getAccountNumber() != null
                || request.getGuestCardNumber() != null));
        status.put("cardDetailsAvailable", approved && request.getGuestCardNumber() != null);
        if (request.getGuestCardNumber() != null) {
            status.put("maskedCardNumber", "**** **** **** " + request.getGuestCardNumber().substring(12));
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/agreement/{token}")
    public ResponseEntity<?> creditCardAgreementDetails(@PathVariable String token) {
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(service.creditCardAgreementDetails(token));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/agreement/{token}/captcha")
    public ResponseEntity<?> creditCardAgreementCaptcha(@PathVariable String token) {
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(service.createCreditCardAgreementCaptcha(token));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/agreement/{token}/sign")
    public ResponseEntity<?> signCreditCardAgreement(
            @PathVariable String token, @RequestBody AgreementSignRequest body,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (body == null) return ResponseEntity.badRequest().build();
        try {
            CreditCardRequestService.AgreementSignResult result = service.signCreditCardAgreement(
                    token, body.challengeId(), body.captchaAnswer(), body.signedName(), body.consent(),
                    httpRequest.getRemoteAddr());
            if (!result.signed()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "CAPTCHA is incorrect or expired. Refresh the CAPTCHA and try again."));
            }
            return ResponseEntity.ok(Map.of(
                    "status", "Approved",
                    "cardIssued", true,
                    "cardDetailsAvailable", result.guestCard(),
                    "message", "Your demo signature was recorded and the approved card was generated."));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/guest/card-details/request-otp")
    public ResponseEntity<?> requestGuestCardDetailsOtp(@RequestBody TrackingRequest body) {
        if (body == null) return ResponseEntity.badRequest().build();
        try {
            String maskedEmail = service.requestGuestCardDetailsOtp(
                    body.applicationId(), body.pan(), body.mobileNumber());
            return ResponseEntity.ok(Map.of(
                    "message", "A verification code was sent to " + maskedEmail + ".",
                    "expiresInSeconds", 120));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/guest/card-details/verify-otp")
    public ResponseEntity<?> verifyGuestCardDetailsOtp(@RequestBody CardDetailsOtpRequest body) {
        if (body == null) return ResponseEntity.badRequest().build();
        try {
            Map<String, Object> details = service.verifyGuestCardDetailsOtp(
                    body.applicationId(), body.pan(), body.mobileNumber(), body.otp());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(details);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/companies")
    public ResponseEntity<List<String>> companySuggestions(
            @RequestParam(defaultValue = "") String query) {
        String normalizedQuery = query.trim();
        if (normalizedQuery.length() < 2) return ResponseEntity.ok(List.of());
        if (normalizedQuery.length() > 100) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(salaryAccountRepository.findCompanyNameSuggestions(normalizedQuery).stream()
                .limit(30).toList());
    }

    @PostMapping("/admin/create")
    public ResponseEntity<?> createForCustomer(
            @RequestBody AdminApplicationSubmission body,
            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (body == null || body.accountNumber() == null || body.accountNumber().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "A customer account number is required."));
        }
        CreditCardRequest request = applicationForAccount(body.accountNumber(), new ApplicationSubmission(
                body.pan(), body.annualIncome()));
        if (request == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "The selected customer account is not eligible to apply."));
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.saveRequest(request, body.annualIncome()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> byStatus(@PathVariable String status, Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(service.getRequestsByStatus(status));
    }

    @PutMapping("/admin/update/{id}")
    public ResponseEntity<?> updateApplication(
            @PathVariable Long id,
            @RequestBody GuestApplicationUpdate body,
            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (body == null) return ResponseEntity.badRequest().body(Map.of("message", "Application details are required."));
        try {
            CreditCardRequest updated = service.updateGuestApplication(id,
                    new CreditCardRequestService.GuestApplicationUpdate(
                            body.userName(), body.userEmail(), body.pan(), body.annualIncome(),
                            body.mobileNumber(), body.dateOfBirth(), body.aadhaarLastFour(),
                            body.address(), body.employmentType(), body.employerName(), body.designation()));
            return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/admin/{id}/documents/{documentType}")
    public ResponseEntity<?> getApplicationDocument(
            @PathVariable Long id,
            @PathVariable String documentType,
            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            Resource document = service.getApplicationDocument(id, documentType);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(service.getApplicationDocumentContentType(document)))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + documentType + "-document\"")
                    .body(document);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestBody ApprovalRequest body,
            Authentication authentication) {
        SessionPrincipal principal = staffPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "An approved credit limit is required."));
        }
        try {
            CreditCardRequestService.ApprovalResult result =
                    service.approveRequest(id, String.valueOf(principal.userId()), body.approvedLimit());
            if (result == null) return ResponseEntity.notFound().build();
            if ("Rejected".equalsIgnoreCase(result.application().getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "message", result.application().getRejectionReason(),
                        "application", result.application()));
            }
            return ResponseEntity.ok(Map.of(
                    "application", result.application(),
                    "shareUrl", result.shareUrl(),
                    "expiresAt", result.expiresAt(),
                    "emailSent", result.emailSent()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/admin/{id}/agreement-link")
    public ResponseEntity<?> renewCreditCardAgreementLink(
            @PathVariable Long id, Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            CreditCardRequestService.AgreementLinkResult result = service.renewCreditCardAgreementLink(id);
            return ResponseEntity.ok(Map.of(
                    "shareUrl", result.shareUrl(),
                    "expiresAt", result.expiresAt(),
                    "emailSent", result.emailSent()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> reject(@PathVariable Long id, Authentication authentication) {
        SessionPrincipal principal = staffPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            CreditCardRequest rejected = service.rejectRequest(id, String.valueOf(principal.userId()));
            return rejected != null ? ResponseEntity.ok(rejected) : ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(Authentication authentication) {
        SessionPrincipal principal = userPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(
                service.getRequestsByAccountNumber(principal.accountNumber()));
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam String pan, @RequestParam(required = false) Double income) {
        if (pan == null || !pan.trim().matches("(?i)[A-Z]{5}[0-9]{4}[A-Z]")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Enter a valid 10-character PAN."));
        }
        if (income == null || !Double.isFinite(income) || income <= 0 || income > 1_000_000_000d) {
            return ResponseEntity.badRequest().body(Map.of("message", "Enter a valid annual income."));
        }
        return ResponseEntity.ok(service.getCreditCardPrediction(
                pan.trim().toUpperCase(java.util.Locale.ROOT), income));
    }

    private SessionPrincipal userPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)
                || !"USER".equals(principal.scope()) || principal.accountNumber() == null) return null;
        return principal;
    }

    private SessionPrincipal staffPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)
                || !("ADMIN".equals(principal.scope()) || "MANAGER".equals(principal.scope()))) return null;
        return principal;
    }

    private boolean isStaff(Authentication authentication) {
        return staffPrincipal(authentication) != null;
    }

    private CreditCardRequest applicationForAccount(String accountNumber, ApplicationSubmission body) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        CreditCardRequest request = new CreditCardRequest();
        request.setAccountNumber(accountNumber);
        if (user != null && "APPROVED".equalsIgnoreCase(user.getStatus())
                && (user.getAccount() == null || "ACTIVE".equalsIgnoreCase(user.getAccount().getStatus()))) {
            request.setUserName(user.getAccount() != null && user.getAccount().getName() != null
                    ? user.getAccount().getName() : user.getUsername());
            request.setUserEmail(user.getEmail());
        } else {
            SalaryAccount salaryAccount = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salaryAccount == null || !"ACTIVE".equalsIgnoreCase(salaryAccount.getStatus())) return null;
            request.setUserName(salaryAccount.getEmployeeName());
            request.setUserEmail(salaryAccount.getEmail());
        }
        request.setPan(body.pan());
        return request;
    }

    public record ApplicationSubmission(String pan, Double annualIncome) {}
    public record AdminApplicationSubmission(String accountNumber, String pan, Double annualIncome) {}
    public record ApprovalRequest(Double approvedLimit) {}
    public record TrackingRequest(String applicationId, String pan, String mobileNumber) {}
    public record AgreementSignRequest(String challengeId, String captchaAnswer, String signedName, boolean consent) {}
    public record CardDetailsOtpRequest(String applicationId, String pan, String mobileNumber, String otp) {}
    public record GuestApplicationUpdate(String userName, String userEmail, String pan, Double annualIncome,
                                         String mobileNumber, String dateOfBirth, String aadhaarLastFour,
                                         String address, String employmentType, String employerName,
                                         String designation) {}
}
