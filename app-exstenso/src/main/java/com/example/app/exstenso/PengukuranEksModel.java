package com.example.app.exstenso;

import com.google.gson.annotations.SerializedName;

public class PengukuranEksModel {
    @SerializedName("id_pengukuran")
    private int idPengukuran;

    @SerializedName("tahun")
    private String tahun;

    @SerializedName("periode")
    private String periode;

    @SerializedName("tanggal")
    private String tanggal;

    @SerializedName("dma")
    private String dma;

    @SerializedName("temp_id")
    private String tempId;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Default constructor
    public PengukuranEksModel() {}

    // Constructor with parameters
    public PengukuranEksModel(int idPengukuran, String tahun, String periode, String tanggal,
                              String dma, String tempId, String createdAt, String updatedAt) {
        this.idPengukuran = idPengukuran;
        this.tahun = tahun;
        this.periode = periode;
        this.tanggal = tanggal;
        this.dma = dma;
        this.tempId = tempId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getIdPengukuran() {
        return idPengukuran;
    }

    public void setIdPengukuran(int idPengukuran) {
        this.idPengukuran = idPengukuran;
    }

    public String getTahun() {
        return tahun;
    }

    public void setTahun(String tahun) {
        this.tahun = tahun;
    }

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public String getTanggal() {
        return tanggal;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public String getDma() {
        return dma;
    }

    public void setDma(String dma) {
        this.dma = dma;
    }

    public String getTempId() {
        return tempId;
    }

    public void setTempId(String tempId) {
        this.tempId = tempId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "PengukuranEksModel{" +
                "idPengukuran=" + idPengukuran +
                ", tahun='" + tahun + '\'' +
                ", periode='" + periode + '\'' +
                ", tanggal='" + tanggal + '\'' +
                ", dma='" + dma + '\'' +
                ", tempId='" + tempId + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}