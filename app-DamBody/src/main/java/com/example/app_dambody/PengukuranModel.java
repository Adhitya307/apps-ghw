package com.example.app_dambody;

public class PengukuranModel {
    private int id_pengukuran;
    private int tahun;
    private String periode;
    private String tanggal;
    private String dma;
    private String temp_id;
    private String created_at;
    private String updated_at;

    public PengukuranModel() {}

    // Getters and Setters
    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public int getTahun() { return tahun; }
    public void setTahun(int tahun) { this.tahun = tahun; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }

    public String getDma() { return dma; }
    public void setDma(String dma) { this.dma = dma; }

    public String getTemp_id() { return temp_id; }
    public void setTemp_id(String temp_id) { this.temp_id = temp_id; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}