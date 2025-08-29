package com.example.kerjapraktik;

public class ThomsonWeirModel {
    private int id;
    private int pengukuran_id;
    private Double a1_r;
    private Double a1_l;
    private Double b1;
    private Double b3;
    private Double b5;

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPengukuran_id() { return pengukuran_id; }
    public void setPengukuran_id(int pengukuran_id) { this.pengukuran_id = pengukuran_id; }

    public Double getA1_r() { return a1_r; }
    public void setA1_r(Double a1_r) { this.a1_r = a1_r; }

    public Double getA1_l() { return a1_l; }
    public void setA1_l(Double a1_l) { this.a1_l = a1_l; }

    public Double getB1() { return b1; }
    public void setB1(Double b1) { this.b1 = b1; }

    public Double getB3() { return b3; }
    public void setB3(Double b3) { this.b3 = b3; }

    public Double getB5() { return b5; }
    public void setB5(Double b5) { this.b5 = b5; }
}
