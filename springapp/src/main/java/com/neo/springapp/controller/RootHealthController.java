package com.neo.springapp.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Handles root and probe paths for cloud health checks (e.g. Render).
 * The production profile disables static resource serving, so /index.html
 * must be mapped explicitly instead of served from classpath:/static/.
 */
@RestController
@Profile("production")
public class RootHealthController {

    @RequestMapping(value = {"/", "/index.html"}, method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "NeoBank API",
                "status", "UP",
                "health", "/actuator/health"
        ));
    }
}
