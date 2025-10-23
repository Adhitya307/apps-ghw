package com.apps.ghw.rembesan;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PBocoranBaruResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<PBocoranBaruModel> data;

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

    public List<PBocoranBaruModel> getData() {
        return data;
    }

    public void setData(List<PBocoranBaruModel> data) {
        this.data = data;
    }
}