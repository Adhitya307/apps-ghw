package com.apps.bubbletilt;

import java.util.List;

public class PerhitunganBt1Response extends BaseResponse {
    private List<PerhitunganBt1Model> data;

    public List<PerhitunganBt1Model> getData() { return data; }
    public void setData(List<PerhitunganBt1Model> data) { this.data = data; }
}