package com.example.app_rightpiezo;

public class I_reading_atas {
    private int id_reading_atas;
    private int id_pengukuran;
    private String titik_piezometer;
    private double Elv_Piez;
    private double kedalaman;
    private String created_at;
    private String updated_at;

    // Getters and Setters
    public int getId_reading_atas() { return id_reading_atas; }
    public void setId_reading_atas(int id_reading_atas) { this.id_reading_atas = id_reading_atas; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public String getTitik_piezometer() { return titik_piezometer; }
    public void setTitik_piezometer(String titik_piezometer) { this.titik_piezometer = titik_piezometer; }

    public double getElv_Piez() { return Elv_Piez; }
    public void setElv_Piez(double Elv_Piez) { this.Elv_Piez = Elv_Piez; }

    public double getKedalaman() { return kedalaman; }
    public void setKedalaman(double kedalaman) { this.kedalaman = kedalaman; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}