package com.example.app_dambody;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    public static final String DATABASE_NAME = "db_hdm.db";
    private static final int DATABASE_VERSION = 5; // Updated version

    // Table names - EXISTING
    private static final String TABLE_PENGUKURAN = "t_pengukuran_hdm";
    private static final String TABLE_PEMBACAAN_625 = "t_pembacaan_hdm_elv625";
    private static final String TABLE_PEMBACAAN_600 = "t_pembacaan_hdm_elv600";
    private static final String TABLE_DEPTH_625 = "t_depth_elv625";
    private static final String TABLE_DEPTH_600 = "t_depth_elv600";
    private static final String TABLE_INITIAL_625 = "m_initial_reading_elv_625";
    private static final String TABLE_INITIAL_600 = "m_initial_reading_elv_600";
    private static final String TABLE_PERGERAKAN_625 = "t_pergerakan_elv625";
    private static final String TABLE_PERGERAKAN_600 = "t_pergerakan_elv600";

    // NEW: Ambang Batas Table names
    private static final String TABLE_AMBANG_BATAS_625_H1 = "ambang_batas_625_h1";
    private static final String TABLE_AMBANG_BATAS_625_H2 = "ambang_batas_625_h2";
    private static final String TABLE_AMBANG_BATAS_625_H3 = "ambang_batas_625_h3";
    private static final String TABLE_AMBANG_BATAS_600_H1 = "ambang_batas_600_h1";
    private static final String TABLE_AMBANG_BATAS_600_H2 = "ambang_batas_600_h2";
    private static final String TABLE_AMBANG_BATAS_600_H3 = "ambang_batas_600_h3";
    private static final String TABLE_AMBANG_BATAS_600_H4 = "ambang_batas_600_h4";
    private static final String TABLE_AMBANG_BATAS_600_H5 = "ambang_batas_600_h5";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging(); // ✅ penting agar DB bisa dibaca tanpa terkunci
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        // EXISTING TABLES - TIDAK DIUBAH
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PENGUKURAN + " (" +
                "id_pengukuran INTEGER PRIMARY KEY, " +
                "tahun INTEGER, " +
                "periode TEXT, " +
                "tanggal TEXT, " +
                "dma TEXT, " +
                "temp_id TEXT, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PEMBACAAN_625 + " (" +
                "id_pembacaan INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "hv_1 REAL, hv_2 REAL, hv_3 REAL, " +
                "created_at TEXT, updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PEMBACAAN_600 + " (" +
                "id_pembacaan INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "hv_1 REAL, hv_2 REAL, hv_3 REAL, hv_4 REAL, hv_5 REAL, " +
                "created_at TEXT, updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DEPTH_625 + " (" +
                "id_depth INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, hv_1 REAL, hv_2 REAL, hv_3 REAL, " +
                "created_at TEXT, updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DEPTH_600 + " (" +
                "id_depth INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, hv_1 REAL, hv_2 REAL, hv_3 REAL, hv_4 REAL, hv_5 REAL, " +
                "created_at TEXT, updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_INITIAL_625 + " (" +
                "id_initial_reading INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, hv_1 REAL, hv_2 REAL, hv_3 REAL, " +
                "created_at TEXT, updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_INITIAL_600 + " (" +
                "id_initial_reading INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, hv_1 REAL, hv_2 REAL, hv_3 REAL, hv_4 REAL, hv_5 REAL, " +
                "created_at TEXT, updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PERGERAKAN_625 + " (" +
                "id_pergerakan INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, hv_1 REAL, hv_2 REAL, hv_3 REAL, " +
                "created_at TEXT, updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PERGERAKAN_600 + " (" +
                "id_pergerakan INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, hv_1 REAL, hv_2 REAL, hv_3 REAL, hv_4 REAL, hv_5 REAL, " +
                "created_at TEXT, updated_at TEXT)");

        // NEW: AMBANG BATAS TABLES
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMBANG_BATAS_625_H1 + " (" +
                "id_ambang_batas INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "aman REAL, " +
                "peringatan REAL, " +
                "bahaya REAL, " +
                "pergerakan REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMBANG_BATAS_625_H2 + " (" +
                "id_ambang_batas INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "aman REAL, " +
                "peringatan REAL, " +
                "bahaya REAL, " +
                "pergerakan REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMBANG_BATAS_625_H3 + " (" +
                "id_ambang_batas INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "aman REAL, " +
                "peringatan REAL, " +
                "bahaya REAL, " +
                "pergerakan REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMBANG_BATAS_600_H1 + " (" +
                "id_ambang_batas INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "aman REAL, " +
                "peringatan REAL, " +
                "bahaya REAL, " +
                "pergerakan REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMBANG_BATAS_600_H2 + " (" +
                "id_ambang_batas INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "aman REAL, " +
                "peringatan REAL, " +
                "bahaya REAL, " +
                "pergerakan REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMBANG_BATAS_600_H3 + " (" +
                "id_ambang_batas INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "aman REAL, " +
                "peringatan REAL, " +
                "bahaya REAL, " +
                "pergerakan REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMBANG_BATAS_600_H4 + " (" +
                "id_ambang_batas INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "aman REAL, " +
                "peringatan REAL, " +
                "bahaya REAL, " +
                "pergerakan REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMBANG_BATAS_600_H5 + " (" +
                "id_ambang_batas INTEGER PRIMARY KEY, " +
                "id_pengukuran INTEGER, " +
                "aman REAL, " +
                "peringatan REAL, " +
                "bahaya REAL, " +
                "pergerakan REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT)");

        Log.i(TAG, "✅ Semua tabel berhasil dibuat");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // EXISTING DROP TABLES - TIDAK DIUBAH
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PEMBACAAN_625);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PEMBACAAN_600);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEPTH_625);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEPTH_600);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INITIAL_625);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INITIAL_600);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERGERAKAN_625);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERGERAKAN_600);

        // NEW: DROP AMBANG BATAS TABLES
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBANG_BATAS_625_H1);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBANG_BATAS_625_H2);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBANG_BATAS_625_H3);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBANG_BATAS_600_H1);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBANG_BATAS_600_H2);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBANG_BATAS_600_H3);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBANG_BATAS_600_H4);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBANG_BATAS_600_H5);

        onCreate(db);
    }

    // ==================== Insert Or Update Generic ====================
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

    // ==================== Specific Insert Or Update Methods ====================
    // EXISTING METHODS - TIDAK DIUBAH
    public long insertOrUpdatePengukuran(PengukuranModel d) {
        ContentValues v = new ContentValues();
        v.put("tahun", d.getTahun());
        v.put("periode", d.getPeriode());
        v.put("tanggal", d.getTanggal());
        v.put("dma", d.getDma());
        v.put("temp_id", d.getTemp_id());
        return insertOrUpdate(TABLE_PENGUKURAN, "id_pengukuran", d.getId_pengukuran(), v);
    }

    public long insertOrUpdateDepth625(Depth625Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("hv_1", d.getHv_1());
        v.put("hv_2", d.getHv_2());
        v.put("hv_3", d.getHv_3());
        return insertOrUpdate(TABLE_DEPTH_625, "id_depth", d.getId_depth(), v);
    }

    public long insertOrUpdateDepth600(Depth600Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("hv_1", d.getHv_1());
        v.put("hv_2", d.getHv_2());
        v.put("hv_3", d.getHv_3());
        v.put("hv_4", d.getHv_4());
        v.put("hv_5", d.getHv_5());
        return insertOrUpdate(TABLE_DEPTH_600, "id_depth", d.getId_depth(), v);
    }

    public long insertOrUpdateInitial625(Initial625Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("hv_1", d.getHv_1());
        v.put("hv_2", d.getHv_2());
        v.put("hv_3", d.getHv_3());
        return insertOrUpdate(TABLE_INITIAL_625, "id_initial_reading", d.getId_initial_reading(), v);
    }

    public long insertOrUpdateInitial600(Initial600Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("hv_1", d.getHv_1());
        v.put("hv_2", d.getHv_2());
        v.put("hv_3", d.getHv_3());
        v.put("hv_4", d.getHv_4());
        v.put("hv_5", d.getHv_5());
        return insertOrUpdate(TABLE_INITIAL_600, "id_initial_reading", d.getId_initial_reading(), v);
    }

    public long insertOrUpdatePergerakan625(Pergerakan625Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("hv_1", d.getHv_1());
        v.put("hv_2", d.getHv_2());
        v.put("hv_3", d.getHv_3());
        return insertOrUpdate(TABLE_PERGERAKAN_625, "id_pergerakan", d.getId_pergerakan(), v);
    }

    public long insertOrUpdatePergerakan600(Pergerakan600Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("hv_1", d.getHv_1());
        v.put("hv_2", d.getHv_2());
        v.put("hv_3", d.getHv_3());
        v.put("hv_4", d.getHv_4());
        v.put("hv_5", d.getHv_5());
        return insertOrUpdate(TABLE_PERGERAKAN_600, "id_pergerakan", d.getId_pergerakan(), v);
    }

    // NEW: AMBANG BATAS INSERT/UPDATE METHODS
    public long insertOrUpdateAmbangBatas625H1(AmbangBatas625H1Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("aman", d.getAman());
        v.put("peringatan", d.getPeringatan());
        v.put("bahaya", d.getBahaya());
        v.put("pergerakan", d.getPergerakan());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_AMBANG_BATAS_625_H1, "id_ambang_batas", d.getId_ambang_batas(), v);
    }

    public long insertOrUpdateAmbangBatas625H2(AmbangBatas625H2Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("aman", d.getAman());
        v.put("peringatan", d.getPeringatan());
        v.put("bahaya", d.getBahaya());
        v.put("pergerakan", d.getPergerakan());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_AMBANG_BATAS_625_H2, "id_ambang_batas", d.getId_ambang_batas(), v);
    }

    public long insertOrUpdateAmbangBatas625H3(AmbangBatas625H3Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("aman", d.getAman());
        v.put("peringatan", d.getPeringatan());
        v.put("bahaya", d.getBahaya());
        v.put("pergerakan", d.getPergerakan());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_AMBANG_BATAS_625_H3, "id_ambang_batas", d.getId_ambang_batas(), v);
    }

    public long insertOrUpdateAmbangBatas600H1(AmbangBatas600H1Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("aman", d.getAman());
        v.put("peringatan", d.getPeringatan());
        v.put("bahaya", d.getBahaya());
        v.put("pergerakan", d.getPergerakan());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_AMBANG_BATAS_600_H1, "id_ambang_batas", d.getId_ambang_batas(), v);
    }

    public long insertOrUpdateAmbangBatas600H2(AmbangBatas600H2Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("aman", d.getAman());
        v.put("peringatan", d.getPeringatan());
        v.put("bahaya", d.getBahaya());
        v.put("pergerakan", d.getPergerakan());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_AMBANG_BATAS_600_H2, "id_ambang_batas", d.getId_ambang_batas(), v);
    }

    public long insertOrUpdateAmbangBatas600H3(AmbangBatas600H3Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("aman", d.getAman());
        v.put("peringatan", d.getPeringatan());
        v.put("bahaya", d.getBahaya());
        v.put("pergerakan", d.getPergerakan());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_AMBANG_BATAS_600_H3, "id_ambang_batas", d.getId_ambang_batas(), v);
    }

    public long insertOrUpdateAmbangBatas600H4(AmbangBatas600H4Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("aman", d.getAman());
        v.put("peringatan", d.getPeringatan());
        v.put("bahaya", d.getBahaya());
        v.put("pergerakan", d.getPergerakan());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_AMBANG_BATAS_600_H4, "id_ambang_batas", d.getId_ambang_batas(), v);
    }

    public long insertOrUpdateAmbangBatas600H5(AmbangBatas600H5Model d) {
        ContentValues v = new ContentValues();
        v.put("id_pengukuran", d.getId_pengukuran());
        v.put("aman", d.getAman());
        v.put("peringatan", d.getPeringatan());
        v.put("bahaya", d.getBahaya());
        v.put("pergerakan", d.getPergerakan());
        v.put("created_at", d.getCreated_at());
        v.put("updated_at", d.getUpdated_at());
        return insertOrUpdate(TABLE_AMBANG_BATAS_600_H5, "id_ambang_batas", d.getId_ambang_batas(), v);
    }


// ==================== GET ALL METHODS ====================

    // EXISTING GET ALL METHODS - TIDAK DIUBAH
    public List<PengukuranModel> getAllPengukuran() {
        List<PengukuranModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PENGUKURAN + " ORDER BY tanggal DESC", null);
        if (c.moveToFirst()) {
            do {
                PengukuranModel d = new PengukuranModel();
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setTahun(c.getInt(c.getColumnIndexOrThrow("tahun")));
                d.setPeriode(c.getString(c.getColumnIndexOrThrow("periode")));
                d.setTanggal(c.getString(c.getColumnIndexOrThrow("tanggal")));
                d.setDma(c.getString(c.getColumnIndexOrThrow("dma")));
                d.setTemp_id(c.getString(c.getColumnIndexOrThrow("temp_id")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Pembacaan625Model> getAllPembacaan625() {
        List<Pembacaan625Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PEMBACAAN_625, null);
        if (c.moveToFirst()) {
            do {
                Pembacaan625Model d = new Pembacaan625Model();
                d.setId_pembacaan(c.getInt(c.getColumnIndexOrThrow("id_pembacaan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Pembacaan600Model> getAllPembacaan600() {
        List<Pembacaan600Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PEMBACAAN_600, null);
        if (c.moveToFirst()) {
            do {
                Pembacaan600Model d = new Pembacaan600Model();
                d.setId_pembacaan(c.getInt(c.getColumnIndexOrThrow("id_pembacaan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                d.setHv_4(c.getDouble(c.getColumnIndexOrThrow("hv_4")));
                d.setHv_5(c.getDouble(c.getColumnIndexOrThrow("hv_5")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Pergerakan625Model> getAllPergerakan625() {
        List<Pergerakan625Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PERGERAKAN_625, null);
        if (c.moveToFirst()) {
            do {
                Pergerakan625Model d = new Pergerakan625Model();
                d.setId_pergerakan(c.getInt(c.getColumnIndexOrThrow("id_pergerakan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Pergerakan600Model> getAllPergerakan600() {
        List<Pergerakan600Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PERGERAKAN_600, null);
        if (c.moveToFirst()) {
            do {
                Pergerakan600Model d = new Pergerakan600Model();
                d.setId_pergerakan(c.getInt(c.getColumnIndexOrThrow("id_pergerakan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                d.setHv_4(c.getDouble(c.getColumnIndexOrThrow("hv_4")));
                d.setHv_5(c.getDouble(c.getColumnIndexOrThrow("hv_5")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // Tambahkan method getAll untuk setiap tabel
    public List<Depth625Model> getAllDepth625() {
        List<Depth625Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_DEPTH_625, null);
        if (c.moveToFirst()) {
            do {
                Depth625Model d = new Depth625Model();
                d.setId_depth(c.getInt(c.getColumnIndexOrThrow("id_depth")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Depth600Model> getAllDepth600() {
        List<Depth600Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_DEPTH_600, null);
        if (c.moveToFirst()) {
            do {
                Depth600Model d = new Depth600Model();
                d.setId_depth(c.getInt(c.getColumnIndexOrThrow("id_depth")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                d.setHv_4(c.getDouble(c.getColumnIndexOrThrow("hv_4")));
                d.setHv_5(c.getDouble(c.getColumnIndexOrThrow("hv_5")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Initial625Model> getAllInitial625() {
        List<Initial625Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_INITIAL_625, null);
        if (c.moveToFirst()) {
            do {
                Initial625Model d = new Initial625Model();
                d.setId_initial_reading(c.getInt(c.getColumnIndexOrThrow("id_initial_reading")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Initial600Model> getAllInitial600() {
        List<Initial600Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_INITIAL_600, null);
        if (c.moveToFirst()) {
            do {
                Initial600Model d = new Initial600Model();
                d.setId_initial_reading(c.getInt(c.getColumnIndexOrThrow("id_initial_reading")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                d.setHv_4(c.getDouble(c.getColumnIndexOrThrow("hv_4")));
                d.setHv_5(c.getDouble(c.getColumnIndexOrThrow("hv_5")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // NEW: GET ALL AMBANG BATAS METHODS
    public List<AmbangBatas625H1Model> getAllAmbangBatas625H1() {
        List<AmbangBatas625H1Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_AMBANG_BATAS_625_H1, null);
        if (c.moveToFirst()) {
            do {
                AmbangBatas625H1Model d = new AmbangBatas625H1Model();
                setAmbangBatasDataFromCursor(d, c);
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<AmbangBatas625H2Model> getAllAmbangBatas625H2() {
        List<AmbangBatas625H2Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_AMBANG_BATAS_625_H2, null);
        if (c.moveToFirst()) {
            do {
                AmbangBatas625H2Model d = new AmbangBatas625H2Model();
                setAmbangBatasDataFromCursor(d, c);
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<AmbangBatas625H3Model> getAllAmbangBatas625H3() {
        List<AmbangBatas625H3Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_AMBANG_BATAS_625_H3, null);
        if (c.moveToFirst()) {
            do {
                AmbangBatas625H3Model d = new AmbangBatas625H3Model();
                setAmbangBatasDataFromCursor(d, c);
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<AmbangBatas600H1Model> getAllAmbangBatas600H1() {
        List<AmbangBatas600H1Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_AMBANG_BATAS_600_H1, null);
        if (c.moveToFirst()) {
            do {
                AmbangBatas600H1Model d = new AmbangBatas600H1Model();
                setAmbangBatasDataFromCursor(d, c);
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<AmbangBatas600H2Model> getAllAmbangBatas600H2() {
        List<AmbangBatas600H2Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_AMBANG_BATAS_600_H2, null);
        if (c.moveToFirst()) {
            do {
                AmbangBatas600H2Model d = new AmbangBatas600H2Model();
                setAmbangBatasDataFromCursor(d, c);
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<AmbangBatas600H3Model> getAllAmbangBatas600H3() {
        List<AmbangBatas600H3Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_AMBANG_BATAS_600_H3, null);
        if (c.moveToFirst()) {
            do {
                AmbangBatas600H3Model d = new AmbangBatas600H3Model();
                setAmbangBatasDataFromCursor(d, c);
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<AmbangBatas600H4Model> getAllAmbangBatas600H4() {
        List<AmbangBatas600H4Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_AMBANG_BATAS_600_H4, null);
        if (c.moveToFirst()) {
            do {
                AmbangBatas600H4Model d = new AmbangBatas600H4Model();
                setAmbangBatasDataFromCursor(d, c);
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<AmbangBatas600H5Model> getAllAmbangBatas600H5() {
        List<AmbangBatas600H5Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_AMBANG_BATAS_600_H5, null);
        if (c.moveToFirst()) {
            do {
                AmbangBatas600H5Model d = new AmbangBatas600H5Model();
                setAmbangBatasDataFromCursor(d, c);
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // ===============================================================
// ✅ INSERT OR UPDATE: PEMBACAAN ELV625
// ===============================================================
    public void insertOrUpdatePembacaan625(Pembacaan625Model item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("id_pembacaan", item.getId_pembacaan());
            values.put("id_pengukuran", item.getId_pengukuran());
            values.put("hv_1", item.getHv_1());
            values.put("hv_2", item.getHv_2());
            values.put("hv_3", item.getHv_3());
            values.put("created_at", item.getCreated_at());
            values.put("updated_at", item.getUpdated_at());

            // Jika sudah ada -> update, kalau belum -> insert
            int updated = db.update(
                    "t_pembacaan_hdm_elv625",
                    values,
                    "id_pembacaan = ?",
                    new String[]{String.valueOf(item.getId_pembacaan())}
            );
            if (updated == 0) {
                db.insert("t_pembacaan_hdm_elv625", null, values);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "❌ insertOrUpdatePembacaan625 gagal: " + e.getMessage());
        }
    }

    // ===============================================================
// ✅ INSERT OR UPDATE: PEMBACAAN ELV600
// ===============================================================
    public void insertOrUpdatePembacaan600(Pembacaan600Model item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("id_pembacaan", item.getId_pembacaan());
            values.put("id_pengukuran", item.getId_pengukuran());
            values.put("hv_1", item.getHv_1());
            values.put("hv_2", item.getHv_2());
            values.put("hv_3", item.getHv_3());
            values.put("hv_4", item.getHv_4());
            values.put("hv_5", item.getHv_5());
            values.put("created_at", item.getCreated_at());
            values.put("updated_at", item.getUpdated_at());

            int updated = db.update(
                    "t_pembacaan_hdm_elv600",
                    values,
                    "id_pembacaan = ?",
                    new String[]{String.valueOf(item.getId_pembacaan())}
            );
            if (updated == 0) {
                db.insert("t_pembacaan_hdm_elv600", null, values);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "❌ insertOrUpdatePembacaan600 gagal: " + e.getMessage());
        }
    }


// ==================== GET BY ID_PENGUKURAN METHODS ====================

    // === PEMBACAAN ELV625 BERDASARKAN ID PENGUKURAN ===
    public List<Pembacaan625Model> getPembacaan625ByPengukuran(int idPengukuran) {
        List<Pembacaan625Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM t_pembacaan_hdm_elv625 WHERE id_pengukuran = ?", new String[]{String.valueOf(idPengukuran)});
        if (c.moveToFirst()) {
            do {
                Pembacaan625Model d = new Pembacaan625Model();
                d.setId_pembacaan(c.getInt(c.getColumnIndexOrThrow("id_pembacaan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }


    // === PEMBACAAN ELV600 BERDASARKAN ID PENGUKURAN ===
    public List<Pembacaan600Model> getPembacaan600ByPengukuran(int idPengukuran) {
        List<Pembacaan600Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM t_pembacaan_hdm_elv600 WHERE id_pengukuran = ?", new String[]{String.valueOf(idPengukuran)});
        if (c.moveToFirst()) {
            do {
                Pembacaan600Model d = new Pembacaan600Model();
                d.setId_pembacaan(c.getInt(c.getColumnIndexOrThrow("id_pembacaan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                d.setHv_4(c.getDouble(c.getColumnIndexOrThrow("hv_4")));
                d.setHv_5(c.getDouble(c.getColumnIndexOrThrow("hv_5")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }


    public List<Pergerakan625Model> getPergerakan625ByPengukuran(int idPengukuran) {
        List<Pergerakan625Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PERGERAKAN_625 + " WHERE id_pengukuran=?",
                new String[]{String.valueOf(idPengukuran)});
        if (c.moveToFirst()) {
            do {
                Pergerakan625Model d = new Pergerakan625Model();
                d.setId_pergerakan(c.getInt(c.getColumnIndexOrThrow("id_pergerakan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Pergerakan600Model> getPergerakan600ByPengukuran(int idPengukuran) {
        List<Pergerakan600Model> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PERGERAKAN_600 + " WHERE id_pengukuran=?",
                new String[]{String.valueOf(idPengukuran)});
        if (c.moveToFirst()) {
            do {
                Pergerakan600Model d = new Pergerakan600Model();
                d.setId_pergerakan(c.getInt(c.getColumnIndexOrThrow("id_pergerakan")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setHv_1(c.getDouble(c.getColumnIndexOrThrow("hv_1")));
                d.setHv_2(c.getDouble(c.getColumnIndexOrThrow("hv_2")));
                d.setHv_3(c.getDouble(c.getColumnIndexOrThrow("hv_3")));
                d.setHv_4(c.getDouble(c.getColumnIndexOrThrow("hv_4")));
                d.setHv_5(c.getDouble(c.getColumnIndexOrThrow("hv_5")));
                list.add(d);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // NEW: GET AMBANG BATAS BY PENGUKURAN METHODS
    public AmbangBatas625H1Model getAmbangBatas625H1ByPengukuran(int idPengukuran) {
        return getAmbangBatasByPengukuran(TABLE_AMBANG_BATAS_625_H1, idPengukuran, new AmbangBatas625H1Model());
    }

    public AmbangBatas625H2Model getAmbangBatas625H2ByPengukuran(int idPengukuran) {
        return getAmbangBatasByPengukuran(TABLE_AMBANG_BATAS_625_H2, idPengukuran, new AmbangBatas625H2Model());
    }

    public AmbangBatas625H3Model getAmbangBatas625H3ByPengukuran(int idPengukuran) {
        return getAmbangBatasByPengukuran(TABLE_AMBANG_BATAS_625_H3, idPengukuran, new AmbangBatas625H3Model());
    }

    public AmbangBatas600H1Model getAmbangBatas600H1ByPengukuran(int idPengukuran) {
        return getAmbangBatasByPengukuran(TABLE_AMBANG_BATAS_600_H1, idPengukuran, new AmbangBatas600H1Model());
    }

    public AmbangBatas600H2Model getAmbangBatas600H2ByPengukuran(int idPengukuran) {
        return getAmbangBatasByPengukuran(TABLE_AMBANG_BATAS_600_H2, idPengukuran, new AmbangBatas600H2Model());
    }

    public AmbangBatas600H3Model getAmbangBatas600H3ByPengukuran(int idPengukuran) {
        return getAmbangBatasByPengukuran(TABLE_AMBANG_BATAS_600_H3, idPengukuran, new AmbangBatas600H3Model());
    }

    public AmbangBatas600H4Model getAmbangBatas600H4ByPengukuran(int idPengukuran) {
        return getAmbangBatasByPengukuran(TABLE_AMBANG_BATAS_600_H4, idPengukuran, new AmbangBatas600H4Model());
    }

    public AmbangBatas600H5Model getAmbangBatas600H5ByPengukuran(int idPengukuran) {
        return getAmbangBatasByPengukuran(TABLE_AMBANG_BATAS_600_H5, idPengukuran, new AmbangBatas600H5Model());
    }

    // ==================== HELPER METHODS ====================

    private <T> T getAmbangBatasByPengukuran(String tableName, int idPengukuran, T model) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + tableName + " WHERE id_pengukuran = ?",
                new String[]{String.valueOf(idPengukuran)});

        if (c.moveToFirst()) {
            setAmbangBatasDataFromCursor(model, c);
        } else {
            model = null;
        }

        c.close();
        return model;
    }

    private void setAmbangBatasDataFromCursor(Object model, Cursor c) {
        if (model instanceof AmbangBatas625H1Model) {
            AmbangBatas625H1Model d = (AmbangBatas625H1Model) model;
            setAmbangBatasCommonData(d, c);
        } else if (model instanceof AmbangBatas625H2Model) {
            AmbangBatas625H2Model d = (AmbangBatas625H2Model) model;
            setAmbangBatasCommonData(d, c);
        } else if (model instanceof AmbangBatas625H3Model) {
            AmbangBatas625H3Model d = (AmbangBatas625H3Model) model;
            setAmbangBatasCommonData(d, c);
        } else if (model instanceof AmbangBatas600H1Model) {
            AmbangBatas600H1Model d = (AmbangBatas600H1Model) model;
            setAmbangBatasCommonData(d, c);
        } else if (model instanceof AmbangBatas600H2Model) {
            AmbangBatas600H2Model d = (AmbangBatas600H2Model) model;
            setAmbangBatasCommonData(d, c);
        } else if (model instanceof AmbangBatas600H3Model) {
            AmbangBatas600H3Model d = (AmbangBatas600H3Model) model;
            setAmbangBatasCommonData(d, c);
        } else if (model instanceof AmbangBatas600H4Model) {
            AmbangBatas600H4Model d = (AmbangBatas600H4Model) model;
            setAmbangBatasCommonData(d, c);
        } else if (model instanceof AmbangBatas600H5Model) {
            AmbangBatas600H5Model d = (AmbangBatas600H5Model) model;
            setAmbangBatasCommonData(d, c);
        }
    }

    private void setAmbangBatasCommonData(Object model, Cursor c) {
        try {
            // Common fields for all AmbangBatas models
            if (model instanceof AmbangBatas625H1Model) {
                AmbangBatas625H1Model d = (AmbangBatas625H1Model) model;
                d.setId_ambang_batas(c.getInt(c.getColumnIndexOrThrow("id_ambang_batas")));
                d.setId_pengukuran(c.getInt(c.getColumnIndexOrThrow("id_pengukuran")));
                d.setAman(c.getDouble(c.getColumnIndexOrThrow("aman")));
                d.setPeringatan(c.getDouble(c.getColumnIndexOrThrow("peringatan")));
                d.setBahaya(c.getDouble(c.getColumnIndexOrThrow("bahaya")));

                int pergerakanIndex = c.getColumnIndex("pergerakan");
                if (!c.isNull(pergerakanIndex)) {
                    d.setPergerakan(c.getDouble(pergerakanIndex));
                }

                d.setCreated_at(c.getString(c.getColumnIndexOrThrow("created_at")));
                d.setUpdated_at(c.getString(c.getColumnIndexOrThrow("updated_at")));
            }
            // Apply the same pattern for other models...
        } catch (Exception e) {
            Log.e(TAG, "Error setting ambang batas data from cursor: " + e.getMessage());
        }
    }

}