package com.example.app.exstenso;

import retrofit2.Call;
import retrofit2.http.GET;
import java.util.List;

public interface ExstensoApiService {

    // Pengukuran
    @GET("api/exstenso/pengukuran-eks")
    Call<ApiResponse<List<PengukuranEksModel>>> getPengukuranEks();

    // Pembacaan
    @GET("api/exstenso/pembacaan-ex1")
    Call<ApiResponse<List<PembacaanEx1Model>>> getPembacaanEx1();

    @GET("api/exstenso/pembacaan-ex2")
    Call<ApiResponse<List<PembacaanEx2Model>>> getPembacaanEx2();

    @GET("api/exstenso/pembacaan-ex3")
    Call<ApiResponse<List<PembacaanEx3Model>>> getPembacaanEx3();

    @GET("api/exstenso/pembacaan-ex4")
    Call<ApiResponse<List<PembacaanEx4Model>>> getPembacaanEx4();

    // Deformasi
    @GET("api/exstenso/deformasi-ex1")
    Call<ApiResponse<List<DeformasiEx1Model>>> getDeformasiEx1();

    @GET("api/exstenso/deformasi-ex2")
    Call<ApiResponse<List<DeformasiEx2Model>>> getDeformasiEx2();

    @GET("api/exstenso/deformasi-ex3")
    Call<ApiResponse<List<DeformasiEx3Model>>> getDeformasiEx3();

    @GET("api/exstenso/deformasi-ex4")
    Call<ApiResponse<List<DeformasiEx4Model>>> getDeformasiEx4();

    // Readings
    @GET("api/exstenso/readings-ex1")
    Call<ApiResponse<List<ReadingsEx1Model>>> getReadingsEx1();

    @GET("api/exstenso/readings-ex2")
    Call<ApiResponse<List<ReadingsEx2Model>>> getReadingsEx2();

    @GET("api/exstenso/readings-ex3")
    Call<ApiResponse<List<ReadingsEx3Model>>> getReadingsEx3();

    @GET("api/exstenso/readings-ex4")
    Call<ApiResponse<List<ReadingsEx4Model>>> getReadingsEx4();
}