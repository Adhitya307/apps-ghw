package com.example.kerjapraktik;

import com.google.gson.annotations.SerializedName;

public class BatasMaksimalModel {
    @SerializedName("id")
    private int id;

    @SerializedName("pengukuran_id")
    private int pengukuran_id;

    @SerializedName("batas_maksimal")
    private double batas_maksimal;

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

    public double getBatas_maksimal() {
        return batas_maksimal;
    }

    public void setBatas_maksimal(double batas_maksimal) {
        this.batas_maksimal = batas_maksimal;
    }
}