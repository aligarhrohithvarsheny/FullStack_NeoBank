package com.neo.springapp.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SystemMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(SystemMonitoringService.class);
    private static final int MAX_EVENTS = 500;
    private static final Duration ALERT_WINDOW = Duration.ofMinutes(5);

    private final DataSource dataSource;
    private final long slowQueryThresholdMs;
    private final int apiErrorSpikeThreshold;
    private final int slowQuerySpikeThreshold;
    private final int scheduledFailureAlertThreshold;
    private final int notificationFailureSpikeThreshold;
    private final double poolUtilizationAlertThreshold;
    private final long startedAt = System.currentTimeMillis();
    private final AtomicLong eventIds = new AtomicLong();
    private final ConcurrentLinkedDeque<MonitoringEvent> events = new ConcurrentLinkedDeque<>();

    public SystemMonitoringService(
            DataSource dataSource,
            @Value("${app.monitoring.slow-query-threshold-ms:1000}") long slowQueryThresholdMs,
            @Value("${app.monitoring.api-error-spike-threshold:5}") int apiErrorSpikeThreshold,
            @Value("${app.monitoring.slow-query-spike-threshold:3}") int slowQuerySpikeThreshold,
            @Value("${app.monitoring.scheduled-failure-alert-threshold:1}") int scheduledFailureAlertThreshold,
            @Value("${app.monitoring.notification-failure-spike-threshold:3}") int notificationFailureSpikeThreshold,
            @Value("${app.monitoring.pool-utilization-alert-threshold:0.85}") double poolUtilizationAlertThreshold) {
        this.dataSource = dataSource;
        this.slowQueryThresholdMs = slowQueryThresholdMs;
        this.apiErrorSpikeThreshold = apiErrorSpikeThreshold;
        this.slowQuerySpikeThreshold = slowQuerySpikeThreshold;
        this.scheduledFailureAlertThreshold = scheduledFailureAlertThreshold;
        this.notificationFailureSpikeThreshold = notificationFailureSpikeThreshold;
        this.poolUtilizationAlertThreshold = poolUtilizationAlertThreshold;
    }

    public void recordApiError(String method, String path, int status) {
        record("API_ERROR", method + " " + normalizePath(path), "HTTP " + status, null);
    }

    public void recordSlowDatabaseOperation(String operation, long durationMs) {
        record("SLOW_DATABASE_QUERY", operation, "Repository operation exceeded threshold", durationMs);
    }

    public void recordScheduledJobFailure(String jobName) {
        record("SCHEDULED_JOB_FAILURE", jobName, "Scheduled job execution failed", null);
    }

    public void recordNotificationFailure(String channel) {
        record("NOTIFICATION_FAILURE", channel, "Notification delivery failed", null);
    }

    public Map<String, Object> getSnapshot(int requestedEventLimit) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(ALERT_WINDOW);
        List<MonitoringEvent> allEvents = new ArrayList<>(events);
        allEvents.sort(Comparator.comparing(MonitoringEvent::timestamp).reversed());
        List<MonitoringEvent> recentEvents = allEvents.stream()
                .filter(event -> !event.timestamp().isBefore(windowStart))
                .toList();

        long apiErrors = count(recentEvents, "API_ERROR");
        long slowQueries = count(recentEvents, "SLOW_DATABASE_QUERY");
        long scheduledFailures = count(recentEvents, "SCHEDULED_JOB_FAILURE");
        long notificationFailures = count(recentEvents, "NOTIFICATION_FAILURE");

        PoolSnapshot pool = getPoolSnapshot();
        List<Map<String, Object>> alerts = new ArrayList<>();
        addThresholdAlert(alerts, "API_ERROR_SPIKE", "HIGH", "API error spike",
                apiErrors, apiErrorSpikeThreshold, "5-minute API error response count");
        addThresholdAlert(alerts, "SLOW_QUERY_SPIKE", "MEDIUM", "Slow database operation spike",
                slowQueries, slowQuerySpikeThreshold, "5-minute slow repository operation count");
        addThresholdAlert(alerts, "SCHEDULED_JOB_FAILURE", "HIGH", "Scheduled job failures",
                scheduledFailures, scheduledFailureAlertThreshold, "5-minute failed scheduled job count");
        addThresholdAlert(alerts, "NOTIFICATION_FAILURE_SPIKE", "MEDIUM", "Notification delivery failures",
                notificationFailures, notificationFailureSpikeThreshold, "5-minute failed notification count");
        if (pool.utilization() != null && pool.utilization() >= poolUtilizationAlertThreshold) {
            alerts.add(alert("DATABASE_POOL_UTILIZATION", "HIGH", "Database connection pool near capacity",
                    pool.activeConnections() + " of " + pool.maximumPoolSize() + " connections are active"));
        } else if (!"UP".equals(pool.status())) {
            alerts.add(alert("DATABASE_POOL_STATUS", "HIGH", "Database connection pool unavailable",
                    "Connection pool status is " + pool.status()));
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("uptimeSeconds", Math.max(0, (System.currentTimeMillis() - startedAt) / 1000));
        metrics.put("apiErrors5m", apiErrors);
        metrics.put("slowDatabaseOperations5m", slowQueries);
        metrics.put("scheduledJobFailures5m", scheduledFailures);
        metrics.put("notificationFailures5m", notificationFailures);
        metrics.put("slowQueryThresholdMs", slowQueryThresholdMs);
        metrics.put("databasePool", pool.asMap());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", alerts.isEmpty() ? "UP" : "DEGRADED");
        snapshot.put("generatedAt", now);
        snapshot.put("windowMinutes", ALERT_WINDOW.toMinutes());
        snapshot.put("retention", "In-memory only; up to " + MAX_EVENTS + " events, reset on restart");
        snapshot.put("metrics", metrics);
        snapshot.put("alerts", alerts);
        snapshot.put("events", allEvents.stream().limit(Math.max(1, Math.min(requestedEventLimit, 100))).toList());
        return snapshot;
    }

    private void record(String category, String source, String message, Long durationMs) {
        events.addFirst(new MonitoringEvent(
                eventIds.incrementAndGet(), Instant.now(), category, source, message, durationMs));
        while (events.size() > MAX_EVENTS) {
            events.pollLast();
        }
    }

    private PoolSnapshot getPoolSnapshot() {
        if (dataSource == null) {
            return PoolSnapshot.unavailable("UNAVAILABLE");
        }
        try {
            HikariDataSource hikari = dataSource instanceof HikariDataSource direct
                    ? direct
                    : dataSource.unwrap(HikariDataSource.class);
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool == null || hikari.isClosed()) {
                return PoolSnapshot.unavailable("INITIALIZING");
            }
            int maximum = hikari.getMaximumPoolSize();
            int active = pool.getActiveConnections();
            Double utilization = maximum > 0 ? (double) active / maximum : null;
            String status = pool.getThreadsAwaitingConnection() > 0 ? "DEGRADED" : "UP";
            return new PoolSnapshot(status, active, pool.getIdleConnections(), pool.getTotalConnections(),
                    maximum, pool.getThreadsAwaitingConnection(), utilization);
        } catch (SQLException | RuntimeException exception) {
            log.warn("Unable to read database connection pool metrics: {}", exception.getClass().getSimpleName());
            return PoolSnapshot.unavailable("UNKNOWN");
        }
    }

    private static long count(List<MonitoringEvent> recentEvents, String category) {
        return recentEvents.stream().filter(event -> category.equals(event.category())).count();
    }

    private static void addThresholdAlert(List<Map<String, Object>> alerts, String code, String severity,
            String title, long count, int threshold, String description) {
        if (count >= threshold) {
            alerts.add(alert(code, severity, title, count + " " + description + " (threshold: " + threshold + ")"));
        }
    }

    private static Map<String, Object> alert(String code, String severity, String title, String message) {
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("code", code);
        alert.put("severity", severity);
        alert.put("title", title);
        alert.put("message", message);
        return alert;
    }

    private static String normalizePath(String path) {
        return path.replaceAll("/\\d+(?=/|$)", "/{id}")
                .replaceAll("/[0-9a-fA-F-]{16,}(?=/|$)", "/{id}");
    }

    public record MonitoringEvent(
            long id,
            Instant timestamp,
            String category,
            String source,
            String message,
            Long durationMs) {
    }

    private record PoolSnapshot(
            String status,
            Integer activeConnections,
            Integer idleConnections,
            Integer totalConnections,
            Integer maximumPoolSize,
            Integer threadsAwaitingConnection,
            Double utilization) {

        static PoolSnapshot unavailable(String status) {
            return new PoolSnapshot(status, null, null, null, null, null, null);
        }

        Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", status);
            result.put("activeConnections", activeConnections);
            result.put("idleConnections", idleConnections);
            result.put("totalConnections", totalConnections);
            result.put("maximumPoolSize", maximumPoolSize);
            result.put("threadsAwaitingConnection", threadsAwaitingConnection);
            result.put("utilization", utilization);
            return result;
        }
    }
}
