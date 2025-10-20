package com.apps.ghw.rembesan;

import com.google.gson.annotations.SerializedName;

public class PSpillwayModel {
    @SerializedName("id")
    private int id;

    @SerializedName("pengukuran_id")
    private int pengukuran_id;

    @SerializedName("B3")
    private double B3;

    @SerializedName("ambang")
    private double ambang;

    @SerializedName("created_at")
    private String created_at;

    @SerializedName("updated_at")
    private String updated_at;

    // Getter and Setter methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPengukuran_id() {
        return pengukuran_id;
    }

    public void setPengukuran_id(int pengukuran_id) {
        this.pengukuran_id = pengukuran_id;
    }

    public double getB3() {
        return B3;
    }

    public void setB3(double B3) {
        this.B3 = B3;
    }

    public double getAmbang() {
        return ambang;
    }

    public void setAmbang(double ambang) {
        this.ambang = ambang;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }
}