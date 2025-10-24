package com.example.app_dambody;

import java.util.List;

public class PengukuranResponse {
    private boolean status;
    private String message;
    private List<PengukuranModel> data;

    public PengukuranResponse() {}

    // Getters and Setters
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<PengukuranModel> getData() { return data; }
    public void setData(List<PengukuranModel> data) { this.data = data; }
}