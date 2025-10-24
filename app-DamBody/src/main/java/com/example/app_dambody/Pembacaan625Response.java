package com.example.app_dambody;

import java.util.List;

public class Pembacaan625Response {
    private boolean status;
    private String message;
    private List<Pembacaan625Model> data;

    public Pembacaan625Response() {}

    // Getters and Setters
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Pembacaan625Model> getData() { return data; }
    public void setData(List<Pembacaan625Model> data) { this.data = data; }
}