package com.apps.bubbletilt;

import java.util.List;

public class BacaanBt1Response extends BaseResponse {
    private List<BacaanBt1Model> data;

    public List<BacaanBt1Model> getData() { return data; }
    public void setData(List<BacaanBt1Model> data) { this.data = data; }
}