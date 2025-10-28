package com.example.app_dambody;

import java.util.List;

public class AmbangBatas625H1Response {
    private boolean success;
    private String message;
    private List<AmbangBatas625H1Model> data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<AmbangBatas625H1Model> getData() { return data; }
    public void setData(List<AmbangBatas625H1Model> data) { this.data = data; }
}