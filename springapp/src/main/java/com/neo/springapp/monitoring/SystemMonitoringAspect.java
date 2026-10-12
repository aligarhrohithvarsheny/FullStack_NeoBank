package com.neo.springapp.monitoring;

import com.neo.springapp.service.SystemMonitoringService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SystemMonitoringAspect {

    private final SystemMonitoringService monitoringService;
    private final long slowQueryThresholdMs;

    public SystemMonitoringAspect(
            SystemMonitoringService monitoringService,
            @Value("${app.monitoring.slow-query-threshold-ms:1000}") long slowQueryThresholdMs) {
        this.monitoringService = monitoringService;
        this.slowQueryThresholdMs = slowQueryThresholdMs;
    }

    @Around("execution(* com.neo.springapp.repository..*(..))")
    public Object monitorRepositoryOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            if (durationMs >= slowQueryThresholdMs) {
                monitoringService.recordSlowDatabaseOperation(joinPoint.getSignature().toShortString(), durationMs);
            }
        }
    }

    @Around("@annotation(scheduled)")
    public Object monitorScheduledJob(ProceedingJoinPoint joinPoint, Scheduled scheduled) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable failure) {
            monitoringService.recordScheduledJobFailure(joinPoint.getSignature().toShortString());
            throw failure;
        }
    }
}
