package com.apps.bubbletilt;

import java.util.List;

public class ScatterBt2Response extends BaseResponse {
    private List<ScatterBt2Model> data;

    public List<ScatterBt2Model> getData() { return data; }
    public void setData(List<ScatterBt2Model> data) { this.data = data; }
}