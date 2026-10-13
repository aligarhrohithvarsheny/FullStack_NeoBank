package com.neo.springapp.service;

import com.neo.springapp.model.EmergencyControl;
import com.neo.springapp.model.EmergencyControlAudit;
import com.neo.springapp.model.Card;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.EmergencyControlAuditRepository;
import com.neo.springapp.repository.EmergencyControlRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EmergencyControlService {
    public static final List<String> SUPPORTED_RAILS = List.of("IMPS", "NEFT", "SEPA", "ACH", "RTGS");
    private static final Set<String> VALID_RAILS = Set.copyOf(SUPPORTED_RAILS);

    private final EmergencyControlRepository controlRepository;
    private final EmergencyControlAuditRepository auditRepository;
    private final CardRepository cardRepository;
    private final SalaryAccountRepository salaryAccountRepository;

    public EmergencyControlService(
            EmergencyControlRepository controlRepository,
            EmergencyControlAuditRepository auditRepository,
            CardRepository cardRepository,
            SalaryAccountRepository salaryAccountRepository) {
        this.controlRepository = controlRepository;
        this.auditRepository = auditRepository;
        this.cardRepository = cardRepository;
        this.salaryAccountRepository = salaryAccountRepository;
    }

    @Transactional(readOnly = true)
    public boolean isOutboundBlocked(String accountNumber, String rail) {
        String normalizedRail = normalize(rail);
        String normalizedAccount = normalize(accountNumber);
        return isActive("RAIL", normalizedRail)
                || isActive("ACCOUNT", normalizedAccount);
    }

    @Transactional(readOnly = true)
    public boolean isAccountIsolated(String accountNumber) {
        String normalizedAccount = normalize(accountNumber);
        return !normalizedAccount.isBlank() && isActive("ACCOUNT", normalizedAccount);
    }

    public void assertAccountNotIsolated(String accountNumber) {
        if (isAccountIsolated(accountNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED, "This account is isolated by the bank and cannot make outgoing payments.");
        }
    }

    public void assertOutboundAllowed(String accountNumber, String rail) {
        if (isOutboundBlocked(accountNumber, rail)) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED, "Outbound " + normalize(rail) + " transfers are paused for this account or rail.");
        }
    }

    @Transactional(readOnly = true)
    public List<EmergencyControl> getActiveControls(String scopeType) {
        return controlRepository.findByScopeTypeAndActiveTrueOrderByUpdatedAtDesc(scopeType);
    }

    @Transactional(readOnly = true)
    public List<EmergencyControl> getAllControls() {
        return controlRepository.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public EmergencyControl getControl(String scopeType, String scopeKey) {
        return controlRepository.findByScopeTypeAndScopeKey(scopeType, normalize(scopeKey)).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EmergencyControlAudit> getRecentAudit() {
        return auditRepository.findTop50ByOrderByChangedAtDesc();
    }

    @Transactional
    public void recordSavingsProductChange(
            String accountNumber, String previousProductCode, String newProductCode,
            String reason, String changedBy) {
        EmergencyControlAudit audit = new EmergencyControlAudit();
        audit.setScopeType("SAVINGS_PRODUCT");
        audit.setScopeKey(normalize(accountNumber) + ":"
                + (previousProductCode == null || previousProductCode.isBlank() ? "unassigned" : previousProductCode)
                + ">" + newProductCode);
        audit.setAction("PRODUCT_CHANGED");
        audit.setPreviousState(previousProductCode != null && !previousProductCode.isBlank());
        audit.setNewState(true);
        audit.setReason(reason.trim());
        audit.setChangedBy(changedBy);
        auditRepository.save(audit);
    }

    @Transactional
    public EmergencyControl setControl(
            String scopeType, String scopeKey, boolean active, String reason, String changedBy) {
        return setControl(scopeType, scopeKey, active, reason, changedBy, null);
    }

    @Transactional
    public EmergencyControl setControl(
            String scopeType, String scopeKey, boolean active, String reason, String changedBy, String priorState) {
        String normalizedType = scopeType.trim().toUpperCase(Locale.ROOT);
        String normalizedKey = normalize(scopeKey);
        if (!normalizedType.equals("RAIL") && !normalizedType.equals("ACCOUNT")
                && !normalizedType.equals("CARD") && !normalizedType.equals("FEATURE")) {
            throw new IllegalArgumentException("Unsupported emergency-control scope.");
        }
        if (normalizedType.equals("RAIL") && !VALID_RAILS.contains(normalizedKey)) {
            throw new IllegalArgumentException("Unsupported outbound rail.");
        }
        if (normalizedKey.isBlank() || reason == null || reason.isBlank() || reason.length() > 500) {
            throw new IllegalArgumentException("A valid target and reason (up to 500 characters) are required.");
        }
        if (changedBy == null || changedBy.isBlank()) {
            throw new IllegalArgumentException("The authenticated admin identity is unavailable.");
        }

        EmergencyControl control = controlRepository.findByScopeTypeAndScopeKey(normalizedType, normalizedKey)
                .orElseGet(() -> {
                    EmergencyControl created = new EmergencyControl();
                    created.setScopeType(normalizedType);
                    created.setScopeKey(normalizedKey);
                    created.setActive(false);
                    created.setChangedBy(changedBy);
                    return created;
                });
        boolean previousState = control.isActive();
        if (normalizedType.equals("CARD") && active && !previousState) {
            control.setPriorState(priorState);
        }
        control.setActive(active);
        control.setReason(reason.trim());
        control.setChangedBy(changedBy);
        EmergencyControl saved = controlRepository.save(control);

        if (previousState != active) {
            EmergencyControlAudit audit = new EmergencyControlAudit();
            audit.setScopeType(normalizedType);
            audit.setScopeKey(normalizedKey);
            audit.setAction(normalizedType.equals("ACCOUNT")
                    ? (active ? "ISOLATE" : "RELEASE")
                    : normalizedType.equals("CARD")
                    ? (active ? "FREEZE" : "UNFREEZE")
                    : normalizedType.equals("FEATURE")
                    ? (active ? "FEATURE_DISABLE" : "FEATURE_ENABLE")
                    : (active ? "PAUSE" : "RESUME"));
            audit.setPreviousState(previousState);
            audit.setNewState(active);
            audit.setReason(reason.trim());
            audit.setChangedBy(changedBy);
            auditRepository.save(audit);
        }

        if (normalizedType.equals("CARD") && previousState != active) {
            if (normalizedKey.startsWith("SALARY:")) {
                String accountNumber = normalizedKey.substring("SALARY:".length());
                var salaryAccount = salaryAccountRepository.findByAccountNumber(accountNumber);
                if (salaryAccount == null) {
                    throw new IllegalArgumentException("Salary account card not found.");
                }
                if (active) {
                    salaryAccount.setDebitCardStatus("Blocked");
                } else {
                    salaryAccount.setDebitCardStatus(control.getPriorState() == null
                            ? "Active" : control.getPriorState());
                    control.setPriorState(null);
                    controlRepository.save(control);
                }
                salaryAccountRepository.save(salaryAccount);
            } else {
                Long cardId;
                try {
                    cardId = Long.valueOf(normalizedKey);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Invalid card id.");
                }
                Card card = cardRepository.findById(cardId)
                        .orElseThrow(() -> new IllegalArgumentException("Card not found."));
                if (active) {
                    card.setBlocked(true);
                    card.setStatus("Blocked");
                } else {
                    String restoreState = control.getPriorState();
                    card.setBlocked("BLOCKED".equals(restoreState));
                    card.setDeactivated("DEACTIVATED".equals(restoreState));
                    card.setStatus("BLOCKED".equals(restoreState) ? "Blocked"
                            : "DEACTIVATED".equals(restoreState) ? "Deactivated" : "Active");
                    control.setPriorState(null);
                    controlRepository.save(control);
                }
                cardRepository.save(card);
            }
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean isCardFrozen(String cardKey) {
        return isActive("CARD", normalize(cardKey));
    }

    private boolean isActive(String scopeType, String scopeKey) {
        if (scopeKey.isBlank()) return false;
        return controlRepository.findByScopeTypeAndScopeKey(scopeType, scopeKey)
                .map(EmergencyControl::isActive)
                .orElse(false);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
