package com.apps.bubbletilt;

import java.util.List;

public class ScatterBt3Response extends BaseResponse {
    private List<ScatterBt3Model> data;

    public List<ScatterBt3Model> getData() { return data; }
    public void setData(List<ScatterBt3Model> data) { this.data = data; }
}