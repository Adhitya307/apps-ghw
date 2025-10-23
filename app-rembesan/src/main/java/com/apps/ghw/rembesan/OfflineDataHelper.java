package com.apps.ghw.rembesan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineDataHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "offline_sync.db";
    private static final int DB_VERSION = 3; // <- dinaikkan versi agar onUpgrade dijalankan

    public OfflineDataHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS pengukuran (" +
                "temp_id TEXT PRIMARY KEY, " +
                "json TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS thomson (" +
                "temp_id TEXT PRIMARY KEY, " +
                "json TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS sr (" +
                "temp_id TEXT PRIMARY KEY, " +
                "json TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS bocoran (" +
                "temp_id TEXT PRIMARY KEY, " +
                "json TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        // ✅ Tambahan tabel baru untuk TMA Waduk
        db.execSQL("CREATE TABLE IF NOT EXISTS tma_waduk (" +
                "temp_id TEXT PRIMARY KEY, " +
                "json TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS pengukuran_master (" +
                "id INTEGER PRIMARY KEY, tanggal TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try { db.execSQL("ALTER TABLE pengukuran ADD COLUMN is_synced INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE thomson ADD COLUMN is_synced INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE sr ADD COLUMN is_synced INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE bocoran ADD COLUMN is_synced INTEGER DEFAULT 0"); } catch (Exception ignored) {}

        // ✅ Tambahkan tabel tma_waduk saat upgrade
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS tma_waduk (" +
                    "temp_id TEXT PRIMARY KEY, " +
                    "json TEXT, " +
                    "is_synced INTEGER DEFAULT 0)");
        } catch (Exception ignored) {}

        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS pengukuran_master (" +
                    "id INTEGER PRIMARY KEY, tanggal TEXT)");
        } catch (Exception ignored) {}
    }

    // ============= INSERT / UPDATE =============

    public boolean insertData(String table, String tempId, String json) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("temp_id", tempId);
        values.put("json", json);
        values.put("is_synced", 0);

        long result = db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    // ============= QUERY DATA =============

    // Semua data (debugging)
    public List<Map<String, String>> getAllData(String table) {
        return queryData("SELECT * FROM " + table);
    }

    // Hanya data yang belum tersinkron
    public List<Map<String, String>> getUnsyncedData(String table) {
        return queryData("SELECT * FROM " + table + " WHERE is_synced = 0");
    }

    // Helper umum untuk query
    private List<Map<String, String>> queryData(String sql) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Map<String, String> item = new HashMap<>();
            item.put("temp_id", cursor.getString(cursor.getColumnIndexOrThrow("temp_id")));
            item.put("json", cursor.getString(cursor.getColumnIndexOrThrow("json")));
            item.put("is_synced", cursor.getString(cursor.getColumnIndexOrThrow("is_synced")));
            list.add(item);
        }
        cursor.close();
        return list;
    }

    // Cek apakah ada data offline yang belum tersinkron
    public boolean hasUnsyncedData() {
        return !getUnsyncedData("pengukuran").isEmpty() ||
                !getUnsyncedData("thomson").isEmpty() ||
                !getUnsyncedData("sr").isEmpty() ||
                !getUnsyncedData("bocoran").isEmpty() ||
                !getUnsyncedData("tma_waduk").isEmpty(); // ✅ tambahan
    }

    // Tandai data sudah tersinkron
    public void markAsSynced(String table, String tempId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_synced", 1);
        db.update(table, values, "temp_id = ?", new String[]{tempId});
    }

    // ============= DELETE =============

    public void deleteByTempId(String table, String tempId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, "temp_id = ?", new String[]{tempId});
    }

    public void deleteAll(String table) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, null, null);
    }

    // ============= PENGUKURAN MASTER =============

    public void insertPengukuranMaster(int id, String tanggal) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("tanggal", tanggal);
        db.insertWithOnConflict("pengukuran_master", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void clearPengukuranMaster() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("pengukuran_master", null, null);
    }

    public List<Map<String, String>> getPengukuranMaster() {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM pengukuran_master", null);
        while (c.moveToNext()) {
            Map<String, String> row = new HashMap<>();
            row.put("id", c.getString(c.getColumnIndexOrThrow("id")));
            row.put("tanggal", c.getString(c.getColumnIndexOrThrow("tanggal")));
            list.add(row);
        }
        c.close();
        return list;
    }
}
