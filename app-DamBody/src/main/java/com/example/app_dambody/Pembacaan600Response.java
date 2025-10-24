package com.example.app_dambody;

import java.util.List;

public class Pembacaan600Response {
    private boolean status;
    private String message;
    private List<Pembacaan600Model> data;

    public Pembacaan600Response() {}

    // Getters and Setters
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Pembacaan600Model> getData() { return data; }
    public void setData(List<Pembacaan600Model> data) { this.data = data; }
}