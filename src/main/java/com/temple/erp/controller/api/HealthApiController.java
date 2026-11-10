package com.temple.erp.controller.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthApiController {
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "OK", "message", "Temple ERP Java API is running");
    }
}
