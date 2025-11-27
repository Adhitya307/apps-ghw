package com.example.app_rightpiezo;

public class T_pengukuran_rightpiez {
    private int id_pengukuran;
    private int tahun;
    private String tanggal;
    private String periode;
    private double tma;
    private double ch_hujan;
    private String temp_id;

    // Getters and Setters
    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public int getTahun() { return tahun; }
    public void setTahun(int tahun) { this.tahun = tahun; }

    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public double getTma() { return tma; }
    public void setTma(double tma) { this.tma = tma; }

    public double getCh_hujan() { return ch_hujan; }
    public void setCh_hujan(double ch_hujan) { this.ch_hujan = ch_hujan; }

    public String getTemp_id() { return temp_id; }
    public void setTemp_id(String temp_id) { this.temp_id = temp_id; }
}