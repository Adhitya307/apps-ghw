package com.apps.bubbletilt;

public class BacaanBt7Model {
    private int id_bacaan;
    private int id_pengukuran;
    private double US_GP;
    private String US_Arah;
    private double TB_GP;
    private String TB_Arah;
    private String created_at;
    private String updated_at;

    public BacaanBt7Model() {}

    // Getters and Setters
    public int getId_bacaan() { return id_bacaan; }
    public void setId_bacaan(int id_bacaan) { this.id_bacaan = id_bacaan; }

    public int getId_pengukuran() { return id_pengukuran; }
    public void setId_pengukuran(int id_pengukuran) { this.id_pengukuran = id_pengukuran; }

    public double getUS_GP() { return US_GP; }
    public void setUS_GP(double US_GP) { this.US_GP = US_GP; }

    public String getUS_Arah() { return US_Arah; }
    public void setUS_Arah(String US_Arah) { this.US_Arah = US_Arah; }

    public double getTB_GP() { return TB_GP; }
    public void setTB_GP(double TB_GP) { this.TB_GP = TB_GP; }

    public String getTB_Arah() { return TB_Arah; }
    public void setTB_Arah(String TB_Arah) { this.TB_Arah = TB_Arah; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}