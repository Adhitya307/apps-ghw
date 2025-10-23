package com.apps.ghw.rembesan;

import java.util.List;

public class SRResponse {
    private String status;
    private List<SRModel> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<SRModel> getData() {
        return data;
    }

    public void setData(List<SRModel> data) {
        this.data = data;
    }
}
