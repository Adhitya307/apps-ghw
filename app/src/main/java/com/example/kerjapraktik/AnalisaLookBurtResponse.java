package com.example.kerjapraktik;

import java.util.List;

public class AnalisaLookBurtResponse {

    private String status; // "success" atau "error"
    private String message;
    private List<AnalisaLookBurtModel> data;

    public AnalisaLookBurtResponse() {}

    public AnalisaLookBurtResponse(String status, String message, List<AnalisaLookBurtModel> data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<AnalisaLookBurtModel> getData() { return data; }
    public void setData(List<AnalisaLookBurtModel> data) { this.data = data; }

    public boolean isSuccess() { return "success".equalsIgnoreCase(status); }

    // Tandai semua data sebagai sudah sinkron
    public void markAllAsSynced() {
        if (data != null) {
            for (AnalisaLookBurtModel model : data) {
                model.setIsSynced(1);
            }
        }
    }

    @Override
    public String toString() {
        return "AnalisaLookBurtResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", dataSize=" + (data != null ? data.size() : 0) +
                '}';
    }
}
