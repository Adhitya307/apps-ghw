package com.apps.bubbletilt;

import java.util.List;

public class PerhitunganBt8Response extends BaseResponse {
    private List<PerhitunganBt8Model> data;

    public List<PerhitunganBt8Model> getData() { return data; }
    public void setData(List<PerhitunganBt8Model> data) { this.data = data; }
}