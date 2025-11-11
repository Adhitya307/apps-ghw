package com.apps.bubbletilt;

import java.util.List;

public class ScatterBt1Response extends BaseResponse {
    private List<ScatterBt1Model> data;

    public List<ScatterBt1Model> getData() { return data; }
    public void setData(List<ScatterBt1Model> data) { this.data = data; }
}