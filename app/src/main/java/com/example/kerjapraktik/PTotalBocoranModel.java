package com.example.kerjapraktik;

import com.google.gson.annotations.SerializedName;

public class PTotalBocoranModel {
    @SerializedName("id")
    private int id;

    @SerializedName("pengukuran_id")
    private int pengukuran_id;

    @SerializedName("R1")
    private double R1;

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

    public double getR1() {
        return R1;
    }

    public void setR1(double R1) {
        this.R1 = R1;
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