package com.apps.bubbletilt;

import java.util.List;

public class BacaanBt7Response extends BaseResponse {
    private List<BacaanBt7Model> data;

    public List<BacaanBt7Model> getData() { return data; }
    public void setData(List<BacaanBt7Model> data) { this.data = data; }
}