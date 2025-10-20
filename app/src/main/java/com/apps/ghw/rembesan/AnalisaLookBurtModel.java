package com.apps.ghw.rembesan;

import com.google.gson.annotations.SerializedName;

public class AnalisaLookBurtModel {

    // ID di database, AUTO_INCREMENT
    private int id;

    @SerializedName("pengukuran_id")
    private int pengukuranId; // int(11)

    @SerializedName("tma_waduk")
    private Double tmaWaduk; // dari join t_data_pengukuran

    @SerializedName("rembesan_bendungan")
    private Double rembesanBendungan; // decimal(10,2)

    @SerializedName("panjang_bendungan")
    private Double panjangBendungan; // decimal(10,2)

    @SerializedName("rembesan_per_m")
    private Double rembesanPerM; // decimal(20,8)

    @SerializedName("nilai_ambang_ok")
    private Double nilaiAmbangOk = 0.28; // decimal(4,2), default 0.28

    @SerializedName("nilai_ambang_notok")
    private Double nilaiAmbangNotok = 0.56; // decimal(4,2), default 0.56

    @SerializedName("keterangan")
    private String keterangan;

    // ===== Sinkronisasi lokal =====
    private int isSynced = 0; // 0 = belum sinkron, 1 = sudah sinkron

    // ===== Getters & Setters =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPengukuranId() { return pengukuranId; }
    public void setPengukuranId(int pengukuranId) { this.pengukuranId = pengukuranId; }

    public Double getTmaWaduk() { return tmaWaduk; }
    public void setTmaWaduk(Double tmaWaduk) { this.tmaWaduk = tmaWaduk; }

    public Double getRembesanBendungan() { return rembesanBendungan; }
    public void setRembesanBendungan(Double rembesanBendungan) { this.rembesanBendungan = rembesanBendungan; }

    public Double getPanjangBendungan() { return panjangBendungan; }
    public void setPanjangBendungan(Double panjangBendungan) { this.panjangBendungan = panjangBendungan; }

    public Double getRembesanPerM() { return rembesanPerM; }
    public void setRembesanPerM(Double rembesanPerM) { this.rembesanPerM = rembesanPerM; }

    public Double getNilaiAmbangOk() { return nilaiAmbangOk; }
    public void setNilaiAmbangOk(Double nilaiAmbangOk) { this.nilaiAmbangOk = nilaiAmbangOk; }

    public Double getNilaiAmbangNotok() { return nilaiAmbangNotok; }
    public void setNilaiAmbangNotok(Double nilaiAmbangNotok) { this.nilaiAmbangNotok = nilaiAmbangNotok; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public int getIsSynced() { return isSynced; }
    public void setIsSynced(int isSynced) { this.isSynced = isSynced; }

    @Override
    public String toString() {
        return "AnalisaLookBurtModel{" +
                "id=" + id +
                ", pengukuranId=" + pengukuranId +
                ", tmaWaduk=" + tmaWaduk +
                ", rembesanBendungan=" + rembesanBendungan +
                ", panjangBendungan=" + panjangBendungan +
                ", rembesanPerM=" + rembesanPerM +
                ", nilaiAmbangOk=" + nilaiAmbangOk +
                ", nilaiAmbangNotok=" + nilaiAmbangNotok +
                ", keterangan='" + keterangan + '\'' +
                ", isSynced=" + isSynced +
                '}';
    }
}
