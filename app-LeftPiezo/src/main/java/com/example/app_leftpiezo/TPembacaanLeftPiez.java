package com.example.app_leftpiezo;

public class TPembacaanLeftPiez {
    private int id_pembacaan;
    private int id_pengukuran;
    private String tipe_piezometer;
    private String feet;
    private String inch;
    private String created_at;
    private String updated_at;

    // Constructor
    public TPembacaanLeftPiez() {}

    // Getters and Setters
    public int getId_pembacaan() { return id_pembacaan; }
    public void setId_pembacaan(int id_pembacaan) { this.id_pembacaan = id_pembacaan; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public String getTipe_piezometer() { return tipe_piezometer; }
    public void setTipe_piezometer(String tipe_piezometer) { this.tipe_piezometer = tipe_piezometer; }

    public String getFeet() { return feet; }
    public void setFeet(String feet) { this.feet = feet; }

    public String getInch() { return inch; }
    public void setInch(String inch) { this.inch = inch; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}