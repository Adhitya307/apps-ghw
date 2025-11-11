package com.apps.bubbletilt;

public class PerhitunganBt6Model {
    private int id_perhitungan;
    private int id_pengukuran;
    private double A_sec;
    private double sin_A_rad;
    private double B_sec;
    private double sin_B_rad;
    private double sin_C_rad;
    private double sin_C_deg;
    private double Cosa;
    private double a_rad;
    private String DMS;
    private String created_at;
    private String updated_at;

    public PerhitunganBt6Model() {}

    public int getId_perhitungan() { return id_perhitungan; }
    public void setId_perhitungan(int id_perhitungan) { this.id_perhitungan = id_perhitungan; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public double getA_sec() { return A_sec; }
    public void setA_sec(double A_sec) { this.A_sec = A_sec; }

    public double getSin_A_rad() { return sin_A_rad; }
    public void setSin_A_rad(double sin_A_rad) { this.sin_A_rad = sin_A_rad; }

    public double getB_sec() { return B_sec; }
    public void setB_sec(double B_sec) { this.B_sec = B_sec; }

    public double getSin_B_rad() { return sin_B_rad; }
    public void setSin_B_rad(double sin_B_rad) { this.sin_B_rad = sin_B_rad; }

    public double getSin_C_rad() { return sin_C_rad; }
    public void setSin_C_rad(double sin_C_rad) { this.sin_C_rad = sin_C_rad; }

    public double getSin_C_deg() { return sin_C_deg; }
    public void setSin_C_deg(double sin_C_deg) { this.sin_C_deg = sin_C_deg; }

    public double getCosa() { return Cosa; }
    public void setCosa(double Cosa) { this.Cosa = Cosa; }

    public double getA_rad() { return a_rad; }
    public void setA_rad(double a_rad) { this.a_rad = a_rad; }

    public String getDMS() { return DMS; }
    public void setDMS(String DMS) { this.DMS = DMS; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}