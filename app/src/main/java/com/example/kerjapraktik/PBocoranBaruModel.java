package com.example.kerjapraktik;

import com.google.gson.annotations.SerializedName;

public class PBocoranBaruModel {
    @SerializedName("id")
    private int id;

    @SerializedName("pengukuran_id")
    private int pengukuran_id;

    @SerializedName("talang1")
    private double talang1;

    @SerializedName("talang2")
    private double talang2;

    @SerializedName("pipa")
    private double pipa;

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

    public double getTalang1() {
        return talang1;
    }

    public void setTalang1(double talang1) {
        this.talang1 = talang1;
    }

    public double getTalang2() {
        return talang2;
    }

    public void setTalang2(double talang2) {
        this.talang2 = talang2;
    }

    public double getPipa() {
        return pipa;
    }

    public void setPipa(double pipa) {
        this.pipa = pipa;
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