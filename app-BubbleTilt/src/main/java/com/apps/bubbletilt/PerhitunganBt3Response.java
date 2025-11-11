package com.apps.bubbletilt;

import java.util.List;

public class PerhitunganBt3Response extends BaseResponse {
    private List<PerhitunganBt3Model> data;

    public List<PerhitunganBt3Model> getData() { return data; }
    public void setData(List<PerhitunganBt3Model> data) { this.data = data; }
}