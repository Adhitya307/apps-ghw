package com.apps.bubbletilt;

import java.util.List;

public class ScatterBt4Response extends BaseResponse {
    private List<ScatterBt4Model> data;

    public List<ScatterBt4Model> getData() { return data; }
    public void setData(List<ScatterBt4Model> data) { this.data = data; }
}