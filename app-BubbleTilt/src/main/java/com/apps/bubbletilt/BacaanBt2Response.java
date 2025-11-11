package com.apps.bubbletilt;

import java.util.List;

public class BacaanBt2Response extends BaseResponse {
    private List<BacaanBt2Model> data;

    public List<BacaanBt2Model> getData() { return data; }
    public void setData(List<BacaanBt2Model> data) { this.data = data; }
}