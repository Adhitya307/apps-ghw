package com.example.app_rightpiezo;

import com.google.gson.annotations.SerializedName;

public class B_piezo_metrik {
    private int id_pengukuran;
    private double feet;
    private double inch;

    @SerializedName("R-01") private Double R_01;
    @SerializedName("R-02") private Double R_02;
    @SerializedName("R-03") private Double R_03;
    @SerializedName("R-04") private Double R_04;
    @SerializedName("R-05") private Double R_05;
    @SerializedName("R-06") private Double R_06;
    @SerializedName("R-07") private Double R_07;
    @SerializedName("R-08") private Double R_08;
    @SerializedName("R-09") private Double R_09;
    @SerializedName("R-10") private Double R_10;
    @SerializedName("R-11") private Double R_11;
    @SerializedName("R-12") private Double R_12;
    @SerializedName("IPZ-01") private Double IPZ_01;
    @SerializedName("PZ-04") private Double PZ_04;

    // Getters and Setters (tetap sama)
    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public double getFeet() { return feet; }
    public void setFeet(double feet) { this.feet = feet; }

    public double getInch() { return inch; }
    public void setInch(double inch) { this.inch = inch; }

    public Double getR01() { return R_01; } public void setR01(Double R_01) { this.R_01 = R_01; }
    public Double getR02() { return R_02; } public void setR02(Double R_02) { this.R_02 = R_02; }
    public Double getR03() { return R_03; } public void setR03(Double R_03) { this.R_03 = R_03; }
    public Double getR04() { return R_04; } public void setR04(Double R_04) { this.R_04 = R_04; }
    public Double getR05() { return R_05; } public void setR05(Double R_05) { this.R_05 = R_05; }
    public Double getR06() { return R_06; } public void setR06(Double R_06) { this.R_06 = R_06; }
    public Double getR07() { return R_07; } public void setR07(Double R_07) { this.R_07 = R_07; }
    public Double getR08() { return R_08; } public void setR08(Double R_08) { this.R_08 = R_08; }
    public Double getR09() { return R_09; } public void setR09(Double R_09) { this.R_09 = R_09; }
    public Double getR10() { return R_10; } public void setR10(Double R_10) { this.R_10 = R_10; }
    public Double getR11() { return R_11; } public void setR11(Double R_11) { this.R_11 = R_11; }
    public Double getR12() { return R_12; } public void setR12(Double R_12) { this.R_12 = R_12; }
    public Double getIPZ01() { return IPZ_01; } public void setIPZ01(Double IPZ_01) { this.IPZ_01 = IPZ_01; }
    public Double getPZ04() { return PZ_04; } public void setPZ04(Double PZ_04) { this.PZ_04 = PZ_04; }
}