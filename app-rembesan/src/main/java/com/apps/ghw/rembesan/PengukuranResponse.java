package com.apps.ghw.rembesan;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PengukuranResponse {
    @SerializedName("status")
    private boolean status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<PengukuranModel> data;

    public boolean isStatus() { return status; }
    public String getMessage() { return message; }
    public List<PengukuranModel> getData() { return data; }
}
