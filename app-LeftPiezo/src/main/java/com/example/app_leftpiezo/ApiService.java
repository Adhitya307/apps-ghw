package com.example.app_leftpiezo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;

public interface ApiService {

    // ==================== MASTER DATA - PENGUKURAN ====================

    @GET("pengukuran-leftpiez")
    Call<ApiResponse<List<T_pengukuran_leftpiez>>> getPengukuranLeftPiez();

    @GET("pengukuran-leftpiez/{id}")
    Call<ApiResponse<T_pengukuran_leftpiez>> getPengukuranById(@Path("id") int id);

    // ==================== READING DATA - I_READING A & B ====================

    @GET("ireading-a")
    Call<ApiResponse<List<I_reading_a>>> getIReadingA();

    @GET("ireading-a/by_pengukuran/{id_pengukuran}")
    Call<ApiResponse<List<I_reading_a>>> getIReadingAByPengukuran(@Path("id_pengukuran") int idPengukuran);

    @GET("ireading-b")
    Call<ApiResponse<List<I_reading_b>>> getIReadingB();

    @GET("ireading-b/by_pengukuran/{id_pengukuran}")
    Call<ApiResponse<List<I_reading_b>>> getIReadingBByPengukuran(@Path("id_pengukuran") int idPengukuran);

    // ==================== INPUT DATA - T_PEMBACAAN ====================

    @GET("tpembacaan")
    Call<ApiResponse<List<TPembacaanLeftPiez>>> getTPembacaan();

    @GET("tpembacaan/by_pengukuran/{id_pengukuran}")
    Call<ApiResponse<List<TPembacaanLeftPiez>>> getTPembacaanByPengukuran(@Path("id_pengukuran") int idPengukuran);

    // ==================== CALCULATION DATA ====================

    @GET("bpiezo-metrik")
    Call<ApiResponse<List<B_piezo_metrik>>> getBPiezoMetrik();

    @GET("bpiezo-metrik/by_pengukuran/{id_pengukuran}")
    Call<ApiResponse<B_piezo_metrik>> getBPiezoMetrikByPengukuran(@Path("id_pengukuran") int idPengukuran);

    @GET("perhitungan-leftpiez")
    Call<ApiResponse<List<Perhitungan_left_piez>>> getPerhitunganLeftPiez();

    @GET("perhitungan-leftpiez/by_pengukuran/{id_pengukuran}")
    Call<ApiResponse<List<Perhitungan_left_piez>>> getPerhitunganLeftPiezByPengukuran(@Path("id_pengukuran") int idPengukuran);

    // ==================== COMBINED DATA ====================

    @GET("detail/{id_pengukuran}")
    Call<ApiResponse<Object>> getDetailByPengukuran(@Path("id_pengukuran") int idPengukuran);

    @GET("all")
    Call<ApiResponse<Object>> getAllData();

    @GET("sync")
    Call<ApiResponse<Object>> getSyncData(@Query("last_sync") String lastSync);

    // ==================== UTILITY ENDPOINTS ====================

    @GET("health")
    Call<ApiResponse<Object>> getHealthStatus();

    @GET("latest")
    Call<ApiResponse<T_pengukuran_leftpiez>> getLatestPengukuran();

    @GET("statistics")
    Call<ApiResponse<Object>> getStatistics();

    @GET("piezometer-points")
    Call<ApiResponse<Object>> getPiezometerPoints();

    @GET("by_date")
    Call<ApiResponse<List<T_pengukuran_leftpiez>>> getDataByDateRange(
            @Query("start") String startDate,
            @Query("end") String endDate);

    @GET("by_tahun/{tahun}")
    Call<ApiResponse<List<T_pengukuran_leftpiez>>> getDataByTahun(@Path("tahun") String tahun);

    @GET("by_periode/{periode}")
    Call<ApiResponse<List<T_pengukuran_leftpiez>>> getDataByPeriode(@Path("periode") String periode);
}