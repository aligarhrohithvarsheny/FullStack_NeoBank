package com.neo.springapp.service;

import com.neo.springapp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomerEventNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerEventNotificationService.class);

    private final UserService userService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final SystemMonitoringService monitoringService;
    private final ApplicationEventPublisher eventPublisher;

    public CustomerEventNotificationService(UserService userService, PdfService pdfService,
                                            EmailService emailService, SystemMonitoringService monitoringService,
                                            ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.monitoringService = monitoringService;
        this.eventPublisher = eventPublisher;
    }

    public void notifyForAccount(String accountNumber, String event, String status,
                                 String reference, Map<String, String> details) {
        Optional<User> user;
        try {
            user = userService.getUserByAccountNumber(accountNumber);
        } catch (RuntimeException e) {
            recordFailure("Customer registered email lookup failed", e);
            return;
        }
        if (user.isEmpty() || user.get().getEmail() == null || user.get().getEmail().isBlank()) {
            logger.warn("Customer event notification not queued: no registered email for account reference {}",
                    mask(accountNumber));
            recordFailure("Customer event notification not queued because no registered email was found");
            return;
        }
        notifyToEmail(user.get().getEmail(), event, status, reference, details);
    }

    public void notifyToEmail(String email, String event, String status,
                              String reference, Map<String, String> details) {
        if (email == null || email.isBlank()) {
            logger.warn("Customer event notification not queued: registered email is unavailable");
            return;
        }
        Map<String, String> safeDetails = new LinkedHashMap<>();
        if (details != null) {
            details.forEach((key, value) -> safeDetails.put(key, value == null ? "N/A" : value));
        }
        try {
            eventPublisher.publishEvent(new NotificationRequest(
                    email.trim(), event, status, reference, safeDetails));
        } catch (RuntimeException e) {
            recordFailure("Customer event notification dispatch failed", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deliver(NotificationRequest request) {
        try {
            byte[] pdf = pdfService.generateCustomerEventSummary(
                    request.event(), request.status(), request.reference(), request.details());
            String subject = "NeoBank - " + request.event() + " " + request.status();
            String body = "Dear Customer,\n\n"
                    + "This is to confirm an update to your NeoBank account.\n"
                    + "Event: " + request.event() + "\n"
                    + "Status: " + request.status() + "\n"
                    + "Reference: " + request.reference() + "\n\n"
                    + "A PDF summary is attached. If you did not request this activity, please contact NeoBank using the official support channels.";
            String fileName = filePart(request.event()) + "_"
                    + filePart(request.reference()) + ".pdf";
            if (!emailService.sendEmailWithPdfAttachment(
                    request.email(), subject, body, fileName, pdf)) {
                logger.error("Customer event notification delivery failed for event {} and reference {}",
                        request.event(), request.reference());
            }
        } catch (IOException | RuntimeException e) {
            recordFailure("Customer event notification could not be generated for event "
                    + request.event() + " and reference " + request.reference(), e);
        }
    }

    private void recordFailure(String message, Exception e) {
        logger.error(message, e);
        try {
            monitoringService.recordNotificationFailure("Customer event PDF email");
        } catch (RuntimeException monitoringException) {
            logger.error("Could not record customer notification failure in monitoring", monitoringException);
        }
    }

    private void recordFailure(String message) {
        logger.error(message);
        try {
            monitoringService.recordNotificationFailure("Customer event PDF email");
        } catch (RuntimeException monitoringException) {
            logger.error("Could not record customer notification failure in monitoring", monitoringException);
        }
    }

    private String filePart(String value) {
        String normalized = Normalizer.normalize(value == null ? "notification" : value, Normalizer.Form.NFKD);
        String safe = normalized.replaceAll("[^A-Za-z0-9_-]", "_");
        return safe.isBlank() ? "notification" : safe.substring(0, Math.min(safe.length(), 48));
    }

    private static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    public record NotificationRequest(String email, String event, String status,
                                     String reference, Map<String, String> details) {}
}
