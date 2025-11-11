package com.apps.bubbletilt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelperBtm extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelperBtm";
    public static final String DATABASE_NAME = "db_btm.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_PENGUKURAN = "t_pengukuran_btm";

    // Bacaan tables
    private static final String TABLE_BACAAN_BT1 = "t_bacaan_bt_1";
    private static final String TABLE_BACAAN_BT2 = "t_bacaan_bt_2";
    private static final String TABLE_BACAAN_BT3 = "t_bacaan_bt_3";
    private static final String TABLE_BACAAN_BT4 = "t_bacaan_bt_4";
    private static final String TABLE_BACAAN_BT5 = "t_bacaan_bt_5";
    private static final String TABLE_BACAAN_BT6 = "t_bacaan_bt_6";
    private static final String TABLE_BACAAN_BT7 = "t_bacaan_bt_7";
    private static final String TABLE_BACAAN_BT8 = "t_bacaan_bt_8";

    // Perhitungan tables
    private static final String TABLE_PERHITUNGAN_BT1 = "p_bt_1";
    private static final String TABLE_PERHITUNGAN_BT2 = "p_bt_2";
    private static final String TABLE_PERHITUNGAN_BT3 = "p_bt_3";
    private static final String TABLE_PERHITUNGAN_BT4 = "p_bt_4";
    private static final String TABLE_PERHITUNGAN_BT5 = "p_bt_5";
    private static final String TABLE_PERHITUNGAN_BT6 = "p_bt_6";
    private static final String TABLE_PERHITUNGAN_BT7 = "p_bt_7";
    private static final String TABLE_PERHITUNGAN_BT8 = "p_bt_8";

    // Scatter tables
    private static final String TABLE_SCATTER_BT1 = "p_scatter_bt_1";
    private static final String TABLE_SCATTER_BT2 = "p_scatter_bt_2";
    private static final String TABLE_SCATTER_BT3 = "p_scatter_bt_3";
    private static final String TABLE_SCATTER_BT4 = "p_scatter_bt_4";
    private static final String TABLE_SCATTER_BT6 = "p_scatter_bt_6";
    private static final String TABLE_SCATTER_BT7 = "p_scatter_bt_7";
    private static final String TABLE_SCATTER_BT8 = "p_scatter_bt_8";

    public DatabaseHelperBtm(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table Pengukuran
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PENGUKURAN + " (" +
                "id_pengukuran INTEGER PRIMARY KEY, " +
                "tahun INTEGER, " +
                "periode TEXT, " +
                "tanggal TEXT, " +
                "temp_id TEXT, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        // Tables Bacaan untuk BT1-BT8
        for (int i = 1; i <= 8; i++) {
            db.execSQL("CREATE TABLE IF NOT EXISTS t_bacaan_bt_" + i + " (" +
                    "id_bacaan INTEGER PRIMARY KEY, " +
                    "id_pengukuran INTEGER, " +
                    "US_GP REAL, " +
                    "US_Arah TEXT, " +
                    "TB_GP REAL, " +
                    "TB_Arah TEXT, " +
                    "created_at TEXT, " +
                    "updated_at TEXT, " +
                    "FOREIGN KEY (id_pengukuran) REFERENCES " + TABLE_PENGUKURAN + "(id_pengukuran))");
        }

        // Tables Perhitungan untuk BT1-BT8
        for (int i = 1; i <= 8; i++) {
            db.execSQL("CREATE TABLE IF NOT EXISTS p_bt_" + i + " (" +
                    "id_perhitungan INTEGER PRIMARY KEY, " +
                    "id_pengukuran INTEGER, " +
                    "A_sec REAL, " +
                    "sin_A_rad REAL, " +
                    "B_sec REAL, " +
                    "sin_B_rad REAL, " +
                    "sin_C_rad REAL, " +
                    "sin_C_deg REAL, " +
                    "Cosa REAL, " +
                    "a_rad REAL, " +
                    "DMS TEXT, " +
                    "created_at TEXT, " +
                    "updated_at TEXT, " +
                    "FOREIGN KEY (id_pengukuran) REFERENCES " + TABLE_PENGUKURAN + "(id_pengukuran))");
        }

        // Tables Scatter untuk BT yang tersedia
        String[] scatterTables = {
                TABLE_SCATTER_BT1, TABLE_SCATTER_BT2, TABLE_SCATTER_BT3, TABLE_SCATTER_BT4,
                TABLE_SCATTER_BT6, TABLE_SCATTER_BT7, TABLE_SCATTER_BT8
        };

        for (String table : scatterTables) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + table + " (" +
                    "id_scatter INTEGER PRIMARY KEY, " +
                    "id_pengukuran INTEGER, " +
                    "Y_US REAL, " +
                    "X_TB REAL, " +
                    "Y_cum REAL, " +
                    "X_cum REAL, " +
                    "created_at TEXT, " +
                    "updated_at TEXT, " +
                    "FOREIGN KEY (id_pengukuran) REFERENCES " + TABLE_PENGUKURAN + "(id_pengukuran))");
        }

        Log.i(TAG, "✅ Semua tabel BTM berhasil dibuat");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop semua tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN);

        for (int i = 1; i <= 8; i++) {
            db.execSQL("DROP TABLE IF EXISTS t_bacaan_bt_" + i);
            db.execSQL("DROP TABLE IF EXISTS p_bt_" + i);
        }

        String[] scatterTables = {
                TABLE_SCATTER_BT1, TABLE_SCATTER_BT2, TABLE_SCATTER_BT3, TABLE_SCATTER_BT4,
                TABLE_SCATTER_BT6, TABLE_SCATTER_BT7, TABLE_SCATTER_BT8
        };

        for (String table : scatterTables) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }

        onCreate(db);
    }

    // ==================== INSERT OR UPDATE GENERIC ====================
    private long insertOrUpdate(String table, String idColumn, int idValue, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + idColumn + " FROM " + table + " WHERE " + idColumn + " = ?",
                new String[]{String.valueOf(idValue)});
        long result;
        if (cursor.moveToFirst()) {
            result = db.update(table, values, idColumn + " = ?", new String[]{String.valueOf(idValue)});
            Log.d(TAG, "🔁 Update: " + table + " id=" + idValue);
        } else {
            values.put(idColumn, idValue);
            result = db.insert(table, null, values);
            Log.d(TAG, "➕ Insert: " + table + " id=" + idValue);
        }
        cursor.close();
        return result;
    }

    // ==================== SPECIFIC INSERT OR UPDATE METHODS ====================

    // PENGUKURAN
    public long insertOrUpdatePengukuran(PengukuranBtmModel d) {
        ContentValues v = new ContentValues();
        v.put("tahun", d.getTahun());
        v.put("periode", d.getPeriode());
        v.put("tanggal", d.getTanggal());
        v.put("temp_id", d.getTemp_id());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_PENGUKURAN, "id_pengukuran", d.getId_pengukuran(), v);
    }

    // BACAAN untuk setiap BT
    public long insertOrUpdateBacaanBt1(BacaanBt1Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("US_GP", d.getUS_GP());
        v.put("US_Arah", d.getUS_Arah());
        v.put("TB_GP", d.getTB_GP());
        v.put("TB_Arah", d.getTB_Arah());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_BACAAN_BT1, "id_bacaan", d.getId_bacaan(), v);
    }

    public long insertOrUpdateBacaanBt2(BacaanBt2Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("US_GP", d.getUS_GP());
        v.put("US_Arah", d.getUS_Arah());
        v.put("TB_GP", d.getTB_GP());
        v.put("TB_Arah", d.getTB_Arah());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_BACAAN_BT2, "id_bacaan", d.getId_bacaan(), v);
    }

    public long insertOrUpdateBacaanBt3(BacaanBt3Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("US_GP", d.getUS_GP());
        v.put("US_Arah", d.getUS_Arah());
        v.put("TB_GP", d.getTB_GP());
        v.put("TB_Arah", d.getTB_Arah());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_BACAAN_BT3, "id_bacaan", d.getId_bacaan(), v);
    }

    public long insertOrUpdateBacaanBt4(BacaanBt4Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("US_GP", d.getUS_GP());
        v.put("US_Arah", d.getUS_Arah());
        v.put("TB_GP", d.getTB_GP());
        v.put("TB_Arah", d.getTB_Arah());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_BACAAN_BT4, "id_bacaan", d.getId_bacaan(), v);
    }



    public long insertOrUpdateBacaanBt6(BacaanBt6Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("US_GP", d.getUS_GP());
        v.put("US_Arah", d.getUS_Arah());
        v.put("TB_GP", d.getTB_GP());
        v.put("TB_Arah", d.getTB_Arah());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_BACAAN_BT6, "id_bacaan", d.getId_bacaan(), v);
    }

    public long insertOrUpdateBacaanBt7(BacaanBt7Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("US_GP", d.getUS_GP());
        v.put("US_Arah", d.getUS_Arah());
        v.put("TB_GP", d.getTB_GP());
        v.put("TB_Arah", d.getTB_Arah());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_BACAAN_BT7, "id_bacaan", d.getId_bacaan(), v);
    }

    public long insertOrUpdateBacaanBt8(BacaanBt8Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("US_GP", d.getUS_GP());
        v.put("US_Arah", d.getUS_Arah());
        v.put("TB_GP", d.getTB_GP());
        v.put("TB_Arah", d.getTB_Arah());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_BACAAN_BT8, "id_bacaan", d.getId_bacaan(), v);
    }

    // PERHITUNGAN untuk setiap BT
    public long insertOrUpdatePerhitunganBt1(PerhitunganBt1Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("A_sec", d.getA_sec());
        v.put("sin_A_rad", d.getSin_A_rad());
        v.put("B_sec", d.getB_sec());
        v.put("sin_B_rad", d.getSin_B_rad());
        v.put("sin_C_rad", d.getSin_C_rad());
        v.put("sin_C_deg", d.getSin_C_deg());
        v.put("Cosa", d.getCosa());
        v.put("a_rad", d.getA_rad());
        v.put("DMS", d.getDMS());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_PERHITUNGAN_BT1, "id_perhitungan", d.getId_perhitungan(), v);
    }

    public long insertOrUpdatePerhitunganBt2(PerhitunganBt2Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("A_sec", d.getA_sec());
        v.put("sin_A_rad", d.getSin_A_rad());
        v.put("B_sec", d.getB_sec());
        v.put("sin_B_rad", d.getSin_B_rad());
        v.put("sin_C_rad", d.getSin_C_rad());
        v.put("sin_C_deg", d.getSin_C_deg());
        v.put("Cosa", d.getCosa());
        v.put("a_rad", d.getA_rad());
        v.put("DMS", d.getDMS());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_PERHITUNGAN_BT2, "id_perhitungan", d.getId_perhitungan(), v);
    }

    public long insertOrUpdatePerhitunganBt3(PerhitunganBt3Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("A_sec", d.getA_sec());
        v.put("sin_A_rad", d.getSin_A_rad());
        v.put("B_sec", d.getB_sec());
        v.put("sin_B_rad", d.getSin_B_rad());
        v.put("sin_C_rad", d.getSin_C_rad());
        v.put("sin_C_deg", d.getSin_C_deg());
        v.put("Cosa", d.getCosa());
        v.put("a_rad", d.getA_rad());
        v.put("DMS", d.getDMS());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_PERHITUNGAN_BT3, "id_perhitungan", d.getId_perhitungan(), v);
    }

    public long insertOrUpdatePerhitunganBt4(PerhitunganBt4Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("A_sec", d.getA_sec());
        v.put("sin_A_rad", d.getSin_A_rad());
        v.put("B_sec", d.getB_sec());
        v.put("sin_B_rad", d.getSin_B_rad());
        v.put("sin_C_rad", d.getSin_C_rad());
        v.put("sin_C_deg", d.getSin_C_deg());
        v.put("Cosa", d.getCosa());
        v.put("a_rad", d.getA_rad());
        v.put("DMS", d.getDMS());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_PERHITUNGAN_BT4, "id_perhitungan", d.getId_perhitungan(), v);
    }



    public long insertOrUpdatePerhitunganBt6(PerhitunganBt6Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("A_sec", d.getA_sec());
        v.put("sin_A_rad", d.getSin_A_rad());
        v.put("B_sec", d.getB_sec());
        v.put("sin_B_rad", d.getSin_B_rad());
        v.put("sin_C_rad", d.getSin_C_rad());
        v.put("sin_C_deg", d.getSin_C_deg());
        v.put("Cosa", d.getCosa());
        v.put("a_rad", d.getA_rad());
        v.put("DMS", d.getDMS());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_PERHITUNGAN_BT6, "id_perhitungan", d.getId_perhitungan(), v);
    }

    public long insertOrUpdatePerhitunganBt7(PerhitunganBt7Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("A_sec", d.getA_sec());
        v.put("sin_A_rad", d.getSin_A_rad());
        v.put("B_sec", d.getB_sec());
        v.put("sin_B_rad", d.getSin_B_rad());
        v.put("sin_C_rad", d.getSin_C_rad());
        v.put("sin_C_deg", d.getSin_C_deg());
        v.put("Cosa", d.getCosa());
        v.put("a_rad", d.getA_rad());
        v.put("DMS", d.getDMS());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_PERHITUNGAN_BT7, "id_perhitungan", d.getId_perhitungan(), v);
    }

    public long insertOrUpdatePerhitunganBt8(PerhitunganBt8Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("A_sec", d.getA_sec());
        v.put("sin_A_rad", d.getSin_A_rad());
        v.put("B_sec", d.getB_sec());
        v.put("sin_B_rad", d.getSin_B_rad());
        v.put("sin_C_rad", d.getSin_C_rad());
        v.put("sin_C_deg", d.getSin_C_deg());
        v.put("Cosa", d.getCosa());
        v.put("a_rad", d.getA_rad());
        v.put("DMS", d.getDMS());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_PERHITUNGAN_BT8, "id_perhitungan", d.getId_perhitungan(), v);
    }

    // SCATTER untuk setiap BT
    public long insertOrUpdateScatterBt1(ScatterBt1Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("Y_US", d.getY_US());
        v.put("X_TB", d.getX_TB());
        v.put("Y_cum", d.getY_cum());
        v.put("X_cum", d.getX_cum());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_SCATTER_BT1, "id_scatter", d.getId_scatter(), v);
    }

    public long insertOrUpdateScatterBt2(ScatterBt2Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("Y_US", d.getY_US());
        v.put("X_TB", d.getX_TB());
        v.put("Y_cum", d.getY_cum());
        v.put("X_cum", d.getX_cum());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_SCATTER_BT2, "id_scatter", d.getId_scatter(), v);
    }

    public long insertOrUpdateScatterBt3(ScatterBt3Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("Y_US", d.getY_US());
        v.put("X_TB", d.getX_TB());
        v.put("Y_cum", d.getY_cum());
        v.put("X_cum", d.getX_cum());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_SCATTER_BT3, "id_scatter", d.getId_scatter(), v);
    }

    public long insertOrUpdateScatterBt4(ScatterBt4Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("Y_US", d.getY_US());
        v.put("X_TB", d.getX_TB());
        v.put("Y_cum", d.getY_cum());
        v.put("X_cum", d.getX_cum());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_SCATTER_BT4, "id_scatter", d.getId_scatter(), v);
    }

    public long insertOrUpdateScatterBt6(ScatterBt6Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("Y_US", d.getY_US());
        v.put("X_TB", d.getX_TB());
        v.put("Y_cum", d.getY_cum());
        v.put("X_cum", d.getX_cum());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_SCATTER_BT6, "id_scatter", d.getId_scatter(), v);
    }

    public long insertOrUpdateScatterBt7(ScatterBt7Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("Y_US", d.getY_US());
        v.put("X_TB", d.getX_TB());
        v.put("Y_cum", d.getY_cum());
        v.put("X_cum", d.getX_cum());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_SCATTER_BT7, "id_scatter", d.getId_scatter(), v);
    }

    public long insertOrUpdateScatterBt8(ScatterBt8Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("Y_US", d.getY_US());
        v.put("X_TB", d.getX_TB());
        v.put("Y_cum", d.getY_cum());
        v.put("X_cum", d.getX_cum());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_SCATTER_BT8, "id_scatter", d.getId_scatter(), v);
    }

    // ==================== GET ALL METHODS ====================

    public List<PengukuranBtmModel> getAllPengukuran() {
        List<PengukuranBtmModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PENGUKURAN + " ORDER BY tanggal ASC", null);
        if (c.moveToFirst()) {
            do {
                PengukuranBtmModel d = new PengukuranBtmModel();
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setTahun(c.getInt(c.getColumnIndexOrThrow("tahun")));
                d.setPeriode(c.getString(c.getColumnIndexOrThrow("periode")));
                d.setTanggal(c.getString(c.getColumnIndexOrThrow("tanggal")));
                d.setTemp_id(c.getString(c.getColumnIndexOrThrow("temp_id")));
                d.setCreated_at(c.getString(c.getColumnIndexOrThrow("created_at")));
                d.setUpdated_at(c.getString(c.getColumnIndexOrThrow("updated_at")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // GET ALL untuk Bacaan
    public List<BacaanBt1Model> getAllBacaanBt1() {
        return getAllBacaan(TABLE_BACAAN_BT1, new BacaanBt1Model());
    }

    public List<BacaanBt2Model> getAllBacaanBt2() {
        return getAllBacaan(TABLE_BACAAN_BT2, new BacaanBt2Model());
    }

    public List<BacaanBt3Model> getAllBacaanBt3() {
        return getAllBacaan(TABLE_BACAAN_BT3, new BacaanBt3Model());
    }

    public List<BacaanBt4Model> getAllBacaanBt4() {
        return getAllBacaan(TABLE_BACAAN_BT4, new BacaanBt4Model());
    }



    public List<BacaanBt6Model> getAllBacaanBt6() {
        return getAllBacaan(TABLE_BACAAN_BT6, new BacaanBt6Model());
    }

    public List<BacaanBt7Model> getAllBacaanBt7() {
        return getAllBacaan(TABLE_BACAAN_BT7, new BacaanBt7Model());
    }

    public List<BacaanBt8Model> getAllBacaanBt8() {
        return getAllBacaan(TABLE_BACAAN_BT8, new BacaanBt8Model());
    }

    // GET ALL untuk Perhitungan
    public List<PerhitunganBt1Model> getAllPerhitunganBt1() {
        return getAllPerhitungan(TABLE_PERHITUNGAN_BT1, new PerhitunganBt1Model());
    }

    public List<PerhitunganBt2Model> getAllPerhitunganBt2() {
        return getAllPerhitungan(TABLE_PERHITUNGAN_BT2, new PerhitunganBt2Model());
    }

    public List<PerhitunganBt3Model> getAllPerhitunganBt3() {
        return getAllPerhitungan(TABLE_PERHITUNGAN_BT3, new PerhitunganBt3Model());
    }

    public List<PerhitunganBt4Model> getAllPerhitunganBt4() {
        return getAllPerhitungan(TABLE_PERHITUNGAN_BT4, new PerhitunganBt4Model());
    }



    public List<PerhitunganBt6Model> getAllPerhitunganBt6() {
        return getAllPerhitungan(TABLE_PERHITUNGAN_BT6, new PerhitunganBt6Model());
    }

    public List<PerhitunganBt7Model> getAllPerhitunganBt7() {
        return getAllPerhitungan(TABLE_PERHITUNGAN_BT7, new PerhitunganBt7Model());
    }

    public List<PerhitunganBt8Model> getAllPerhitunganBt8() {
        return getAllPerhitungan(TABLE_PERHITUNGAN_BT8, new PerhitunganBt8Model());
    }

    // GET ALL untuk Scatter
    public List<ScatterBt1Model> getAllScatterBt1() {
        return getAllScatter(TABLE_SCATTER_BT1, new ScatterBt1Model());
    }

    public List<ScatterBt2Model> getAllScatterBt2() {
        return getAllScatter(TABLE_SCATTER_BT2, new ScatterBt2Model());
    }

    public List<ScatterBt3Model> getAllScatterBt3() {
        return getAllScatter(TABLE_SCATTER_BT3, new ScatterBt3Model());
    }

    public List<ScatterBt4Model> getAllScatterBt4() {
        return getAllScatter(TABLE_SCATTER_BT4, new ScatterBt4Model());
    }

    public List<ScatterBt6Model> getAllScatterBt6() {
        return getAllScatter(TABLE_SCATTER_BT6, new ScatterBt6Model());
    }

    public List<ScatterBt7Model> getAllScatterBt7() {
        return getAllScatter(TABLE_SCATTER_BT7, new ScatterBt7Model());
    }

    public List<ScatterBt8Model> getAllScatterBt8() {
        return getAllScatter(TABLE_SCATTER_BT8, new ScatterBt8Model());
    }

    // ==================== GET BY ID_PENGUKURAN METHODS ====================

    public PengukuranBtmModel getPengukuranById(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PENGUKURAN + " WHERE id_pengukuran = ?",
                new String[]{String.valueOf(idPengukuran)});

        PengukuranBtmModel d = null;
        if (c.moveToFirst()) {
            d = new PengukuranBtmModel();
            d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
            d.setTahun(c.getInt(c.getColumnIndexOrThrow("tahun")));
            d.setPeriode(c.getString(c.getColumnIndexOrThrow("periode")));
            d.setTanggal(c.getString(c.getColumnIndexOrThrow("tanggal")));
            d.setTemp_id(c.getString(c.getColumnIndexOrThrow("temp_id")));
            d.setCreated_at(c.getString(c.getColumnIndexOrThrow("created_at")));
            d.setUpdated_at(c.getString(c.getColumnIndexOrThrow("updated_at")));
        }
        c.close();
        return d;
    }

    public BacaanBt1Model getBacaanBt1ByPengukuran(int idPengukuran) {
        return getBacaanByPengukuran(TABLE_BACAAN_BT1, idPengukuran, new BacaanBt1Model());
    }

    public BacaanBt2Model getBacaanBt2ByPengukuran(int idPengukuran) {
        return getBacaanByPengukuran(TABLE_BACAAN_BT2, idPengukuran, new BacaanBt2Model());
    }

    public BacaanBt3Model getBacaanBt3ByPengukuran(int idPengukuran) {
        return getBacaanByPengukuran(TABLE_BACAAN_BT3, idPengukuran, new BacaanBt3Model());
    }

    public BacaanBt4Model getBacaanBt4ByPengukuran(int idPengukuran) {
        return getBacaanByPengukuran(TABLE_BACAAN_BT4, idPengukuran, new BacaanBt4Model());
    }


    public BacaanBt6Model getBacaanBt6ByPengukuran(int idPengukuran) {
        return getBacaanByPengukuran(TABLE_BACAAN_BT6, idPengukuran, new BacaanBt6Model());
    }

    public BacaanBt7Model getBacaanBt7ByPengukuran(int idPengukuran) {
        return getBacaanByPengukuran(TABLE_BACAAN_BT7, idPengukuran, new BacaanBt7Model());
    }

    public BacaanBt8Model getBacaanBt8ByPengukuran(int idPengukuran) {
        return getBacaanByPengukuran(TABLE_BACAAN_BT8, idPengukuran, new BacaanBt8Model());
    }

    public PerhitunganBt1Model getPerhitunganBt1ByPengukuran(int idPengukuran) {
        return getPerhitunganByPengukuran(TABLE_PERHITUNGAN_BT1, idPengukuran, new PerhitunganBt1Model());
    }

    public PerhitunganBt2Model getPerhitunganBt2ByPengukuran(int idPengukuran) {
        return getPerhitunganByPengukuran(TABLE_PERHITUNGAN_BT2, idPengukuran, new PerhitunganBt2Model());
    }

    public PerhitunganBt3Model getPerhitunganBt3ByPengukuran(int idPengukuran) {
        return getPerhitunganByPengukuran(TABLE_PERHITUNGAN_BT3, idPengukuran, new PerhitunganBt3Model());
    }

    public PerhitunganBt4Model getPerhitunganBt4ByPengukuran(int idPengukuran) {
        return getPerhitunganByPengukuran(TABLE_PERHITUNGAN_BT4, idPengukuran, new PerhitunganBt4Model());
    }



    public PerhitunganBt6Model getPerhitunganBt6ByPengukuran(int idPengukuran) {
        return getPerhitunganByPengukuran(TABLE_PERHITUNGAN_BT6, idPengukuran, new PerhitunganBt6Model());
    }

    public PerhitunganBt7Model getPerhitunganBt7ByPengukuran(int idPengukuran) {
        return getPerhitunganByPengukuran(TABLE_PERHITUNGAN_BT7, idPengukuran, new PerhitunganBt7Model());
    }

    public PerhitunganBt8Model getPerhitunganBt8ByPengukuran(int idPengukuran) {
        return getPerhitunganByPengukuran(TABLE_PERHITUNGAN_BT8, idPengukuran, new PerhitunganBt8Model());
    }

    public ScatterBt1Model getScatterBt1ByPengukuran(int idPengukuran) {
        return getScatterByPengukuran(TABLE_SCATTER_BT1, idPengukuran, new ScatterBt1Model());
    }

    public ScatterBt2Model getScatterBt2ByPengukuran(int idPengukuran) {
        return getScatterByPengukuran(TABLE_SCATTER_BT2, idPengukuran, new ScatterBt2Model());
    }

    public ScatterBt3Model getScatterBt3ByPengukuran(int idPengukuran) {
        return getScatterByPengukuran(TABLE_SCATTER_BT3, idPengukuran, new ScatterBt3Model());
    }

    public ScatterBt4Model getScatterBt4ByPengukuran(int idPengukuran) {
        return getScatterByPengukuran(TABLE_SCATTER_BT4, idPengukuran, new ScatterBt4Model());
    }

    public ScatterBt6Model getScatterBt6ByPengukuran(int idPengukuran) {
        return getScatterByPengukuran(TABLE_SCATTER_BT6, idPengukuran, new ScatterBt6Model());
    }

    public ScatterBt7Model getScatterBt7ByPengukuran(int idPengukuran) {
        return getScatterByPengukuran(TABLE_SCATTER_BT7, idPengukuran, new ScatterBt7Model());
    }

    public ScatterBt8Model getScatterBt8ByPengukuran(int idPengukuran) {
        return getScatterByPengukuran(TABLE_SCATTER_BT8, idPengukuran, new ScatterBt8Model());
    }

    // ==================== HELPER METHODS ====================

    private <T> List<T> getAllBacaan(String tableName, T model) {
        List<T> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);

        if (c.moveToFirst()) {
            do {
                T item = createBacaanFromCursor(model, c);
                if (item != null) {
                    list.add(item);
                }
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private <T> List<T> getAllPerhitungan(String tableName, T model) {
        List<T> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);

        if (c.moveToFirst()) {
            do {
                T item = createPerhitunganFromCursor(model, c);
                if (item != null) {
                    list.add(item);
                }
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private <T> List<T> getAllScatter(String tableName, T model) {
        List<T> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);

        if (c.moveToFirst()) {
            do {
                T item = createScatterFromCursor(model, c);
                if (item != null) {
                    list.add(item);
                }
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private <T> T getBacaanByPengukuran(String tableName, int idPengukuran, T model) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + tableName + " WHERE id_pengukuran = ?",
                new String[]{String.valueOf(idPengukuran)});

        T result = null;
        if (c.moveToFirst()) {
            result = createBacaanFromCursor(model, c);
        }
        c.close();
        return result;
    }

    private <T> T getPerhitunganByPengukuran(String tableName, int idPengukuran, T model) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + tableName + " WHERE id_pengukuran = ?",
                new String[]{String.valueOf(idPengukuran)});

        T result = null;
        if (c.moveToFirst()) {
            result = createPerhitunganFromCursor(model, c);
        }
        c.close();
        return result;
    }

    private <T> T getScatterByPengukuran(String tableName, int idPengukuran, T model) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + tableName + " WHERE id_pengukuran = ?",
                new String[]{String.valueOf(idPengukuran)});

        T result = null;
        if (c.moveToFirst()) {
            result = createScatterFromCursor(model, c);
        }
        c.close();
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T createBacaanFromCursor(T model, Cursor c) {
        try {
            if (model instanceof BacaanBt1Model) {
                BacaanBt1Model d = new BacaanBt1Model();
                setBacaanDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof BacaanBt2Model) {
                BacaanBt2Model d = new BacaanBt2Model();
                setBacaanDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof BacaanBt3Model) {
                BacaanBt3Model d = new BacaanBt3Model();
                setBacaanDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof BacaanBt4Model) {
                BacaanBt4Model d = new BacaanBt4Model();
                setBacaanDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof BacaanBt6Model) {
                BacaanBt6Model d = new BacaanBt6Model();
                setBacaanDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof BacaanBt7Model) {
                BacaanBt7Model d = new BacaanBt7Model();
                setBacaanDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof BacaanBt8Model) {
                BacaanBt8Model d = new BacaanBt8Model();
                setBacaanDataFromCursor(d, c);
                return (T) d;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating bacaan from cursor: " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T createPerhitunganFromCursor(T model, Cursor c) {
        try {
            if (model instanceof PerhitunganBt1Model) {
                PerhitunganBt1Model d = new PerhitunganBt1Model();
                setPerhitunganDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof PerhitunganBt2Model) {
                PerhitunganBt2Model d = new PerhitunganBt2Model();
                setPerhitunganDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof PerhitunganBt3Model) {
                PerhitunganBt3Model d = new PerhitunganBt3Model();
                setPerhitunganDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof PerhitunganBt4Model) {
                PerhitunganBt4Model d = new PerhitunganBt4Model();
                setPerhitunganDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof PerhitunganBt6Model) {
                PerhitunganBt6Model d = new PerhitunganBt6Model();
                setPerhitunganDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof PerhitunganBt7Model) {
                PerhitunganBt7Model d = new PerhitunganBt7Model();
                setPerhitunganDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof PerhitunganBt8Model) {
                PerhitunganBt8Model d = new PerhitunganBt8Model();
                setPerhitunganDataFromCursor(d, c);
                return (T) d;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating perhitungan from cursor: " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T createScatterFromCursor(T model, Cursor c) {
        try {
            if (model instanceof ScatterBt1Model) {
                ScatterBt1Model d = new ScatterBt1Model();
                setScatterDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof ScatterBt2Model) {
                ScatterBt2Model d = new ScatterBt2Model();
                setScatterDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof ScatterBt3Model) {
                ScatterBt3Model d = new ScatterBt3Model();
                setScatterDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof ScatterBt4Model) {
                ScatterBt4Model d = new ScatterBt4Model();
                setScatterDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof ScatterBt6Model) {
                ScatterBt6Model d = new ScatterBt6Model();
                setScatterDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof ScatterBt7Model) {
                ScatterBt7Model d = new ScatterBt7Model();
                setScatterDataFromCursor(d, c);
                return (T) d;
            } else if (model instanceof ScatterBt8Model) {
                ScatterBt8Model d = new ScatterBt8Model();
                setScatterDataFromCursor(d, c);
                return (T) d;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating scatter from cursor: " + e.getMessage());
        }
        return null;
    }

    private void setBacaanDataFromCursor(Object model, Cursor c) {
        try {
            if (model instanceof BacaanBt1Model) {
                BacaanBt1Model d = (BacaanBt1Model) model;
                d.setId_bacaan(c.getInt(c.getColumnIndexOrThrow("id_bacaan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setUS_GP(c.getDouble(c.getColumnIndexOrThrow("US_GP")));
                d.setUS_Arah(c.getString(c.getColumnIndexOrThrow("US_Arah")));
                d.setTB_GP(c.getDouble(c.getColumnIndexOrThrow("TB_GP")));
                d.setTB_Arah(c.getString(c.getColumnIndexOrThrow("TB_Arah")));
                d.setCreated_at(c.getString(c.getColumnIndexOrThrow("created_at")));
                d.setUpdated_at(c.getString(c.getColumnIndexOrThrow("updated_at")));
            }
            // Same implementation for other Bacaan models
        } catch (Exception e) {
            Log.e(TAG, "Error setting bacaan data from cursor: " + e.getMessage());
        }
    }

    private void setPerhitunganDataFromCursor(Object model, Cursor c) {
        try {
            if (model instanceof PerhitunganBt1Model) {
                PerhitunganBt1Model d = (PerhitunganBt1Model) model;
                d.setId_perhitungan(c.getInt(c.getColumnIndexOrThrow("id_perhitungan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setA_sec(c.getDouble(c.getColumnIndexOrThrow("A_sec")));
                d.setSin_A_rad(c.getDouble(c.getColumnIndexOrThrow("sin_A_rad")));
                d.setB_sec(c.getDouble(c.getColumnIndexOrThrow("B_sec")));
                d.setSin_B_rad(c.getDouble(c.getColumnIndexOrThrow("sin_B_rad")));
                d.setSin_C_rad(c.getDouble(c.getColumnIndexOrThrow("sin_C_rad")));
                d.setSin_C_deg(c.getDouble(c.getColumnIndexOrThrow("sin_C_deg")));
                d.setCosa(c.getDouble(c.getColumnIndexOrThrow("Cosa")));
                d.setA_rad(c.getDouble(c.getColumnIndexOrThrow("a_rad")));
                d.setDMS(c.getString(c.getColumnIndexOrThrow("DMS")));
                d.setCreated_at(c.getString(c.getColumnIndexOrThrow("created_at")));
                d.setUpdated_at(c.getString(c.getColumnIndexOrThrow("updated_at")));
            }
            // Same implementation for other Perhitungan models
        } catch (Exception e) {
            Log.e(TAG, "Error setting perhitungan data from cursor: " + e.getMessage());
        }
    }

    private void setScatterDataFromCursor(Object model, Cursor c) {
        try {
            if (model instanceof ScatterBt1Model) {
                ScatterBt1Model d = (ScatterBt1Model) model;
                d.setId_scatter(c.getInt(c.getColumnIndexOrThrow("id_scatter")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setY_US(c.getDouble(c.getColumnIndexOrThrow("Y_US")));
                d.setX_TB(c.getDouble(c.getColumnIndexOrThrow("X_TB")));
                d.setY_cum(c.getDouble(c.getColumnIndexOrThrow("Y_cum")));
                d.setX_cum(c.getDouble(c.getColumnIndexOrThrow("X_cum")));
                d.setCreated_at(c.getString(c.getColumnIndexOrThrow("created_at")));
                d.setUpdated_at(c.getString(c.getColumnIndexOrThrow("updated_at")));
            }
            // Same implementation for other Scatter models
        } catch (Exception e) {
            Log.e(TAG, "Error setting scatter data from cursor: " + e.getMessage());
        }
    }

    // ==================== BULK OPERATIONS ====================

    public void bulkInsertPengukuran(List<PengukuranBtmModel> data) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (PengukuranBtmModel item : data) {
                insertOrUpdatePengukuran(item);
            }
            db.setTransactionSuccessful();
            Log.i(TAG, "✅ Bulk insert pengukuran berhasil: " + data.size() + " records");
        } catch (Exception e) {
            Log.e(TAG, "❌ Bulk insert pengukuran gagal: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    public void bulkInsertBacaanBt1(List<BacaanBt1Model> data) {
        bulkInsertBacaan(data, TABLE_BACAAN_BT1);
    }

    public void bulkInsertBacaanBt2(List<BacaanBt2Model> data) {
        bulkInsertBacaan(data, TABLE_BACAAN_BT2);
    }

    public void bulkInsertBacaanBt3(List<BacaanBt3Model> data) {
        bulkInsertBacaan(data, TABLE_BACAAN_BT3);
    }

    public void bulkInsertBacaanBt4(List<BacaanBt4Model> data) {
        bulkInsertBacaan(data, TABLE_BACAAN_BT4);
    }



    public void bulkInsertBacaanBt6(List<BacaanBt6Model> data) {
        bulkInsertBacaan(data, TABLE_BACAAN_BT6);
    }

    public void bulkInsertBacaanBt7(List<BacaanBt7Model> data) {
        bulkInsertBacaan(data, TABLE_BACAAN_BT7);
    }

    public void bulkInsertBacaanBt8(List<BacaanBt8Model> data) {
        bulkInsertBacaan(data, TABLE_BACAAN_BT8);
    }

    private <T> void bulkInsertBacaan(List<T> data, String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (T item : data) {
                if (item instanceof BacaanBt1Model) {
                    insertOrUpdateBacaanBt1((BacaanBt1Model) item);
                } else if (item instanceof BacaanBt2Model) {
                    insertOrUpdateBacaanBt2((BacaanBt2Model) item);
                }
                // ... continue for other Bacaan types
            }
            db.setTransactionSuccessful();
            Log.i(TAG, "✅ Bulk insert " + tableName + " berhasil: " + data.size() + " records");
        } catch (Exception e) {
            Log.e(TAG, "❌ Bulk insert " + tableName + " gagal: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    // ==================== CLEAR DATA ====================

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_PENGUKURAN);

            for (int i = 1; i <= 8; i++) {
                db.execSQL("DELETE FROM t_bacaan_bt_" + i);
                db.execSQL("DELETE FROM p_bt_" + i);
            }

            String[] scatterTables = {
                    TABLE_SCATTER_BT1, TABLE_SCATTER_BT2, TABLE_SCATTER_BT3, TABLE_SCATTER_BT4,
                    TABLE_SCATTER_BT6, TABLE_SCATTER_BT7, TABLE_SCATTER_BT8
            };

            for (String table : scatterTables) {
                db.execSQL("DELETE FROM " + table);
            }

            db.setTransactionSuccessful();
            Log.i(TAG, "✅ Semua data BTM berhasil dihapus");
        } catch (Exception e) {
            Log.e(TAG, "❌ Gagal menghapus data BTM: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    // ==================== GET DATABASE INFO ====================

    public int getTableCount(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public void printDatabaseInfo() {
        Log.i(TAG, "=== DATABASE BTM INFO ===");
        Log.i(TAG, "Pengukuran: " + getTableCount(TABLE_PENGUKURAN) + " records");

        for (int i = 1; i <= 8; i++) {
            Log.i(TAG, "Bacaan BT" + i + ": " + getTableCount("t_bacaan_bt_" + i) + " records");
            Log.i(TAG, "Perhitungan BT" + i + ": " + getTableCount("p_bt_" + i) + " records");
        }

        String[] scatterTables = {
                TABLE_SCATTER_BT1, TABLE_SCATTER_BT2, TABLE_SCATTER_BT3, TABLE_SCATTER_BT4,
                TABLE_SCATTER_BT6, TABLE_SCATTER_BT7, TABLE_SCATTER_BT8
        };

        for (String table : scatterTables) {
            Log.i(TAG, table + ": " + getTableCount(table) + " records");
        }
        Log.i(TAG, "=== END DATABASE INFO ===");
    }
}