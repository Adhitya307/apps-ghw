package com.example.app.exstenso;

import com.google.gson.annotations.SerializedName;

public class ReadingsEx1Model {
    @SerializedName("id_reading_ex1")
    private int idReadingEx1;

    @SerializedName("id_pengukuran")
    private int idPengukuran;

    @SerializedName("reading_10")
    private double reading10;

    @SerializedName("reading_20")
    private double reading20;

    @SerializedName("reading_30")
    private double reading30;

    public ReadingsEx1Model() {}

    public ReadingsEx1Model(int idReadingEx1, int idPengukuran, double reading10,
                            double reading20, double reading30) {
        this.idReadingEx1 = idReadingEx1;
        this.idPengukuran = idPengukuran;
        this.reading10 = reading10;
        this.reading20 = reading20;
        this.reading30 = reading30;
    }

    public int getIdReadingEx1() {
        return idReadingEx1;
    }

    public void setIdReadingEx1(int idReadingEx1) {
        this.idReadingEx1 = idReadingEx1;
    }

    public int getIdPengukuran() {
        return idPengukuran;
    }

    public void setIdPengukuran(int idPengukuran) {
        this.idPengukuran = idPengukuran;
    }

    public double getReading10() {
        return reading10;
    }

    public void setReading10(double reading10) {
        this.reading10 = reading10;
    }

    public double getReading20() {
        return reading20;
    }

    public void setReading20(double reading20) {
        this.reading20 = reading20;
    }

    public double getReading30() {
        return reading30;
    }

    public void setReading30(double reading30) {
        this.reading30 = reading30;
    }

    @Override
    public String toString() {
        return "ReadingsEx1Model{" +
                "idReadingEx1=" + idReadingEx1 +
                ", idPengukuran=" + idPengukuran +
                ", reading10=" + reading10 +
                ", reading20=" + reading20 +
                ", reading30=" + reading30 +
                '}';
    }
}