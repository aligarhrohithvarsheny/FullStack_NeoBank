package com.neo.springapp.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemMonitoringServiceTest {

    @Test
    void snapshotCountsRecentIncidentsAndRaisesConfiguredSpikeAlerts() {
        SystemMonitoringService service = new SystemMonitoringService(null, 500, 2, 3, 1, 3, 0.85);

        service.recordApiError("GET", "/api/accounts/123456789", 500);
        service.recordApiError("POST", "/api/transfers", 503);
        service.recordSlowDatabaseOperation("AccountRepository.findById(..)", 700);
        service.recordScheduledJobFailure("FDInterestSchedulerService.calculate(..)");
        service.recordNotificationFailure("OTP email");

        Map<String, Object> snapshot = service.getSnapshot(100);
        Map<?, ?> metrics = (Map<?, ?>) snapshot.get("metrics");
        List<?> alerts = (List<?>) snapshot.get("alerts");
        List<?> events = (List<?>) snapshot.get("events");

        assertEquals("DEGRADED", snapshot.get("status"));
        assertEquals(2L, metrics.get("apiErrors5m"));
        assertEquals(1L, metrics.get("slowDatabaseOperations5m"));
        assertEquals(1L, metrics.get("scheduledJobFailures5m"));
        assertEquals(1L, metrics.get("notificationFailures5m"));
        assertEquals(3, alerts.size());
        assertTrue(events.stream().anyMatch(event ->
                ((SystemMonitoringService.MonitoringEvent) event).source().contains("/{id}")));
    }

    @Test
    void eventListLimitIsClampedAndDoesNotExposePoolWhenUnavailable() {
        SystemMonitoringService service = new SystemMonitoringService(null, 1000, 5, 3, 1, 3, 0.85);
        service.recordNotificationFailure("Email");

        Map<String, Object> snapshot = service.getSnapshot(0);
        Map<?, ?> metrics = (Map<?, ?>) snapshot.get("metrics");
        Map<?, ?> pool = (Map<?, ?>) metrics.get("databasePool");

        assertEquals(1, ((List<?>) snapshot.get("events")).size());
        assertEquals("UNAVAILABLE", pool.get("status"));
        assertEquals("DEGRADED", snapshot.get("status"));
    }
}
