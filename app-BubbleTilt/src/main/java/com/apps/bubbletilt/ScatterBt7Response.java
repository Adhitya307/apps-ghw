package com.apps.bubbletilt;

import java.util.List;

public class ScatterBt7Response extends BaseResponse {
    private List<ScatterBt7Model> data;

    public List<ScatterBt7Model> getData() { return data; }
    public void setData(List<ScatterBt7Model> data) { this.data = data; }
}