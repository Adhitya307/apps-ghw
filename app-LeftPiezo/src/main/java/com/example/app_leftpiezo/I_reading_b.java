package com.example.app_leftpiezo;

public class I_reading_b {
    private int id_reading_B;
    private int id_pengukuran;
    private String titik_piezometer;
    private double Elv_Piez;
    private String created_at;
    private String updated_at;

    // Constructor
    public I_reading_b() {}

    // Getters and Setters
    public int getId_reading_B() { return id_reading_B; }
    public void setId_reading_B(int id_reading_B) { this.id_reading_B = id_reading_B; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public String getTitik_piezometer() { return titik_piezometer; }
    public void setTitik_piezometer(String titik_piezometer) { this.titik_piezometer = titik_piezometer; }

    public double getElv_Piez() { return Elv_Piez; }
    public void setElv_Piez(double Elv_Piez) { this.Elv_Piez = Elv_Piez; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}