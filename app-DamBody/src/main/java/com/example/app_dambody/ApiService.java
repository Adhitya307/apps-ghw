package com.example.app_dambody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
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

    // =============================================================
    // ✅ ENDPOINT AMBANG BATAS BARU
    // =============================================================

    // Ambang Batas 625
    @GET("api/dambody/ambang-batas-625-h1")
    Call<AmbangBatas625H1Response> getAmbangBatas625H1();

    @GET("api/dambody/ambang-batas-625-h2")
    Call<AmbangBatas625H2Response> getAmbangBatas625H2();

    @GET("api/dambody/ambang-batas-625-h3")
    Call<AmbangBatas625H3Response> getAmbangBatas625H3();

    // Ambang Batas 600
    @GET("api/dambody/ambang-batas-600-h1")
    Call<AmbangBatas600H1Response> getAmbangBatas600H1();

    @GET("api/dambody/ambang-batas-600-h2")
    Call<AmbangBatas600H2Response> getAmbangBatas600H2();

    @GET("api/dambody/ambang-batas-600-h3")
    Call<AmbangBatas600H3Response> getAmbangBatas600H3();

    @GET("api/dambody/ambang-batas-600-h4")
    Call<AmbangBatas600H4Response> getAmbangBatas600H4();

    @GET("api/dambody/ambang-batas-600-h5")
    Call<AmbangBatas600H5Response> getAmbangBatas600H5();

    // Endpoint untuk sync semua data
    @GET("api/dambody/sync")
    Call<SyncResponse> getSyncData(@Query("last_sync") String lastSync);

    // Update Ambang Batas (gunakan sesuai kebutuhan)
    @POST("ambangbatas625/h1/update")
    Call<AmbangBatas625H1Response> updateAmbangBatas625H1(@Body AmbangBatas625H1Model ambangBatas);

    @POST("ambangbatas600/h1/update")
    Call<AmbangBatas600H1Response> updateAmbangBatas600H1(@Body AmbangBatas600H1Model ambangBatas);
}