package com.example.app_dambody;

public class AmbangBatas625H1Model {
    private int id_ambang_batas;
    private int id_pengukuran;
    private double aman;
    private double peringatan;
    private double bahaya;
    private Double pergerakan;
    private String created_at;
    private String updated_at;

    public AmbangBatas625H1Model() {}

    // Getter dan Setter
    public int getId_ambang_batas() { return id_ambang_batas; }
    public void setId_ambang_batas(int id_ambang_batas) { this.id_ambang_batas = id_ambang_batas; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public double getAman() { return aman; }
    public void setAman(double aman) { this.aman = aman; }

    public double getPeringatan() { return peringatan; }
    public void setPeringatan(double peringatan) { this.peringatan = peringatan; }

    public double getBahaya() { return bahaya; }
    public void setBahaya(double bahaya) { this.bahaya = bahaya; }

    public Double getPergerakan() { return pergerakan; }
    public void setPergerakan(Double pergerakan) { this.pergerakan = pergerakan; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}