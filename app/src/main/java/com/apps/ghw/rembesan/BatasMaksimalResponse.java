package com.apps.ghw.rembesan;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BatasMaksimalResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<BatasMaksimalModel> data;

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

    public List<BatasMaksimalModel> getData() {
        return data;
    }

    public void setData(List<BatasMaksimalModel> data) {
        this.data = data;
    }
}