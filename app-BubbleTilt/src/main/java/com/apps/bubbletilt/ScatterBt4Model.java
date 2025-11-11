package com.apps.bubbletilt;

public class ScatterBt4Model {
    private int id_scatter;
    private int id_pengukuran;
    private double Y_US;
    private double X_TB;
    private double Y_cum;
    private double X_cum;
    private String created_at;
    private String updated_at;

    public ScatterBt4Model() {}

    public int getId_scatter() { return id_scatter; }
    public void setId_scatter(int id_scatter) { this.id_scatter = id_scatter; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public double getY_US() { return Y_US; }
    public void setY_US(double Y_US) { this.Y_US = Y_US; }

    public double getX_TB() { return X_TB; }
    public void setX_TB(double X_TB) { this.X_TB = X_TB; }

    public double getY_cum() { return Y_cum; }
    public void setY_cum(double Y_cum) { this.Y_cum = Y_cum; }

    public double getX_cum() { return X_cum; }
    public void setX_cum(double X_cum) { this.X_cum = X_cum; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}