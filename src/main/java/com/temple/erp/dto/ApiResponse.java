package com.temple.erp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    private boolean success;
    private String message;
    private Map<String, Object> data = new HashMap<>();

    public ApiResponse() {}
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ApiResponse ok(String message) { return new ApiResponse(true, message); }
    public static ApiResponse fail(String message) { return new ApiResponse(false, message); }

    public ApiResponse put(String key, Object value) { this.data.put(key, value); return this; }

    // Jackson will serialize getters; expose dynamic fields by flattening in controllers when needed.
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
