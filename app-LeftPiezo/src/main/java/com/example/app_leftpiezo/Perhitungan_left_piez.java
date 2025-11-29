package com.example.app_leftpiezo;

public class Perhitungan_left_piez {
    private int id_perhitungan;
    private int id_pengukuran;
    private String tipe_piezometer;
    private double elv_piez;
    private double kedalaman;
    private double record_max;
    private double record_min;
    private double koordinat_x;
    private double koordinat_y;
    private double t_psmetrik;
    private String created_at;
    private String updated_at;

    // Constructor
    public Perhitungan_left_piez() {}

    // Getters and Setters
    public int getId_perhitungan() { return id_perhitungan; }
    public void setId_perhitungan(int id_perhitungan) { this.id_perhitungan = id_perhitungan; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public String getTipe_piezometer() { return tipe_piezometer; }
    public void setTipe_piezometer(String tipe_piezometer) { this.tipe_piezometer = tipe_piezometer; }

    public double getElv_piez() { return elv_piez; }
    public void setElv_piez(double elv_piez) { this.elv_piez = elv_piez; }

    public double getKedalaman() { return kedalaman; }
    public void setKedalaman(double kedalaman) { this.kedalaman = kedalaman; }

    public double getRecord_max() { return record_max; }
    public void setRecord_max(double record_max) { this.record_max = record_max; }

    public double getRecord_min() { return record_min; }
    public void setRecord_min(double record_min) { this.record_min = record_min; }

    public double getKoordinat_x() { return koordinat_x; }
    public void setKoordinat_x(double koordinat_x) { this.koordinat_x = koordinat_x; }

    public double getKoordinat_y() { return koordinat_y; }
    public void setKoordinat_y(double koordinat_y) { this.koordinat_y = koordinat_y; }

    public double getT_psmetrik() { return t_psmetrik; }
    public void setT_psmetrik(double t_psmetrik) { this.t_psmetrik = t_psmetrik; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}