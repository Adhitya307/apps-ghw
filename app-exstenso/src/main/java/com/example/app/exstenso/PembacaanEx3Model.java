package com.example.app.exstenso;

import com.google.gson.annotations.SerializedName;

public class PembacaanEx3Model {
    @SerializedName("id_pembacaan_ex3")
    private int idPembacaanEx3;

    @SerializedName("id_pengukuran")
    private int idPengukuran;

    @SerializedName("pembacaan_10")
    private double pembacaan10;

    @SerializedName("pembacaan_20")
    private double pembacaan20;

    @SerializedName("pembacaan_30")
    private double pembacaan30;

    public PembacaanEx3Model() {}

    public PembacaanEx3Model(int idPembacaanEx3, int idPengukuran, double pembacaan10,
                             double pembacaan20, double pembacaan30) {
        this.idPembacaanEx3 = idPembacaanEx3;
        this.idPengukuran = idPengukuran;
        this.pembacaan10 = pembacaan10;
        this.pembacaan20 = pembacaan20;
        this.pembacaan30 = pembacaan30;
    }

    public int getIdPembacaanEx3() {
        return idPembacaanEx3;
    }

    public void setIdPembacaanEx3(int idPembacaanEx3) {
        this.idPembacaanEx3 = idPembacaanEx3;
    }

    public int getIdPengukuran() {
        return idPengukuran;
    }

    public void setIdPengukuran(int idPengukuran) {
        this.idPengukuran = idPengukuran;
    }

    public double getPembacaan10() {
        return pembacaan10;
    }

    public void setPembacaan10(double pembacaan10) {
        this.pembacaan10 = pembacaan10;
    }

    public double getPembacaan20() {
        return pembacaan20;
    }

    public void setPembacaan20(double pembacaan20) {
        this.pembacaan20 = pembacaan20;
    }

    public double getPembacaan30() {
        return pembacaan30;
    }

    public void setPembacaan30(double pembacaan30) {
        this.pembacaan30 = pembacaan30;
    }

    @Override
    public String toString() {
        return "PembacaanEx3Model{" +
                "idPembacaanEx3=" + idPembacaanEx3 +
                ", idPengukuran=" + idPengukuran +
                ", pembacaan10=" + pembacaan10 +
                ", pembacaan20=" + pembacaan20 +
                ", pembacaan30=" + pembacaan30 +
                '}';
    }
}