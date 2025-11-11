package com.apps.bubbletilt;

import java.util.List;

public class PerhitunganBt2Response extends BaseResponse {
    private List<PerhitunganBt2Model> data;

    public List<PerhitunganBt2Model> getData() { return data; }
    public void setData(List<PerhitunganBt2Model> data) { this.data = data; }
}