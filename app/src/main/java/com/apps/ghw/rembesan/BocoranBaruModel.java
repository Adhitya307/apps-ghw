package com.apps.ghw.rembesan;

public class BocoranBaruModel {
    private int id;
    private int pengukuran_id;

    // ELV 624 T1
    private Double elv_624_t1;
    private String elv_624_t1_kode;

    // ELV 615 T2
    private Double elv_615_t2;
    private String elv_615_t2_kode;

    // PIPA P1
    private Double pipa_p1;
    private String pipa_p1_kode;

    // ===== Getter & Setter =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPengukuran_id() { return pengukuran_id; }
    public void setPengukuran_id(int pengukuran_id) { this.pengukuran_id = pengukuran_id; }

    public Double getElv_624_t1() { return elv_624_t1; }
    public void setElv_624_t1(Double elv_624_t1) { this.elv_624_t1 = elv_624_t1; }

    public String getElv_624_t1_kode() { return elv_624_t1_kode; }
    public void setElv_624_t1_kode(String elv_624_t1_kode) { this.elv_624_t1_kode = elv_624_t1_kode; }

    public Double getElv_615_t2() { return elv_615_t2; }
    public void setElv_615_t2(Double elv_615_t2) { this.elv_615_t2 = elv_615_t2; }

    public String getElv_615_t2_kode() { return elv_615_t2_kode; }
    public void setElv_615_t2_kode(String elv_615_t2_kode) { this.elv_615_t2_kode = elv_615_t2_kode; }

    public Double getPipa_p1() { return pipa_p1; }
    public void setPipa_p1(Double pipa_p1) { this.pipa_p1 = pipa_p1; }

    public String getPipa_p1_kode() { return pipa_p1_kode; }
    public void setPipa_p1_kode(String pipa_p1_kode) { this.pipa_p1_kode = pipa_p1_kode; }
}
