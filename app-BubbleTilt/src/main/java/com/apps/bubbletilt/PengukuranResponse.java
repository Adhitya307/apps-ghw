package com.apps.bubbletilt;

import java.util.List;

public class PengukuranResponse extends BaseResponse {
    private List<PengukuranBtmModel> data;

    public List<PengukuranBtmModel> getData() { return data; }
    public void setData(List<PengukuranBtmModel> data) { this.data = data; }
}