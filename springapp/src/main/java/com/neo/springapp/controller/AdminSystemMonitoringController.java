package com.neo.springapp.controller;

import com.neo.springapp.service.SystemMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/system-monitoring")
public class AdminSystemMonitoringController {

    private final SystemMonitoringService monitoringService;

    public AdminSystemMonitoringController(SystemMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSnapshot(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(monitoringService.getSnapshot(limit));
    }
}
