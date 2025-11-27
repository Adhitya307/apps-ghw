package com.example.app.exstenso;

import com.google.gson.annotations.SerializedName;

public class DeformasiEx1Model {
    @SerializedName("id_deformasi_ex1")
    private int idDeformasiEx1;

    @SerializedName("id_pengukuran")
    private int idPengukuran;

    @SerializedName("deformasi_10")
    private double deformasi10;

    @SerializedName("deformasi_20")
    private double deformasi20;

    @SerializedName("deformasi_30")
    private double deformasi30;

    @SerializedName("pemb_awal10")
    private double pembAwal10;

    @SerializedName("pemb_awal20")
    private double pembAwal20;

    @SerializedName("pemb_awal30")
    private double pembAwal30;

    public DeformasiEx1Model() {}

    public DeformasiEx1Model(int idDeformasiEx1, int idPengukuran, double deformasi10,
                             double deformasi20, double deformasi30, double pembAwal10,
                             double pembAwal20, double pembAwal30) {
        this.idDeformasiEx1 = idDeformasiEx1;
        this.idPengukuran = idPengukuran;
        this.deformasi10 = deformasi10;
        this.deformasi20 = deformasi20;
        this.deformasi30 = deformasi30;
        this.pembAwal10 = pembAwal10;
        this.pembAwal20 = pembAwal20;
        this.pembAwal30 = pembAwal30;
    }

    public int getIdDeformasiEx1() {
        return idDeformasiEx1;
    }

    public void setIdDeformasiEx1(int idDeformasiEx1) {
        this.idDeformasiEx1 = idDeformasiEx1;
    }

    public int getIdPengukuran() {
        return idPengukuran;
    }

    public void setIdPengukuran(int idPengukuran) {
        this.idPengukuran = idPengukuran;
    }

    public double getDeformasi10() {
        return deformasi10;
    }

    public void setDeformasi10(double deformasi10) {
        this.deformasi10 = deformasi10;
    }

    public double getDeformasi20() {
        return deformasi20;
    }

    public void setDeformasi20(double deformasi20) {
        this.deformasi20 = deformasi20;
    }

    public double getDeformasi30() {
        return deformasi30;
    }

    public void setDeformasi30(double deformasi30) {
        this.deformasi30 = deformasi30;
    }

    public double getPembAwal10() {
        return pembAwal10;
    }

    public void setPembAwal10(double pembAwal10) {
        this.pembAwal10 = pembAwal10;
    }

    public double getPembAwal20() {
        return pembAwal20;
    }

    public void setPembAwal20(double pembAwal20) {
        this.pembAwal20 = pembAwal20;
    }

    public double getPembAwal30() {
        return pembAwal30;
    }

    public void setPembAwal30(double pembAwal30) {
        this.pembAwal30 = pembAwal30;
    }

    @Override
    public String toString() {
        return "DeformasiEx1Model{" +
                "idDeformasiEx1=" + idDeformasiEx1 +
                ", idPengukuran=" + idPengukuran +
                ", deformasi10=" + deformasi10 +
                ", deformasi20=" + deformasi20 +
                ", deformasi30=" + deformasi30 +
                ", pembAwal10=" + pembAwal10 +
                ", pembAwal20=" + pembAwal20 +
                ", pembAwal30=" + pembAwal30 +
                '}';
    }
}