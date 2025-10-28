package com.example.app_dambody;

import java.util.List;

public class AmbangBatas600H3Response {
    private boolean success;
    private String message;
    private List<AmbangBatas600H3Model> data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<AmbangBatas600H3Model> getData() { return data; }
    public void setData(List<AmbangBatas600H3Model> data) { this.data = data; }
}