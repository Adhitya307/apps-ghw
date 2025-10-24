package com.example.app_dambody;

public class Pergerakan625Model {
    private int id_pergerakan;
    private int id_pengukuran;
    private Double hv_1;
    private Double hv_2;
    private Double hv_3;
    private String created_at;
    private String updated_at;

    // Getters and Setters
    public int getId_pergerakan() { return id_pergerakan; }
    public void setId_pergerakan(int id_pergerakan) { this.id_pergerakan = id_pergerakan; }
    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }
    public Double getHv_1() { return hv_1; }
    public void setHv_1(Double hv_1) { this.hv_1 = hv_1; }
    public Double getHv_2() { return hv_2; }
    public void setHv_2(Double hv_2) { this.hv_2 = hv_2; }
    public Double getHv_3() { return hv_3; }
    public void setHv_3(Double hv_3) { this.hv_3 = hv_3; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}