package com.apps.bubbletilt;

import java.util.List;

public class BacaanBt8Response extends BaseResponse {
    private List<BacaanBt8Model> data;

    public List<BacaanBt8Model> getData() { return data; }
    public void setData(List<BacaanBt8Model> data) { this.data = data; }
}