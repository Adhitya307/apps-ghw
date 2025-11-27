package com.example.app_rightpiezo;

public class T_pembacaan {
    private int id_bacaan;
    private int id_pengukuran;
    private String lokasi;
    private String feet;
    private String inch;

    // Getters and Setters
    public int getId_bacaan() { return id_bacaan; }
    public void setId_bacaan(int id_bacaan) { this.id_bacaan = id_bacaan; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public String getLokasi() { return lokasi; }
    public void setLokasi(String lokasi) { this.lokasi = lokasi; }

    public String getFeet() { return feet; }
    public void setFeet(String feet) { this.feet = feet; }

    public String getInch() { return inch; }
    public void setInch(String inch) { this.inch = inch; }
}