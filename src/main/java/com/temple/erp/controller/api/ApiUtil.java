package com.temple.erp.controller.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

public final class ApiUtil {
    private ApiUtil() {}

    public static ResponseEntity<Map<String, Object>> ok(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", message);
        return ResponseEntity.ok(m);
    }

    public static ResponseEntity<Map<String, Object>> fail(int status, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", message);
        return ResponseEntity.status(status).body(m);
    }

    public static Map<String, Object> body(boolean success) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", success);
        return m;
    }
}
