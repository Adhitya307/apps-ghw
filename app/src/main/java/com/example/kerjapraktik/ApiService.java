package com.example.kerjapraktik;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
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

}
