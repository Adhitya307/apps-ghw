package com.example.app_dambody;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    // Endpoint untuk data pengukuran
    @GET("api/dambody/pengukuran")
    Call<PengukuranResponse> getPengukuran();

    // Endpoint untuk data pembacaan ELV 625
    @GET("api/dambody/pembacaan-625")
    Call<Pembacaan625Response> getPembacaan625();

    // Endpoint untuk data pembacaan ELV 600
    @GET("api/dambody/pembacaan-600")
    Call<Pembacaan600Response> getPembacaan600();

    // Endpoint untuk data depth ELV 625
    @GET("api/dambody/depth-625")
    Call<Depth625Response> getDepth625();

    // Endpoint untuk data depth ELV 600
    @GET("api/dambody/depth-600")
    Call<Depth600Response> getDepth600();

    // Endpoint untuk data initial reading ELV 625
    @GET("api/dambody/initial-625")
    Call<Initial625Response> getInitial625();

    // Endpoint untuk data initial reading ELV 600
    @GET("api/dambody/initial-600")
    Call<Initial600Response> getInitial600();

    // Endpoint untuk data pergerakan ELV 625
    @GET("api/dambody/pergerakan-625")
    Call<Pergerakan625Response> getPergerakan625();

    // Endpoint untuk data pergerakan ELV 600
    @GET("api/dambody/pergerakan-600")
    Call<Pergerakan600Response> getPergerakan600();

    // Endpoint untuk sync semua data
    @GET("api/dambody/sync")
    Call<SyncResponse> getSyncData(@Query("last_sync") String lastSync);
}