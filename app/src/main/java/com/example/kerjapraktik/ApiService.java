package com.example.kerjapraktik;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    // === DATA INPUT ===

    // Pengukuran (sudah dibungkus dalam PengukuranResponse)
    @GET("api/rembesan/backup/pengukuran")
    Call<PengukuranResponse> getPengukuran();

    // Thomson (gunakan ThomsonResponse)
    @GET("api/rembesan/backup/thomson")
    Call<ThomsonResponse> getThomson();

    // SR (gunakan SRResponse)
    @GET("api/rembesan/backup/sr")
    Call<SRResponse> getSR();

    // Bocoran (gunakan BocoranResponse)
    @GET("api/rembesan/backup/bocoran")
    Call<BocoranResponse> getBocoran();

    // === DATA HASIL PERHITUNGAN (P_) ===

    // Batas Maksimal
    @GET("api/rembesan/backup/p_batasmaksimal")
    Call<BatasMaksimalResponse> getBatasMaksimal();

    // Bocoran Baru (hasil)
    @GET("api/rembesan/backup/p_bocoran_baru")
    Call<PBocoranBaruResponse> getPBocoranBaru();

    // Inti Gallery
    @GET("api/rembesan/backup/p_intigalery")
    Call<PIntiGalleryResponse> getPIntiGallery();

    // Spillway
    @GET("api/rembesan/backup/p_spillway")
    Call<PSpillwayResponse> getPSpillway();

    // SR (hasil)
    @GET("api/rembesan/backup/p_sr")
    Call<PSRResponse> getPSR();

    // Tebing Kanan
    @GET("api/rembesan/backup/p_tebingkanan")
    Call<PTebingKananResponse> getPTebingKanan();

    // Thomson Weir (hasil)
    @GET("api/rembesan/backup/p_thomson_weir")
    Call<PThomsonWeirResponse> getPThomsonWeir();

    // Total Bocoran
    @GET("api/rembesan/backup/p_totalbocoran")
    Call<PTotalBocoranResponse> getPTotalBocoran();

    // Retrofit API
    @GET("api/rembesan/analisa_look_burt")
    Call<AnalisaLookBurtResponse> getAnalisaLookBurt();

}