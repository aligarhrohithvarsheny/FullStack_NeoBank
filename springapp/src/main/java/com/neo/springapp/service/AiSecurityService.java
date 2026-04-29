package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Security Service - Core AI-powered security engine for NeoBank.
 * Provides multi-channel threat detection, risk scoring, anomaly detection,
 * device fingerprinting, and adaptive security across all banking channels.
 */
@Service
public class AiSecurityService {

    private static final Logger log = LoggerFactory.getLogger(AiSecurityService.class);

    private final AiSecurityEventRepository eventRepository;
    private final AiThreatScoreRepository threatScoreRepository;
    private final AiDeviceFingerprintRepository deviceFingerprintRepository;
    private final AiSecurityRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;

    // AI Model thresholds
    private static final double HIGH_RISK_THRESHOLD = 70.0;
    private static final double CRITICAL_RISK_THRESHOLD = 85.0;
    private static final int MAX_FAILED_LOGINS = 3;
    private static final int RAPID_TRANSACTION_WINDOW_MINUTES = 10;
    private static final int RAPID_TRANSACTION_THRESHOLD = 5;
    private static final double HIGH_AMOUNT_THRESHOLD = 100000.0;

    public AiSecurityService(AiSecurityEventRepository eventRepository,
                             AiThreatScoreRepository threatScoreRepository,
                             AiDeviceFingerprintRepository deviceFingerprintRepository,
                             AiSecurityRuleRepository ruleRepository,
                             TransactionRepository transactionRepository) {
        this.eventRepository = eventRepository;
        this.threatScoreRepository = threatScoreRepository;
        this.deviceFingerprintRepository = deviceFingerprintRepository;
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
    }

    // ========================= REAL-TIME THREAT ANALYSIS =========================

    /**
     * AI-powered login risk analysis. Evaluates login attempt across multiple risk dimensions.
     * Returns a risk assessment with recommended action.
     */
    @Transactional
    public Map<String, Object> analyzeLoginAttempt(String entityId, String entityType,
                                                    String clientIp, String userAgent,
                                                    String deviceInfo, String location,
                                                    boolean loginSuccess) {
        Map<String, Object> assessment = new HashMap<>();
        double riskScore = 0.0;
        List<String> riskFactors = new ArrayList<>();

        // 1. Device fingerprint analysis
        String deviceHash = generateDeviceHash(userAgent, deviceInfo);
        AiDeviceFingerprint knownDevice = deviceFingerprintRepository
                .findByEntityIdAndDeviceHash(entityId, deviceHash).orElse(null);
        boolean isNewDevice = (knownDevice == null);

        if (isNewDevice) {
            riskScore += 25.0;
            riskFactors.add("New device detected");
            registerDevice(entityId, entityType, deviceHash, deviceInfo, userAgent, clientIp, location);
            createSecurityEvent(AiSecurityEvent.EventType.NEW_DEVICE_DETECTED,
                    AiSecurityEvent.Channel.WEB, AiSecurityEvent.Severity.MEDIUM,
                    riskScore, entityId, entityType,
                    "New device login detected",
                    "Login attempt from previously unseen device: " + deviceInfo,
                    clientIp, location, deviceHash, userAgent);
        } else {
            knownDevice.setLoginCount(knownDevice.getLoginCount() + 1);
            knownDevice.setLastSeenAt(LocalDateTime.now());
            knownDevice.setIpAddress(clientIp);
            deviceFingerprintRepository.save(knownDevice);
            if (Boolean.TRUE.equals(knownDevice.getIsTrusted())) {
                riskScore -= 10.0; // Bonus for trusted device
            }
        }

        // 2. Failed login pattern analysis (brute force detection)
        if (!loginSuccess) {
            List<AiSecurityEvent> recentFailedLogins = eventRepository
                    .findBySourceEntityIdOrderByCreatedAtDesc(entityId).stream()
                    .filter(e -> e.getEventType() == AiSecurityEvent.EventType.SUSPICIOUS_LOGIN
                            || e.getEventType() == AiSecurityEvent.EventType.BRUTE_FORCE_ATTACK)
                    .filter(e -> e.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(15)))
                    .collect(Collectors.toList());

            if (recentFailedLogins.size() >= MAX_FAILED_LOGINS) {
                riskScore += 40.0;
                riskFactors.add("Brute force pattern detected: " + recentFailedLogins.size() + " failed attempts in 15 min");
                createSecurityEvent(AiSecurityEvent.EventType.BRUTE_FORCE_ATTACK,
                        AiSecurityEvent.Channel.WEB, AiSecurityEvent.Severity.CRITICAL,
                        riskScore, entityId, entityType,
                        "Brute force attack detected",
                        "Multiple failed login attempts from: " + clientIp,
                        clientIp, location, deviceHash, userAgent);
            } else {
                riskScore += 15.0;
                riskFactors.add("Failed login attempt");
                createSecurityEvent(AiSecurityEvent.EventType.SUSPICIOUS_LOGIN,
                        AiSecurityEvent.Channel.WEB, AiSecurityEvent.Severity.LOW,
                        riskScore, entityId, entityType,
                        "Failed login attempt",
                        "Login failed from IP: " + clientIp,
                        clientIp, location, deviceHash, userAgent);
            }
        }

        // 3. Geo-anomaly detection (impossible travel)
        List<AiSecurityEvent> recentLogins = eventRepository
                .findBySourceEntityIdOrderByCreatedAtDesc(entityId).stream()
                .filter(e -> e.getLocation() != null && !e.getLocation().isEmpty())
                .filter(e -> e.getCreatedAt().isAfter(LocalDateTime.now().minusHours(2)))
                .limit(5)
                .collect(Collectors.toList());

        if (!recentLogins.isEmpty() && location != null) {
            for (AiSecurityEvent recent : recentLogins) {
                if (recent.getLocation() != null && !recent.getLocation().equals(location)) {
                    long minutesDiff = ChronoUnit.MINUTES.between(recent.getCreatedAt(), LocalDateTime.now());
                    if (minutesDiff < 120) { // Different location within 2 hours
                        riskScore += 35.0;
                        riskFactors.add("Impossible travel: location changed from " +
                                recent.getLocation() + " to " + location + " in " + minutesDiff + " min");
                        createSecurityEvent(AiSecurityEvent.EventType.IMPOSSIBLE_TRAVEL,
                                AiSecurityEvent.Channel.WEB, AiSecurityEvent.Severity.HIGH,
                                riskScore, entityId, entityType,
                                "Impossible travel detected",
                                "Location changed impossibly fast between " + recent.getLocation() + " and " + location,
                                clientIp, location, deviceHash, userAgent);
                        break;
                    }
                }
            }
        }

        // 4. Off-hours activity detection
        int hour = LocalDateTime.now().getHour();
        if (hour >= 0 && hour < 5) {
            riskScore += 10.0;
            riskFactors.add("Off-hours activity (between 12AM-5AM)");
        }

        // 5. Update threat score
        riskScore = Math.max(0, Math.min(100, riskScore));
        updateThreatScore(entityId, entityType, riskScore, "login", riskFactors);

        // Build assessment
        assessment.put("riskScore", riskScore);
        assessment.put("riskLevel", getRiskLevel(riskScore));
        assessment.put("riskFactors", riskFactors);
        assessment.put("isNewDevice", isNewDevice);
        assessment.put("recommendedAction", getRecommendedAction(riskScore));
        assessment.put("timestamp", LocalDateTime.now().toString());

        log.info("AI Login Analysis for {}: risk={}, level={}, factors={}",
                entityId, riskScore, getRiskLevel(riskScore), riskFactors.size());

        return assessment;
    }

    /**
     * AI-powered transaction risk analysis. Evaluates transaction patterns and anomalies.
     */
    @Transactional
    public Map<String, Object> analyzeTransaction(String accountNumber, String entityName,
                                                   Double amount, String recipientAccount,
                                                   String transactionType, Double currentBalance,
                                                   String clientIp, String location, String deviceInfo) {
        Map<String, Object> assessment = new HashMap<>();
        double riskScore = 0.0;
        List<String> riskFactors = new ArrayList<>();
        AiSecurityEvent.Channel channel = AiSecurityEvent.Channel.NET_BANKING;

        // 1. High value transfer detection
        if (amount != null && currentBalance != null && currentBalance > 0) {
            double balanceRatio = amount / currentBalance;
            if (balanceRatio > 0.5) {
                riskScore += 25.0;
                riskFactors.add("High-value transfer: " + String.format("%.1f%%", balanceRatio * 100) + " of balance");
            }
            if (amount >= HIGH_AMOUNT_THRESHOLD) {
                riskScore += 20.0;
                riskFactors.add("Large transaction amount: ₹" + String.format("%.2f", amount));
            }
        }

        // 2. Rapid-fire transaction detection
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RAPID_TRANSACTION_WINDOW_MINUTES);
        List<AiSecurityEvent> recentTxEvents = eventRepository
                .findBySourceEntityIdOrderByCreatedAtDesc(accountNumber).stream()
                .filter(e -> e.getEventType() == AiSecurityEvent.EventType.ANOMALOUS_TRANSACTION
                        || e.getEventType() == AiSecurityEvent.EventType.RAPID_FIRE_TRANSACTIONS)
                .filter(e -> e.getCreatedAt().isAfter(windowStart))
                .collect(Collectors.toList());

        if (recentTxEvents.size() >= RAPID_TRANSACTION_THRESHOLD) {
            riskScore += 35.0;
            riskFactors.add("Rapid-fire transactions: " + recentTxEvents.size() + " events in " + RAPID_TRANSACTION_WINDOW_MINUTES + " min");
            createSecurityEvent(AiSecurityEvent.EventType.RAPID_FIRE_TRANSACTIONS,
                    channel, AiSecurityEvent.Severity.CRITICAL,
                    riskScore, accountNumber, "USER",
                    "Rapid-fire transactions detected",
                    "Multiple transactions in short window from account " + accountNumber,
                    clientIp, location, null, null);
        }

        // 3. Statistical anomaly: deviation from user's average transaction amount
        try {
            Page<Transaction> recentTransactions = transactionRepository
                    .findByAccountNumberAndDateBetweenOrderByDateDesc(
                            accountNumber,
                            LocalDateTime.now().minusDays(90),
                            LocalDateTime.now(),
                            PageRequest.of(0, 100));

            if (recentTransactions.getTotalElements() > 5) {
                double avgAmount = recentTransactions.getContent().stream()
                        .filter(t -> t.getAmount() != null)
                        .mapToDouble(Transaction::getAmount)
                        .average().orElse(0.0);
                double stdDev = calculateStdDev(recentTransactions.getContent().stream()
                        .filter(t -> t.getAmount() != null)
                        .mapToDouble(Transaction::getAmount)
                        .toArray());

                if (stdDev > 0 && amount != null && (amount - avgAmount) > 2 * stdDev) {
                    riskScore += 20.0;
                    riskFactors.add("Statistical anomaly: amount ₹" + String.format("%.2f", amount) +
                            " is >2σ from average ₹" + String.format("%.2f", avgAmount));
                }
            }
        } catch (Exception e) {
            log.warn("Error analyzing transaction history for {}", accountNumber);
        }

        // 4. Off-hours transaction
        int hour = LocalDateTime.now().getHour();
        if (hour >= 0 && hour < 5) {
            riskScore += 10.0;
            riskFactors.add("Transaction during off-hours (12AM-5AM)");
        }

        riskScore = Math.max(0, Math.min(100, riskScore));

        // Create event if risk is elevated
        if (riskScore >= 30) {
            AiSecurityEvent.Severity severity = riskScore >= CRITICAL_RISK_THRESHOLD ?
                    AiSecurityEvent.Severity.CRITICAL : riskScore >= HIGH_RISK_THRESHOLD ?
                    AiSecurityEvent.Severity.HIGH : AiSecurityEvent.Severity.MEDIUM;

            String detailsJson = String.format(
                    "{\"amount\":%.2f,\"balance\":%.2f,\"recipient\":\"%s\",\"type\":\"%s\",\"riskScore\":%.1f}",
                    amount != null ? amount : 0, currentBalance != null ? currentBalance : 0,
                    recipientAccount != null ? recipientAccount : "",
                    transactionType != null ? transactionType : "", riskScore);

            createSecurityEvent(AiSecurityEvent.EventType.ANOMALOUS_TRANSACTION,
                    channel, severity, riskScore, accountNumber, "USER",
                    "Anomalous transaction detected",
                    "AI detected suspicious transaction pattern: " + String.join("; ", riskFactors),
                    clientIp, location, null, null);
        }

        updateThreatScore(accountNumber, "USER", riskScore, "transaction", riskFactors);

        assessment.put("riskScore", riskScore);
        assessment.put("riskLevel", getRiskLevel(riskScore));
        assessment.put("riskFactors", riskFactors);
        assessment.put("recommendedAction", getRecommendedAction(riskScore));
        assessment.put("shouldBlock", riskScore >= CRITICAL_RISK_THRESHOLD);
        assessment.put("requiresMFA", riskScore >= HIGH_RISK_THRESHOLD);
        assessment.put("timestamp", LocalDateTime.now().toString());

        return assessment;
    }

    /**
     * AI behavioral analysis - monitors typing patterns, mouse movements, session behavior.
     */
    @Transactional
    public Map<String, Object> analyzeBehavior(String entityId, String entityType,
                                                Double typingSpeedWpm, Double mouseMovementScore,
                                                Double sessionDurationMinutes, Integer pagesVisited,
                                                String clientIp, String deviceInfo) {
        Map<String, Object> assessment = new HashMap<>();
        double riskScore = 0.0;
        List<String> riskFactors = new ArrayList<>();

        // 1. Typing speed anomaly (bots type too fast or too consistently)
        if (typingSpeedWpm != null) {
            if (typingSpeedWpm > 150) {
                riskScore += 30.0;
                riskFactors.add("Abnormally fast typing: " + typingSpeedWpm + " WPM (possible bot)");
            } else if (typingSpeedWpm < 5) {
                riskScore += 15.0;
                riskFactors.add("Abnormally slow typing: " + typingSpeedWpm + " WPM (possible automated tool)");
            }
        }

        // 2. Mouse movement analysis (bots have linear or no movement)
        if (mouseMovementScore != null) {
            if (mouseMovementScore < 10) {
                riskScore += 25.0;
                riskFactors.add("Minimal mouse movement: score " + mouseMovementScore + " (possible bot/automation)");
            }
        }

        // 3. Session duration anomaly
        if (sessionDurationMinutes != null) {
            if (sessionDurationMinutes < 0.5) {
                riskScore += 20.0;
                riskFactors.add("Extremely short session: " + sessionDurationMinutes + " min (possible automated scraping)");
            } else if (sessionDurationMinutes > 480) {
                riskScore += 10.0;
                riskFactors.add("Abnormally long session: " + sessionDurationMinutes + " min");
            }
        }

        // 4. Rapid page navigation (scraping/bot indicator)
        if (pagesVisited != null && sessionDurationMinutes != null && sessionDurationMinutes > 0) {
            double pagesPerMinute = pagesVisited / sessionDurationMinutes;
            if (pagesPerMinute > 10) {
                riskScore += 30.0;
                riskFactors.add("Rapid page navigation: " + String.format("%.1f", pagesPerMinute) + " pages/min (possible bot)");
            }
        }

        riskScore = Math.max(0, Math.min(100, riskScore));

        if (riskScore >= 30) {
            AiSecurityEvent.Severity severity = riskScore >= CRITICAL_RISK_THRESHOLD ?
                    AiSecurityEvent.Severity.CRITICAL : riskScore >= HIGH_RISK_THRESHOLD ?
                    AiSecurityEvent.Severity.HIGH : AiSecurityEvent.Severity.MEDIUM;

            createSecurityEvent(AiSecurityEvent.EventType.ACCOUNT_TAKEOVER,
                    AiSecurityEvent.Channel.WEB, severity, riskScore,
                    entityId, entityType,
                    "Behavioral anomaly detected",
                    "AI behavioral analysis flagged: " + String.join("; ", riskFactors),
                    clientIp, null, null, null);
        }

        updateThreatScore(entityId, entityType, riskScore, "behavioral", riskFactors);

        assessment.put("riskScore", riskScore);
        assessment.put("riskLevel", getRiskLevel(riskScore));
        assessment.put("riskFactors", riskFactors);
        assessment.put("isBotLikely", riskScore >= 50);
        assessment.put("timestamp", LocalDateTime.now().toString());

        return assessment;
    }

    // ========================= DASHBOARD & ANALYTICS =========================

    /**
     * Get comprehensive AI security dashboard data.
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        LocalDateTime last7d = LocalDateTime.now().minusDays(7);
        LocalDateTime last30d = LocalDateTime.now().minusDays(30);

        // Summary metrics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEvents24h", eventRepository.countSince(last24h));
        summary.put("totalEvents7d", eventRepository.countSince(last7d));
        summary.put("totalEvents30d", eventRepository.countSince(last30d));
        summary.put("criticalEvents24h", eventRepository.countBySeveritySince(AiSecurityEvent.Severity.CRITICAL, last24h));
        summary.put("highEvents24h", eventRepository.countBySeveritySince(AiSecurityEvent.Severity.HIGH, last24h));
        summary.put("activeThreats", eventRepository.countByStatus(AiSecurityEvent.Status.DETECTED));
        summary.put("blockedThreats", eventRepository.countByStatus(AiSecurityEvent.Status.BLOCKED));
        summary.put("resolvedThreats", eventRepository.countByStatus(AiSecurityEvent.Status.RESOLVED));
        summary.put("avgRiskScore", eventRepository.avgRiskScoreSince(last24h));
        dashboard.put("summary", summary);

        // Channel distribution
        List<Object[]> channelData = eventRepository.countByChannelSince(last7d);
        Map<String, Long> channelDistribution = new LinkedHashMap<>();
        for (Object[] row : channelData) {
            channelDistribution.put(row[0].toString(), (Long) row[1]);
        }
        dashboard.put("channelDistribution", channelDistribution);

        // Event type distribution
        List<Object[]> typeData = eventRepository.countByEventTypeSince(last7d);
        Map<String, Long> typeDistribution = new LinkedHashMap<>();
        for (Object[] row : typeData) {
            typeDistribution.put(row[0].toString(), (Long) row[1]);
        }
        dashboard.put("eventTypeDistribution", typeDistribution);

        // Severity distribution
        List<Object[]> severityData = eventRepository.countBySeverityGroupSince(last7d);
        Map<String, Long> severityDistribution = new LinkedHashMap<>();
        for (Object[] row : severityData) {
            severityDistribution.put(row[0].toString(), (Long) row[1]);
        }
        dashboard.put("severityDistribution", severityDistribution);

        // Daily trend (last 30 days)
        List<Object[]> dailyTrend = eventRepository.countDailyEventsSince(last30d);
        List<Map<String, Object>> trendData = new ArrayList<>();
        for (Object[] row : dailyTrend) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", row[0].toString());
            point.put("count", row[1]);
            trendData.add(point);
        }
        dashboard.put("dailyTrend", trendData);

        // High-risk entities
        List<AiThreatScore> highRisk = threatScoreRepository.findHighRiskEntities(HIGH_RISK_THRESHOLD);
        dashboard.put("highRiskEntities", highRisk.stream().limit(10).collect(Collectors.toList()));

        // Watchlisted entities
        List<AiThreatScore> watchlisted = threatScoreRepository.findByIsWatchlistedTrueOrderByOverallRiskScoreDesc();
        dashboard.put("watchlistedEntities", watchlisted);

        // Risk level counts
        Map<String, Long> riskLevelCounts = new LinkedHashMap<>();
        riskLevelCounts.put("LOW", threatScoreRepository.countByRiskLevel(AiThreatScore.RiskLevel.LOW));
        riskLevelCounts.put("MEDIUM", threatScoreRepository.countByRiskLevel(AiThreatScore.RiskLevel.MEDIUM));
        riskLevelCounts.put("HIGH", threatScoreRepository.countByRiskLevel(AiThreatScore.RiskLevel.HIGH));
        riskLevelCounts.put("CRITICAL", threatScoreRepository.countByRiskLevel(AiThreatScore.RiskLevel.CRITICAL));
        dashboard.put("riskLevelCounts", riskLevelCounts);

        // Active security rules
        List<AiSecurityRule> activeRules = ruleRepository.findByIsActiveTrueOrderByPriorityAsc();
        dashboard.put("activeRules", activeRules);
        dashboard.put("totalRules", ruleRepository.count());

        // AI model info
        Map<String, Object> aiInfo = new HashMap<>();
        aiInfo.put("modelVersion", "v1.0");
        aiInfo.put("engine", "NeoBank AI Security Engine");
        aiInfo.put("capabilities", Arrays.asList(
                "Real-time Login Threat Analysis",
                "Transaction Anomaly Detection",
                "Behavioral Biometric Analysis",
                "Device Fingerprinting & Trust Scoring",
                "Impossible Travel Detection",
                "Brute Force Attack Detection",
                "Session Hijack Detection",
                "Off-Hours Activity Monitoring",
                "Statistical Anomaly Detection",
                "Adaptive Risk Scoring"
        ));
        aiInfo.put("channels", Arrays.asList("WEB", "MOBILE", "API", "ATM", "NET_BANKING", "UPI", "ADMIN_PORTAL"));
        dashboard.put("aiInfo", aiInfo);

        return dashboard;
    }

    /**
     * Get paginated security events with optional filters.
     */
    public Page<AiSecurityEvent> getSecurityEvents(String eventType, String channel,
                                                     String severity, String status,
                                                     int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        AiSecurityEvent.EventType et = eventType != null && !eventType.isEmpty() ?
                AiSecurityEvent.EventType.valueOf(eventType) : null;
        AiSecurityEvent.Channel ch = channel != null && !channel.isEmpty() ?
                AiSecurityEvent.Channel.valueOf(channel) : null;
        AiSecurityEvent.Severity sv = severity != null && !severity.isEmpty() ?
                AiSecurityEvent.Severity.valueOf(severity) : null;
        AiSecurityEvent.Status st = status != null && !status.isEmpty() ?
                AiSecurityEvent.Status.valueOf(status) : null;

        return eventRepository.findWithFilters(et, ch, sv, st, pageable);
    }

    /**
     * Get threat score for a specific entity.
     */
    public AiThreatScore getThreatScore(String entityId, String entityType) {
        return threatScoreRepository.findByEntityIdAndEntityType(entityId, entityType).orElse(null);
    }

    /**
     * Get device fingerprints for an entity.
     */
    public List<AiDeviceFingerprint> getDeviceFingerprints(String entityId) {
        return deviceFingerprintRepository.findByEntityIdAndEntityTypeOrderByLastSeenAtDesc(entityId, "USER");
    }

    /**
     * Update security event status (admin action).
     */
    @Transactional
    public AiSecurityEvent updateEventStatus(Long eventId, String newStatus, String resolvedBy, String notes) {
        AiSecurityEvent event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return null;

        event.setStatus(AiSecurityEvent.Status.valueOf(newStatus));
        if ("RESOLVED".equals(newStatus) || "FALSE_POSITIVE".equals(newStatus)) {
            event.setResolvedBy(resolvedBy);
            event.setResolvedAt(LocalDateTime.now());
            event.setResolutionNotes(notes);

            // Update threat score false positive count
            if ("FALSE_POSITIVE".equals(newStatus) && event.getSourceEntityId() != null) {
                AiThreatScore score = threatScoreRepository
                        .findByEntityIdAndEntityType(event.getSourceEntityId(), event.getSourceEntityType())
                        .orElse(null);
                if (score != null) {
                    score.setFalsePositives(score.getFalsePositives() + 1);
                    // Reduce risk score for false positives to self-calibrate
                    score.setOverallRiskScore(Math.max(0, score.getOverallRiskScore() - 5));
                    threatScoreRepository.save(score);
                }
            }
        }
        return eventRepository.save(event);
    }

    /**
     * Toggle watchlist status for an entity.
     */
    @Transactional
    public AiThreatScore toggleWatchlist(String entityId, String entityType, boolean watchlist, String reason) {
        AiThreatScore score = threatScoreRepository.findByEntityIdAndEntityType(entityId, entityType)
                .orElse(new AiThreatScore());
        score.setEntityId(entityId);
        score.setEntityType(entityType);
        score.setIsWatchlisted(watchlist);
        score.setWatchlistReason(watchlist ? reason : null);
        score.setUpdatedAt(LocalDateTime.now());
        return threatScoreRepository.save(score);
    }

    /**
     * Trust or untrust a device.
     */
    @Transactional
    public AiDeviceFingerprint setDeviceTrust(Long deviceId, boolean trusted) {
        AiDeviceFingerprint device = deviceFingerprintRepository.findById(deviceId).orElse(null);
        if (device == null) return null;
        device.setIsTrusted(trusted);
        device.setTrustScore(trusted ? 95.0 : 10.0);
        return deviceFingerprintRepository.save(device);
    }

    // ========================= SECURITY RULES MANAGEMENT =========================

    public List<AiSecurityRule> getAllRules() {
        return ruleRepository.findAll();
    }

    @Transactional
    public AiSecurityRule toggleRule(Long ruleId, boolean active) {
        AiSecurityRule rule = ruleRepository.findById(ruleId).orElse(null);
        if (rule == null) return null;
        rule.setIsActive(active);
        rule.setUpdatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    @Transactional
    public AiSecurityRule createRule(AiSecurityRule rule) {
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    // ========================= INTERNAL HELPERS =========================

    private void createSecurityEvent(AiSecurityEvent.EventType type, AiSecurityEvent.Channel channel,
                                     AiSecurityEvent.Severity severity, double riskScore,
                                     String sourceId, String sourceType, String title, String description,
                                     String clientIp, String location, String deviceHash, String userAgent) {
        AiSecurityEvent event = new AiSecurityEvent();
        event.setEventType(type);
        event.setChannel(channel);
        event.setSeverity(severity);
        event.setRiskScore(riskScore);
        event.setSourceEntityId(sourceId);
        event.setSourceEntityType(sourceType);
        event.setTitle(title);
        event.setDescription(description);
        event.setClientIp(clientIp);
        event.setLocation(location);
        event.setDeviceFingerprint(deviceHash);
        event.setUserAgent(userAgent);
        event.setAiConfidence(calculateConfidence(riskScore));
        event.setCreatedAt(LocalDateTime.now());

        // Determine action
        if (riskScore >= CRITICAL_RISK_THRESHOLD) {
            event.setActionTaken(AiSecurityEvent.ActionTaken.ACCOUNT_LOCKED);
            event.setStatus(AiSecurityEvent.Status.BLOCKED);
        } else if (riskScore >= HIGH_RISK_THRESHOLD) {
            event.setActionTaken(AiSecurityEvent.ActionTaken.MFA_TRIGGERED);
            event.setStatus(AiSecurityEvent.Status.ESCALATED);
        } else {
            event.setActionTaken(AiSecurityEvent.ActionTaken.ALERT_SENT);
        }

        eventRepository.save(event);
    }

    @Transactional
    private void updateThreatScore(String entityId, String entityType, double newScore,
                                    String dimension, List<String> factors) {
        AiThreatScore score = threatScoreRepository.findByEntityIdAndEntityType(entityId, entityType)
                .orElse(new AiThreatScore());

        score.setEntityId(entityId);
        score.setEntityType(entityType);
        score.setTotalEvents(score.getTotalEvents() + 1);

        // Update dimension-specific scores with exponential moving average
        double alpha = 0.3; // EMA smoothing factor
        switch (dimension) {
            case "login":
                score.setLoginRiskScore(ema(score.getLoginRiskScore(), newScore, alpha));
                break;
            case "transaction":
                score.setTransactionRiskScore(ema(score.getTransactionRiskScore(), newScore, alpha));
                break;
            case "behavioral":
                score.setBehavioralRiskScore(ema(score.getBehavioralRiskScore(), newScore, alpha));
                break;
            default:
                break;
        }

        // Weighted composite risk score
        double composite = score.getLoginRiskScore() * 0.3 +
                score.getTransactionRiskScore() * 0.35 +
                score.getBehavioralRiskScore() * 0.2 +
                score.getDeviceRiskScore() * 0.1 +
                score.getNetworkRiskScore() * 0.05;

        score.setOverallRiskScore(Math.max(0, Math.min(100, composite)));
        score.setRiskLevel(AiThreatScore.RiskLevel.valueOf(getRiskLevel(composite)));
        score.setRiskFactors(String.join("; ", factors));
        score.setLastActivity(dimension + " analysis");
        score.setLastEvaluatedAt(LocalDateTime.now());
        score.setUpdatedAt(LocalDateTime.now());

        threatScoreRepository.save(score);
    }

    private void registerDevice(String entityId, String entityType, String deviceHash,
                                 String deviceInfo, String userAgent, String ipAddress, String location) {
        AiDeviceFingerprint device = new AiDeviceFingerprint();
        device.setEntityId(entityId);
        device.setEntityType(entityType);
        device.setDeviceHash(deviceHash);
        device.setDeviceType(extractDeviceType(userAgent));
        device.setBrowser(extractBrowser(userAgent));
        device.setOs(extractOS(userAgent));
        device.setIpAddress(ipAddress);
        device.setGeoLocation(location);
        device.setFirstSeenAt(LocalDateTime.now());
        device.setLastSeenAt(LocalDateTime.now());
        device.setLoginCount(1);
        deviceFingerprintRepository.save(device);
    }

    private String generateDeviceHash(String userAgent, String deviceInfo) {
        try {
            String raw = (userAgent != null ? userAgent : "") + "|" + (deviceInfo != null ? deviceInfo : "");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().substring(0, 32);
        } catch (Exception e) {
            return "unknown-" + System.currentTimeMillis();
        }
    }

    private String getRiskLevel(double score) {
        if (score >= CRITICAL_RISK_THRESHOLD) return "CRITICAL";
        if (score >= HIGH_RISK_THRESHOLD) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private String getRecommendedAction(double score) {
        if (score >= CRITICAL_RISK_THRESHOLD) return "BLOCK_AND_LOCK";
        if (score >= HIGH_RISK_THRESHOLD) return "REQUIRE_MFA";
        if (score >= 40) return "MONITOR";
        return "ALLOW";
    }

    private double calculateConfidence(double riskScore) {
        // Higher risk scores have higher confidence (more factors contribute)
        return Math.min(0.95, 0.5 + (riskScore / 200.0));
    }

    private double ema(Double oldValue, double newValue, double alpha) {
        if (oldValue == null || oldValue == 0) return newValue;
        return alpha * newValue + (1 - alpha) * oldValue;
    }

    private double calculateStdDev(double[] values) {
        if (values.length == 0) return 0;
        double mean = Arrays.stream(values).average().orElse(0);
        double variance = Arrays.stream(values).map(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    private String extractDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "Mobile";
        if (ua.contains("tablet") || ua.contains("ipad")) return "Tablet";
        return "Desktop";
    }

    private String extractBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Safari";
        if (userAgent.contains("Edg")) return "Edge";
        return "Other";
    }

    private String extractOS(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac OS")) return "macOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";
        return "Other";
    }
}
