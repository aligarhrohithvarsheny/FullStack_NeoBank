package com.neo.springapp.controller;

import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardAuditLog;
import com.neo.springapp.model.CreditCardServiceRequest;
import com.neo.springapp.model.CreditCardServiceRequestHistory;
import com.neo.springapp.model.CreditCardSettings;
import com.neo.springapp.model.CreditCardTransaction;
import com.neo.springapp.repository.CreditCardAuditLogRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.CreditCardServiceRequestHistoryRepository;
import com.neo.springapp.repository.CreditCardServiceRequestRepository;
import com.neo.springapp.repository.CreditCardSettingsRepository;
import com.neo.springapp.repository.CreditCardTransactionRepository;
import com.neo.springapp.service.UserSessionTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/credit-card-requests")
public class CreditCardServiceRequestController {

    private static final Set<String> REQUEST_TYPES = Set.of(
            "Card Replacement", "Credit Limit Increase", "Statement Request",
            "Billing Issue", "Reward Issue", "EMI Issue", "Transaction Dispute",
            "Address Update", "Card Closure");
    private static final Set<String> REQUEST_STATUSES = Set.of(
            "Under Review", "Investigation", "Approved", "Resolved", "Rejected");
    private static final Set<String> DISPUTE_CATEGORIES = Set.of(
            "Unauthorized transaction", "Duplicate transaction", "Incorrect amount",
            "Refund not received", "Merchant dispute", "Cash withdrawal issue");
    private static final Set<String> REPLACEMENT_REASONS = Set.of("Lost", "Stolen", "Damaged", "Expired");

    private final CreditCardRepository cardRepository;
    private final CreditCardServiceRequestRepository requestRepository;
    private final CreditCardServiceRequestHistoryRepository historyRepository;
    private final CreditCardSettingsRepository settingsRepository;
    private final CreditCardTransactionRepository transactionRepository;
    private final CreditCardAuditLogRepository auditLogRepository;

    public CreditCardServiceRequestController(
            CreditCardRepository cardRepository,
            CreditCardServiceRequestRepository requestRepository,
            CreditCardServiceRequestHistoryRepository historyRepository,
            CreditCardSettingsRepository settingsRepository,
            CreditCardTransactionRepository transactionRepository,
            CreditCardAuditLogRepository auditLogRepository) {
        this.cardRepository = cardRepository;
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.settingsRepository = settingsRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/configuration")
    public ResponseEntity<?> getCustomerConfiguration(Authentication authentication) {
        UserSessionTokenService.SessionPrincipal principal = userPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BigDecimal ceiling = settingsRepository.findAll().stream().findFirst()
                .map(CreditCardSettings::getCreditLimitIncreaseCeiling).orElse(null);
        return ResponseEntity.ok(Map.of("creditLimitIncreaseEnabled", ceiling != null,
                "creditLimitIncreaseCeiling", ceiling == null ? BigDecimal.ZERO : ceiling));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createRequest(
            @RequestBody CreateRequest body,
            Authentication authentication) {
        UserSessionTokenService.SessionPrincipal principal = userPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (body == null || body.creditCardId() == null || body.requestType() == null
                || !REQUEST_TYPES.contains(body.requestType())) {
            return ResponseEntity.badRequest().body(Map.of("message", "A valid card and request type are required"));
        }
        if ("Transaction Dispute".equals(body.requestType())
                && (body.category() == null || !DISPUTE_CATEGORIES.contains(body.category()))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Select a valid transaction dispute category"));
        }
        if ("Card Replacement".equals(body.requestType())
                && (body.category() == null || !REPLACEMENT_REASONS.contains(body.category()))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Select a valid card replacement reason"));
        }
        boolean reportedLostOrStolen = "Card Replacement".equals(body.requestType())
                && Set.of("Lost", "Stolen").contains(body.category());
        if (!Set.of("Transaction Dispute", "Card Replacement").contains(body.requestType())
                && body.category() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Category is not supported for this request type"));
        }
        if ("Transaction Dispute".equals(body.requestType()) && body.transactionId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Select the transaction being disputed"));
        }
        if (!"Transaction Dispute".equals(body.requestType()) && body.transactionId() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "A transaction can only be linked to a dispute"));
        }

        CreditCard card = cardRepository.findById(body.creditCardId())
                .filter(candidate -> principal.accountNumber().equals(candidate.getAccountNumber()))
                .orElse(null);
        if (card == null) return ResponseEntity.notFound().build();
        if (body.transactionId() != null) {
            CreditCardTransaction transaction = transactionRepository.findById(body.transactionId())
                    .filter(candidate -> card.getId().equals(candidate.getCreditCardId()))
                    .orElse(null);
            if (transaction == null) return ResponseEntity.notFound().build();
        }

        String description = body.description() == null ? "" : body.description().trim();
        if (description.length() < 8 || description.length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Description must contain 8 to 2000 characters"));
        }
        if ("Credit Limit Increase".equals(body.requestType())) {
            BigDecimal ceiling = settingsRepository.findAll().stream().findFirst()
                    .map(CreditCardSettings::getCreditLimitIncreaseCeiling).orElse(null);
            BigDecimal requestedLimit = body.requestedLimit();
            if (ceiling == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("message", "Credit-limit increase requests are not currently configured"));
            }
            if (requestedLimit == null || requestedLimit.signum() <= 0
                    || card.getApprovedLimit() == null
                    || requestedLimit.compareTo(BigDecimal.valueOf(card.getApprovedLimit())) <= 0
                    || requestedLimit.compareTo(ceiling) > 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Requested limit must exceed the current approved limit and not exceed the bank-configured ceiling",
                        "ceiling", ceiling));
            }
        } else if (body.requestedLimit() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "A requested limit is only valid for a credit-limit increase"));
        }

        CreditCardServiceRequest request = new CreditCardServiceRequest();
        request.setCreditCardId(card.getId());
        request.setTransactionId(body.transactionId());
        request.setAccountNumber(card.getAccountNumber());
        request.setRequestType(body.requestType());
        request.setCategory(body.category());
        request.setDescription(description);
        request.setRequestedLimit(body.requestedLimit());
        request.setStatus("Submitted");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        CreditCardServiceRequest saved = requestRepository.save(request);
        if (reportedLostOrStolen) {
            String previousStatus = card.getStatus();
            card.setStatus("Blocked");
            card.setBlocked(true);
            card.setDeactivated(true);
            card.setOnlineTransactionsEnabled(false);
            card.setPosTransactionsEnabled(false);
            card.setContactlessEnabled(false);
            card.setAtmWithdrawalsEnabled(false);
            card.setInternationalTransactionsEnabled(false);
            cardRepository.save(card);

            CreditCardAuditLog audit = new CreditCardAuditLog();
            audit.setCreditCardId(card.getId());
            audit.setAccountNumber(card.getAccountNumber());
            audit.setAction("LOST_STOLEN_CARD_REPORTED_AND_LOCKED");
            audit.setPreviousValue(previousStatus == null ? "UNKNOWN" : previousStatus);
            audit.setNewValue("BLOCKED_ALL_CHANNELS");
            auditLogRepository.save(audit);
        }
        addHistory(saved.getId(), "Submitted",
                reportedLostOrStolen
                        ? "Lost/stolen report submitted; card locked and all channels disabled"
                        : "Request submitted",
                principal.accountNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(requestSummary(saved));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyRequests(Authentication authentication) {
        UserSessionTokenService.SessionPrincipal principal = userPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(requestRepository.findByAccountNumberOrderByCreatedAtDesc(principal.accountNumber())
                .stream().map(this::requestSummary).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().body(Map.of("message", "Page must be non-negative and size must be between 1 and 100"));
        }
        Page<CreditCardServiceRequest> requests = requestRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(Map.of(
                "content", requests.getContent().stream().map(this::requestSummary).toList(),
                "page", requests.getNumber(),
                "size", requests.getSize(),
                "totalElements", requests.getTotalElements(),
                "totalPages", requests.getTotalPages()));
    }

    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatus body,
            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (body == null || body.status() == null || !REQUEST_STATUSES.contains(body.status())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request status"));
        }
        CreditCardServiceRequest request = requestRepository.findById(id).orElse(null);
        if (request == null) return ResponseEntity.notFound().build();
        if (Set.of("Resolved", "Rejected", "Approved").contains(request.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "A completed request cannot be changed"));
        }
        String resolution = body.resolution() == null ? "" : body.resolution().trim();
        if (Set.of("Resolved", "Rejected", "Approved").contains(body.status())
                && resolution.length() < 5) {
            return ResponseEntity.badRequest().body(Map.of("message", "Provide a resolution of at least 5 characters"));
        }

        if ("Credit Limit Increase".equals(request.getRequestType()) && "Approved".equals(body.status())) {
            CreditCard card = cardRepository.findById(request.getCreditCardId()).orElse(null);
            BigDecimal ceiling = settingsRepository.findAll().stream().findFirst()
                    .map(CreditCardSettings::getCreditLimitIncreaseCeiling).orElse(null);
            if (card == null || ceiling == null || request.getRequestedLimit() == null
                    || request.getRequestedLimit().compareTo(ceiling) > 0
                    || card.getApprovedLimit() == null
                    || request.getRequestedLimit().compareTo(BigDecimal.valueOf(card.getApprovedLimit())) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "The requested limit is no longer valid for approval"));
            }
            double previousLimit = card.getApprovedLimit();
            card.setApprovedLimit(request.getRequestedLimit().doubleValue());
            card.calculateAvailableLimit();
            card.calculateUsageLimit();
            cardRepository.save(card);
            CreditCardAuditLog audit = new CreditCardAuditLog();
            audit.setCreditCardId(card.getId());
            audit.setAccountNumber(card.getAccountNumber());
            audit.setAction("CREDIT_LIMIT_INCREASE_APPROVED");
            audit.setPreviousValue(String.valueOf(previousLimit));
            audit.setNewValue(request.getRequestedLimit().toPlainString());
            auditLogRepository.save(audit);
        }

        request.setStatus(body.status());
        request.setResolution(resolution.isEmpty() ? null : resolution);
        request.setUpdatedAt(LocalDateTime.now());
        CreditCardServiceRequest saved = requestRepository.save(request);
        addHistory(saved.getId(), saved.getStatus(), resolution, staffName(authentication));
        return ResponseEntity.ok(requestSummary(saved));
    }

    private Map<String, Object> requestSummary(CreditCardServiceRequest request) {
        List<Map<String, Object>> timeline = historyRepository
                .findByServiceRequestIdOrderByCreatedAtAsc(request.getId()).stream()
                .map(event -> Map.<String, Object>of(
                        "status", event.getStatus(),
                        "note", event.getNote() == null ? "" : event.getNote(),
                        "createdAt", event.getCreatedAt()))
                .toList();
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", request.getId());
        summary.put("requestNumber", request.getRequestNumber());
        summary.put("creditCardId", request.getCreditCardId());
        summary.put("transactionId", request.getTransactionId());
        summary.put("accountNumber", request.getAccountNumber());
        summary.put("requestType", request.getRequestType());
        summary.put("category", request.getCategory() == null ? "" : request.getCategory());
        summary.put("description", request.getDescription() == null ? "" : request.getDescription());
        summary.put("requestedLimit", request.getRequestedLimit() == null ? BigDecimal.ZERO : request.getRequestedLimit());
        summary.put("status", request.getStatus());
        summary.put("resolution", request.getResolution() == null ? "" : request.getResolution());
        summary.put("createdAt", request.getCreatedAt());
        summary.put("updatedAt", request.getUpdatedAt());
        summary.put("timeline", timeline);
        return summary;
    }

    private void addHistory(Long requestId, String status, String note, String updatedBy) {
        CreditCardServiceRequestHistory history = new CreditCardServiceRequestHistory();
        history.setServiceRequestId(requestId);
        history.setStatus(status);
        history.setNote(note);
        history.setUpdatedBy(updatedBy);
        history.setCreatedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    private UserSessionTokenService.SessionPrincipal userPrincipal(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal)
                || !"USER".equals(principal.scope())) return null;
        return principal;
    }

    private boolean isStaff(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal
                && ("ADMIN".equals(principal.scope()) || "MANAGER".equals(principal.scope()));
    }

    private String staffName(Authentication authentication) {
        return authentication.getName() == null ? "Staff" : authentication.getName();
    }

    public record CreateRequest(
            Long creditCardId,
            String requestType,
            String category,
            String description,
            BigDecimal requestedLimit,
            Long transactionId) {}
    public record UpdateStatus(String status, String resolution) {}
}
