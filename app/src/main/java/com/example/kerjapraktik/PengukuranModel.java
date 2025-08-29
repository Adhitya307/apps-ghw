package com.example.kerjapraktik;

import com.google.gson.annotations.SerializedName;

public class PengukuranModel {
    private int id;

    @SerializedName("tahun")
    private String tahun;

    @SerializedName("bulan")
    private String bulan;

    @SerializedName("periode")
    private String periode;

    @SerializedName("tanggal")
    private String tanggal;

    @SerializedName("tma_waduk")
    private Double tmaWaduk;

    @SerializedName("curah_hujan")
    private Double curahHujan;

    @SerializedName("temp_id")
    private String tempId;

    // === Getter & Setter ===
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTahun() { return tahun; }
    public void setTahun(String tahun) { this.tahun = tahun; }

    public String getBulan() { return bulan; }
    public void setBulan(String bulan) { this.bulan = bulan; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }

    public Double getTmaWaduk() { return tmaWaduk; }
    public void setTmaWaduk(Double tmaWaduk) { this.tmaWaduk = tmaWaduk; }

    public Double getCurahHujan() { return curahHujan; }
    public void setCurahHujan(Double curahHujan) { this.curahHujan = curahHujan; }

    public String getTempId() { return tempId; }
    public void setTempId(String tempId) { this.tempId = tempId; }
}
