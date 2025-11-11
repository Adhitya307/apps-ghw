package com.apps.bubbletilt;

import java.util.List;

public class AllDataResponse extends BaseResponse {
    private AllDataBtm data;
    private String last_sync;

    public AllDataBtm getData() { return data; }
    public void setData(AllDataBtm data) { this.data = data; }

    public String getLast_sync() { return last_sync; }
    public void setLast_sync(String last_sync) { this.last_sync = last_sync; }

    public static class AllDataBtm {
        private List<BacaanBt1Model> bacaan_bt1;
        private List<BacaanBt2Model> bacaan_bt2;
        private List<BacaanBt3Model> bacaan_bt3;
        private List<BacaanBt4Model> bacaan_bt4;
        private List<BacaanBt6Model> bacaan_bt6;
        private List<BacaanBt7Model> bacaan_bt7;
        private List<BacaanBt8Model> bacaan_bt8;

        public List<BacaanBt1Model> getBacaan_bt1() { return bacaan_bt1; }
        public void setBacaan_bt1(List<BacaanBt1Model> bacaan_bt1) { this.bacaan_bt1 = bacaan_bt1; }

        public List<BacaanBt2Model> getBacaan_bt2() { return bacaan_bt2; }
        public void setBacaan_bt2(List<BacaanBt2Model> bacaan_bt2) { this.bacaan_bt2 = bacaan_bt2; }

        public List<BacaanBt3Model> getBacaan_bt3() { return bacaan_bt3; }
        public void setBacaan_bt3(List<BacaanBt3Model> bacaan_bt3) { this.bacaan_bt3 = bacaan_bt3; }

        public List<BacaanBt4Model> getBacaan_bt4() { return bacaan_bt4; }
        public void setBacaan_bt4(List<BacaanBt4Model> bacaan_bt4) { this.bacaan_bt4 = bacaan_bt4; }

        public List<BacaanBt6Model> getBacaan_bt6() { return bacaan_bt6; }
        public void setBacaan_bt6(List<BacaanBt6Model> bacaan_bt6) { this.bacaan_bt6 = bacaan_bt6; }

        public List<BacaanBt7Model> getBacaan_bt7() { return bacaan_bt7; }
        public void setBacaan_bt7(List<BacaanBt7Model> bacaan_bt7) { this.bacaan_bt7 = bacaan_bt7; }

        public List<BacaanBt8Model> getBacaan_bt8() { return bacaan_bt8; }
        public void setBacaan_bt8(List<BacaanBt8Model> bacaan_bt8) { this.bacaan_bt8 = bacaan_bt8; }
    }
}