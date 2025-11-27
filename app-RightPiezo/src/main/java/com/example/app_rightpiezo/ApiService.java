package com.example.app_rightpiezo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;

public interface ApiService {

    // ==================== MASTER DATA ====================
    @GET("pengukuran")
    Call<ApiResponse<List<T_pengukuran_rightpiez>>> getPengukuranRightPiez();

    // ✅ DIPERBAIKI: Gunakan path parameter sesuai controller
    @GET("pengukuran/{id}")
    Call<ApiResponse<T_pengukuran_rightpiez>> getPengukuranById(@Path("id") int idPengukuran);

    // ==================== READING DATA ====================
    @GET("ireading")
    Call<ApiResponse<List<I_reading_atas>>> getIReadingAtas();

    // ✅ DIPERBAIKI: Gunakan path parameter sesuai controller
    @GET("ireading/by_pengukuran/{id}")
    Call<ApiResponse<List<I_reading_atas>>> getIReadingByPengukuran(@Path("id") int idPengukuran);

    // ✅ ENDPOINT BARU: by titik
    @GET("ireading/by_titik/{titik}")
    Call<ApiResponse<List<I_reading_atas>>> getIReadingByTitik(@Path("titik") String titik);

    // ==================== INPUT DATA ====================
    @GET("tpembacaan")
    Call<ApiResponse<List<T_pembacaan>>> getTPembacaan();

    // ✅ DIPERBAIKI: Gunakan path parameter sesuai controller
    @GET("tpembacaan/by_pengukuran/{id}")
    Call<ApiResponse<List<T_pembacaan>>> getTPembacaanByPengukuran(@Path("id") int idPengukuran);

    // ✅ ENDPOINT BARU: by lokasi
    @GET("tpembacaan/by_lokasi/{lokasi}")
    Call<ApiResponse<List<T_pembacaan>>> getTPembacaanByLokasi(@Path("lokasi") String lokasi);

    // ==================== CALCULATION DATA ====================
    @GET("bpiezometrik")
    Call<ApiResponse<List<B_piezo_metrik>>> getBPiezoMetrik();

    // ✅ DIPERBAIKI: Gunakan path parameter sesuai controller
    @GET("bpiezometrik/by_pengukuran/{id}")
    Call<ApiResponse<B_piezo_metrik>> getBPiezoMetrikByPengukuran(@Path("id") int idPengukuran);

    @GET("perhitunganpsmetrik")
    Call<ApiResponse<List<Perhitungan_t_psmetrik>>> getPerhitunganPsMetrik();

    // ✅ DIPERBAIKI: Gunakan path parameter sesuai controller
    @GET("perhitunganpsmetrik/by_pengukuran/{id}")
    Call<ApiResponse<Perhitungan_t_psmetrik>> getPerhitunganPsMetrikByPengukuran(@Path("id") int idPengukuran);

    // ==================== COMBINED DATA ====================
    // ✅ ENDPOINT BARU: Detail lengkap
    @GET("detail/{id}")
    Call<ApiResponse<Object>> getDetailByPengukuran(@Path("id") int idPengukuran);

    // ✅ ENDPOINT BARU: Semua data
    @GET("all")
    Call<ApiResponse<Object>> getAllData();

    // ✅ ENDPOINT BARU: Sync data
    @GET("sync")
    Call<ApiResponse<Object>> getSyncData(@Query("last_sync") String lastSync);

    // ==================== UTILITY ENDPOINTS ====================
    // ✅ ENDPOINT BARU: Health check
    @GET("health")
    Call<ApiResponse<Object>> getHealthStatus();

    // ✅ ENDPOINT BARU: Data terbaru
    @GET("latest")
    Call<ApiResponse<T_pengukuran_rightpiez>> getLatestPengukuran();

    // ✅ ENDPOINT BARU: By date range
    @GET("by_date")
    Call<ApiResponse<List<T_pengukuran_rightpiez>>> getDataByDateRange(
            @Query("start") String startDate,
            @Query("end") String endDate);

    // ✅ ENDPOINT BARU: Statistics
    @GET("statistics")
    Call<ApiResponse<Object>> getStatistics();
}