package com.example.app_dambody;

import java.util.List;

public class AmbangBatas600H5Response {
    private boolean success;
    private String message;
    private List<AmbangBatas600H5Model> data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<AmbangBatas600H5Model> getData() { return data; }
    public void setData(List<AmbangBatas600H5Model> data) { this.data = data; }
}