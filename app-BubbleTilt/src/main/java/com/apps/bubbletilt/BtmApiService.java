package com.apps.bubbletilt;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BtmApiService {

    // ==================== BACAAAN ENDPOINTS ====================
    @GET("bacaan_bt1")
    Call<BacaanBt1Response> getBacaanBt1();

    @GET("bacaan_bt2")
    Call<BacaanBt2Response> getBacaanBt2();

    @GET("bacaan_bt3")
    Call<BacaanBt3Response> getBacaanBt3();

    @GET("bacaan_bt4")
    Call<BacaanBt4Response> getBacaanBt4();

    @GET("bacaan_bt6")
    Call<BacaanBt6Response> getBacaanBt6();

    @GET("bacaan_bt7")
    Call<BacaanBt7Response> getBacaanBt7();

    @GET("bacaan_bt8")
    Call<BacaanBt8Response> getBacaanBt8();

    // ==================== PERHITUNGAN ENDPOINTS ====================
    @GET("perhitungan_bt1")
    Call<PerhitunganBt1Response> getPerhitunganBt1();

    @GET("perhitungan_bt2")
    Call<PerhitunganBt2Response> getPerhitunganBt2();

    @GET("perhitungan_bt3")
    Call<PerhitunganBt3Response> getPerhitunganBt3();

    @GET("perhitungan_bt4")
    Call<PerhitunganBt4Response> getPerhitunganBt4();

    @GET("perhitungan_bt6")
    Call<PerhitunganBt6Response> getPerhitunganBt6();

    @GET("perhitungan_bt7")
    Call<PerhitunganBt7Response> getPerhitunganBt7();

    @GET("perhitungan_bt8")
    Call<PerhitunganBt8Response> getPerhitunganBt8();

    // ==================== SCATTER ENDPOINTS ====================
    @GET("scatter_bt1")
    Call<ScatterBt1Response> getScatterBt1();

    @GET("scatter_bt2")
    Call<ScatterBt2Response> getScatterBt2();

    @GET("scatter_bt3")
    Call<ScatterBt3Response> getScatterBt3();

    @GET("scatter_bt4")
    Call<ScatterBt4Response> getScatterBt4();

    @GET("scatter_bt6")
    Call<ScatterBt6Response> getScatterBt6();

    @GET("scatter_bt7")
    Call<ScatterBt7Response> getScatterBt7();

    @GET("scatter_bt8")
    Call<ScatterBt8Response> getScatterBt8();

    // ==================== PENGUKURAN ENDPOINTS ====================
    @GET("pengukuran")
    Call<PengukuranResponse> getPengukuran();

    // ==================== COMPREHENSIVE ENDPOINTS ====================
    @GET("all_data")
    Call<AllDataResponse> getAllData();

    @GET("sync")
    Call<AllDataResponse> syncData();

    @GET("by_pengukuran/{id_pengukuran}")
    Call<ByPengukuranResponse> getByPengukuran(@Path("id_pengukuran") int id_pengukuran);
}