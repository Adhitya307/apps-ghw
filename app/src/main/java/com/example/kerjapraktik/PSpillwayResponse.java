package com.example.kerjapraktik;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PSpillwayResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<PSpillwayModel> data;

    // Getter and Setter methods
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<PSpillwayModel> getData() {
        return data;
    }

    public void setData(List<PSpillwayModel> data) {
        this.data = data;
    }
}