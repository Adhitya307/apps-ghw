package com.apps.ghw.rembesan;

import com.google.gson.annotations.SerializedName;

public class PThomsonWeirModel {
    @SerializedName("id")
    private int id;

    @SerializedName("a1_r")
    private double a1_r;

    @SerializedName("a1_l")
    private double a1_l;

    @SerializedName("b1")
    private double b1;

    @SerializedName("b3")
    private double b3;

    @SerializedName("b5")
    private double b5;

    @SerializedName("pengukuran_id")
    private int pengukuran_id;

    // Getter and Setter methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getA1_r() {
        return a1_r;
    }

    public void setA1_r(double a1_r) {
        this.a1_r = a1_r;
    }

    public double getA1_l() {
        return a1_l;
    }

    public void setA1_l(double a1_l) {
        this.a1_l = a1_l;
    }

    public double getB1() {
        return b1;
    }

    public void setB1(double b1) {
        this.b1 = b1;
    }

    public double getB3() {
        return b3;
    }

    public void setB3(double b3) {
        this.b3 = b3;
    }

    public double getB5() {
        return b5;
    }

    public void setB5(double b5) {
        this.b5 = b5;
    }

    public int getPengukuran_id() {
        return pengukuran_id;
    }

    public void setPengukuran_id(int pengukuran_id) {
        this.pengukuran_id = pengukuran_id;
    }
}