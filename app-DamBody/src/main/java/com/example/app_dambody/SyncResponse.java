package com.example.app_dambody;

import java.util.Map;

public class SyncResponse {
    private String status;
    private String message;
    private Map<String, Object> data;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
