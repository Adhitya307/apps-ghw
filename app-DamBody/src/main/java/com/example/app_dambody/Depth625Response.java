package com.example.app_dambody;

import java.util.List;

public class Depth625Response {
    private boolean status;
    private String message;
    private List<Depth625Model> data;

    // Getters and Setters
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Depth625Model> getData() { return data; }
    public void setData(List<Depth625Model> data) { this.data = data; }
}