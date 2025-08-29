package com.example.kerjapraktik;

import java.util.List;

public class ThomsonResponse {
    private String status;
    private List<ThomsonWeirModel> data;

    public String getStatus() { return status; }
    public List<ThomsonWeirModel> getData() { return data; }
}

