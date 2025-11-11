package com.apps.bubbletilt;

import java.util.List;

public class BacaanBt4Response extends BaseResponse {
    private List<BacaanBt4Model> data;

    public List<BacaanBt4Model> getData() { return data; }
    public void setData(List<BacaanBt4Model> data) { this.data = data; }
}