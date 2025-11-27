package com.example.app_leftpiezo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OfflineDataHelperLeftPiezo extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "leftpiezo_offline.db";
    private static final int DATABASE_VERSION = 1;

    // Tables
    private static final String TABLE_PENGUKURAN = "offline_pengukuran";
    private static final String TABLE_DATA = "offline_data";

    // Common columns
    private static final String KEY_ID = "id";
    private static final String KEY_TEMP_ID = "temp_id";
    private static final String KEY_JSON_DATA = "json_data";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_SYNC_STATUS = "sync_status";

    // Additional columns for data table
    private static final String KEY_PENGUKURAN_ID = "pengukuran_id";
    private static final String KEY_MODE = "mode";

    public OfflineDataHelperLeftPiezo(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PENGUKURAN_TABLE = "CREATE TABLE " + TABLE_PENGUKURAN + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TEMP_ID + " TEXT UNIQUE,"
                + KEY_JSON_DATA + " TEXT,"
                + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + KEY_SYNC_STATUS + " INTEGER DEFAULT 0" + ")";

        String CREATE_DATA_TABLE = "CREATE TABLE " + TABLE_DATA + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TEMP_ID + " TEXT UNIQUE,"
                + KEY_JSON_DATA + " TEXT,"
                + KEY_PENGUKURAN_ID + " INTEGER,"
                + KEY_MODE + " TEXT,"
                + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + KEY_SYNC_STATUS + " INTEGER DEFAULT 0" + ")";

        db.execSQL(CREATE_PENGUKURAN_TABLE);
        db.execSQL(CREATE_DATA_TABLE);

        Log.d("DB_CREATION", "Database tables created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);
        onCreate(db);
    }

    // ==================== BASIC CRUD OPERATIONS ====================

    /**
     * Insert data untuk tabel pengukuran atau data (basic)
     */
    public boolean insertDataLeftPiezo(String tableType, String tempId, String jsonData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TEMP_ID, tempId);
        values.put(KEY_JSON_DATA, jsonData);
        values.put(KEY_SYNC_STATUS, 0);

        String tableName = tableType.equals("pengukuran") ? TABLE_PENGUKURAN : TABLE_DATA;
        long result = db.insert(tableName, null, values);

        Log.d("DB_INSERT", "Insert " + tableType + " - tempId: " + tempId + ", success: " + (result != -1));
        return result != -1;
    }

    /**
     * Insert data untuk tabel data dengan mode dan pengukuran_id
     */
    public boolean insertDataLeftPiezoWithMode(String tableType, String tempId, String jsonData,
                                               int pengukuranId, String mode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TEMP_ID, tempId);
        values.put(KEY_JSON_DATA, jsonData);
        values.put(KEY_PENGUKURAN_ID, pengukuranId);
        values.put(KEY_MODE, mode);
        values.put(KEY_SYNC_STATUS, 0);

        long result = db.insert(TABLE_DATA, null, values);

        Log.d("DB_INSERT", "Insert data with mode - tempId: " + tempId +
                ", pengukuranId: " + pengukuranId + ", mode: " + mode + ", success: " + (result != -1));
        return result != -1;
    }

    // ==================== GET UNSYNCED DATA ====================

    /**
     * Get semua data yang belum tersinkronisasi
     */
    public List<Map<String, String>> getUnsyncedDataLeftPiezo(String tableType) {
        List<Map<String, String>> dataList = new ArrayList<>();
        String tableName = tableType.equals("pengukuran") ? TABLE_PENGUKURAN : TABLE_DATA;

        String selectQuery = "SELECT * FROM " + tableName + " WHERE " + KEY_SYNC_STATUS + " = 0";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        Log.d("DB_QUERY", "Getting unsynced data from " + tableName + ", count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                data.put("temp_id", cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEMP_ID)));
                data.put("json", cursor.getString(cursor.getColumnIndexOrThrow(KEY_JSON_DATA)));

                if (tableName.equals(TABLE_DATA)) {
                    data.put("pengukuran_id", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PENGUKURAN_ID))));
                    data.put("mode", cursor.getString(cursor.getColumnIndexOrThrow(KEY_MODE)));
                }
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dataList;
    }

    /**
     * Get data unsynced yang dikelompokkan berdasarkan pengukuran_id
     */
    public Map<Integer, List<Map<String, String>>> getUnsyncedDataGroupedByPengukuran() {
        Map<Integer, List<Map<String, String>>> groupedData = new HashMap<>();

        String selectQuery = "SELECT * FROM " + TABLE_DATA + " WHERE " + KEY_SYNC_STATUS + " = 0";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        Log.d("DB_QUERY", "Getting grouped unsynced data, count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                int pengukuranId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PENGUKURAN_ID));
                Map<String, String> data = new HashMap<>();
                data.put("temp_id", cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEMP_ID)));
                data.put("json", cursor.getString(cursor.getColumnIndexOrThrow(KEY_JSON_DATA)));
                data.put("mode", cursor.getString(cursor.getColumnIndexOrThrow(KEY_MODE)));

                if (!groupedData.containsKey(pengukuranId)) {
                    groupedData.put(pengukuranId, new ArrayList<Map<String, String>>());
                }
                groupedData.get(pengukuranId).add(data);

            } while (cursor.moveToNext());
        }
        cursor.close();
        return groupedData;
    }

    // ==================== GET DATA BY SPECIFIC CRITERIA ====================

    /**
     * Get data untuk pengukuran_id dan lokasi tertentu
     */
    public Map<String, String> getLeftPiezoData(int pengukuranId, String lokasi) {
        Map<String, String> data = new HashMap<>();
        String mode = "pembacaan_" + lokasi.toLowerCase();

        String selectQuery = "SELECT " + KEY_JSON_DATA + " FROM " + TABLE_DATA +
                " WHERE " + KEY_PENGUKURAN_ID + " = ? AND " + KEY_MODE + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(pengukuranId), mode});

        Log.d("DB_QUERY", "Getting data for pengukuran: " + pengukuranId + ", lokasi: " + lokasi);

        if (cursor.moveToFirst()) {
            String jsonData = cursor.getString(cursor.getColumnIndexOrThrow(KEY_JSON_DATA));
            try {
                JSONObject json = new JSONObject(jsonData);
                // Extract semua field dari JSON menggunakan Iterator
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    data.put(key, json.getString(key));
                }

                Log.d("DB_QUERY", "Found data: " + data.toString());
            } catch (Exception e) {
                Log.e("DB_QUERY", "Error parsing JSON: " + e.getMessage());
            }
        } else {
            Log.d("DB_QUERY", "No data found for pengukuran: " + pengukuranId + ", lokasi: " + lokasi);
        }
        cursor.close();
        return data;
    }

    /**
     * Get semua lokasi yang perlu dihitung untuk pengukuran tertentu
     */
    public List<String> getLokasiForPengukuran(int pengukuranId) {
        List<String> lokasiList = new ArrayList<>();

        String selectQuery = "SELECT " + KEY_MODE + " FROM " + TABLE_DATA +
                " WHERE " + KEY_PENGUKURAN_ID + " = ? AND " + KEY_SYNC_STATUS + " = 0";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(pengukuranId)});

        Log.d("DB_QUERY", "Getting lokasi for pengukuran: " + pengukuranId);

        if (cursor.moveToFirst()) {
            do {
                String mode = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MODE));
                if (mode != null && mode.startsWith("pembacaan_")) {
                    String lokasi = mode.replace("pembacaan_", "").toUpperCase();
                    if (!lokasiList.contains(lokasi)) {
                        lokasiList.add(lokasi);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        Log.d("DB_QUERY", "Found lokasi: " + lokasiList.toString());
        return lokasiList;
    }

    /**
     * Get data master pengukuran untuk dropdown
     */
    public List<Map<String, String>> getPengukuranMasterLeftPiezo() {
        List<Map<String, String>> dataList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_PENGUKURAN + " ORDER BY " + KEY_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        Log.d("DB_QUERY", "Getting pengukuran master, count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                String tempId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEMP_ID));
                String jsonData = cursor.getString(cursor.getColumnIndexOrThrow(KEY_JSON_DATA));

                data.put("temp_id", tempId);

                try {
                    JSONObject json = new JSONObject(jsonData);
                    if (json.has("tanggal")) {
                        data.put("tanggal", json.getString("tanggal"));
                    } else {
                        data.put("tanggal", "Tanggal tidak tersedia");
                    }

                    if (json.has("id_pengukuran")) {
                        data.put("id_pengukuran", json.getString("id_pengukuran"));
                    } else {
                        // Jika tidak ada id_pengukuran, gunakan temp_id (untuk data offline)
                        data.put("id_pengukuran", tempId);
                    }

                    // Tambahkan info tambahan jika ada
                    if (json.has("tahun")) data.put("tahun", json.getString("tahun"));
                    if (json.has("bulan")) data.put("bulan", json.getString("bulan"));
                    if (json.has("periode")) data.put("periode", json.getString("periode"));

                } catch (Exception e) {
                    Log.e("DB_QUERY", "Error parsing pengukuran JSON: " + e.getMessage());
                    data.put("tanggal", "Error parsing data");
                    data.put("id_pengukuran", tempId);
                }
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dataList;
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * Delete data by temp_id
     */
    public void deleteByTempIdLeftPiezo(String tableType, String tempId) {
        String tableName = tableType.equals("pengukuran") ? TABLE_PENGUKURAN : TABLE_DATA;
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(tableName, KEY_TEMP_ID + " = ?", new String[]{tempId});

        Log.d("DB_DELETE", "Deleted from " + tableName + " - tempId: " + tempId + ", rows affected: " + rowsDeleted);
    }

    /**
     * Mark data as synced (update sync_status to 1)
     */
    public void markAsSynced(String tableType, String tempId) {
        String tableName = tableType.equals("pengukuran") ? TABLE_PENGUKURAN : TABLE_DATA;
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SYNC_STATUS, 1);

        int rowsUpdated = db.update(tableName, values, KEY_TEMP_ID + " = ?", new String[]{tempId});

        Log.d("DB_UPDATE", "Marked as synced - " + tableName + " - tempId: " + tempId + ", rows updated: " + rowsUpdated);
    }

    // ==================== CHECK OPERATIONS ====================

    /**
     * Check if there is any unsynced data
     */
    public boolean hasUnsyncedDataLeftPiezo() {
        SQLiteDatabase db = this.getReadableDatabase();

        String queryPengukuran = "SELECT COUNT(*) FROM " + TABLE_PENGUKURAN + " WHERE " + KEY_SYNC_STATUS + " = 0";
        String queryData = "SELECT COUNT(*) FROM " + TABLE_DATA + " WHERE " + KEY_SYNC_STATUS + " = 0";

        Cursor cursor1 = db.rawQuery(queryPengukuran, null);
        Cursor cursor2 = db.rawQuery(queryData, null);

        cursor1.moveToFirst();
        cursor2.moveToFirst();

        int countPengukuran = cursor1.getInt(0);
        int countData = cursor2.getInt(0);

        cursor1.close();
        cursor2.close();

        boolean hasUnsynced = (countPengukuran + countData) > 0;
        Log.d("DB_CHECK", "Has unsynced data - pengukuran: " + countPengukuran +
                ", data: " + countData + ", total: " + (countPengukuran + countData));

        return hasUnsynced;
    }

    /**
     * Get total count of offline data
     */
    public int getOfflineDataCountLeftPiezo() {
        SQLiteDatabase db = this.getReadableDatabase();

        String queryPengukuran = "SELECT COUNT(*) FROM " + TABLE_PENGUKURAN + " WHERE " + KEY_SYNC_STATUS + " = 0";
        String queryData = "SELECT COUNT(*) FROM " + TABLE_DATA + " WHERE " + KEY_SYNC_STATUS + " = 0";

        Cursor cursor1 = db.rawQuery(queryPengukuran, null);
        Cursor cursor2 = db.rawQuery(queryData, null);

        cursor1.moveToFirst();
        cursor2.moveToFirst();

        int countPengukuran = cursor1.getInt(0);
        int countData = cursor2.getInt(0);

        cursor1.close();
        cursor2.close();

        int total = countPengukuran + countData;
        Log.d("DB_COUNT", "Offline data count - pengukuran: " + countPengukuran +
                ", data: " + countData + ", total: " + total);

        return total;
    }

    /**
     * Check if specific data exists for pengukuran and lokasi
     */
    public boolean isDataExists(int pengukuranId, String lokasi) {
        String mode = "pembacaan_" + lokasi.toLowerCase();

        String selectQuery = "SELECT COUNT(*) FROM " + TABLE_DATA +
                " WHERE " + KEY_PENGUKURAN_ID + " = ? AND " + KEY_MODE + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(pengukuranId), mode});

        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        Log.d("DB_CHECK", "Data exists check - pengukuran: " + pengukuranId +
                ", lokasi: " + lokasi + ", exists: " + (count > 0));

        return count > 0;
    }

    /**
     * Get all data for specific pengukuran (all lokasi)
     */
    public List<Map<String, String>> getAllDataForPengukuran(int pengukuranId) {
        List<Map<String, String>> dataList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_DATA + " WHERE " + KEY_PENGUKURAN_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(pengukuranId)});

        Log.d("DB_QUERY", "Getting all data for pengukuran: " + pengukuranId + ", count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                data.put("temp_id", cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEMP_ID)));
                data.put("json", cursor.getString(cursor.getColumnIndexOrThrow(KEY_JSON_DATA)));
                data.put("mode", cursor.getString(cursor.getColumnIndexOrThrow(KEY_MODE)));
                data.put("pengukuran_id", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PENGUKURAN_ID))));
                data.put("sync_status", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SYNC_STATUS))));

                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dataList;
    }

    /**
     * Clean up old synced data (optional - untuk maintenance)
     */
    public void cleanupOldData() {
        SQLiteDatabase db = this.getWritableDatabase();

        // Hapus data yang sudah disinkronisasi lebih dari 30 hari
        int deletedPengukuran = db.delete(TABLE_PENGUKURAN,
                KEY_SYNC_STATUS + " = 1 AND " + KEY_CREATED_AT + " < datetime('now', '-30 days')", null);

        int deletedData = db.delete(TABLE_DATA,
                KEY_SYNC_STATUS + " = 1 AND " + KEY_CREATED_AT + " < datetime('now', '-30 days')", null);

        Log.d("DB_CLEANUP", "Cleaned up old data - pengukuran: " + deletedPengukuran +
                ", data: " + deletedData);
    }

    /**
     * Get database info (for debugging)
     */
    public Map<String, Integer> getDatabaseInfo() {
        Map<String, Integer> info = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Count total records
        Cursor cursor1 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PENGUKURAN, null);
        Cursor cursor2 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_DATA, null);

        // Count unsynced records
        Cursor cursor3 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PENGUKURAN + " WHERE " + KEY_SYNC_STATUS + " = 0", null);
        Cursor cursor4 = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_DATA + " WHERE " + KEY_SYNC_STATUS + " = 0", null);

        cursor1.moveToFirst();
        cursor2.moveToFirst();
        cursor3.moveToFirst();
        cursor4.moveToFirst();

        info.put("total_pengukuran", cursor1.getInt(0));
        info.put("total_data", cursor2.getInt(0));
        info.put("unsynced_pengukuran", cursor3.getInt(0));
        info.put("unsynced_data", cursor4.getInt(0));

        cursor1.close();
        cursor2.close();
        cursor3.close();
        cursor4.close();

        return info;
    }

    /**
     * Close database connection
     */
    @Override
    public synchronized void close() {
        super.close();
        Log.d("DB_CLOSE", "Database connection closed");
    }
}