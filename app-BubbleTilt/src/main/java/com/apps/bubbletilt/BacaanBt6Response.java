package com.apps.bubbletilt;

import java.util.List;

public class BacaanBt6Response extends BaseResponse {
    private List<BacaanBt6Model> data;

    public List<BacaanBt6Model> getData() { return data; }
    public void setData(List<BacaanBt6Model> data) { this.data = data; }
}