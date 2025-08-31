package com.example.kerjapraktik;

import com.google.gson.annotations.SerializedName;

public class PIntiGalleryModel {
    @SerializedName("id")
    private int id;

    @SerializedName("pengukuran_id")
    private int pengukuran_id;

    @SerializedName("a1")
    private double a1;

    @SerializedName("ambang_a1")
    private double ambang_a1;

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

    public double getA1() {
        return a1;
    }

    public void setA1(double a1) {
        this.a1 = a1;
    }

    public double getAmbang_a1() {
        return ambang_a1;
    }

    public void setAmbang_a1(double ambang_a1) {
        this.ambang_a1 = ambang_a1;
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