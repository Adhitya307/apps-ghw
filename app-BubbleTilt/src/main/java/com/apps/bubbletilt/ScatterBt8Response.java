package com.apps.bubbletilt;

import java.util.List;

public class ScatterBt8Response extends BaseResponse {
    private List<ScatterBt8Model> data;

    public List<ScatterBt8Model> getData() { return data; }
    public void setData(List<ScatterBt8Model> data) { this.data = data; }
}