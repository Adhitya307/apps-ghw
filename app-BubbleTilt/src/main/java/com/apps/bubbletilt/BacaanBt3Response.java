package com.apps.bubbletilt;

import java.util.List;

public class BacaanBt3Response extends BaseResponse {
    private List<BacaanBt3Model> data;

    public List<BacaanBt3Model> getData() { return data; }
    public void setData(List<BacaanBt3Model> data) { this.data = data; }
}