package com.example.app_dambody;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineDataHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "db_hdm_offline.db";
    private static final int DATABASE_VERSION = 4;

    // Tables untuk ELV625
    private static final String TABLE_PENGUKURAN_ELV625 = "offline_pengukuran_elv625";
    private static final String TABLE_DMA_ELV625 = "offline_dma_elv625";
    private static final String TABLE_DATA_ELV625 = "offline_data_elv625";

    // Tables untuk ELV600
    private static final String TABLE_PENGUKURAN_ELV600 = "offline_pengukuran_elv600";
    private static final String TABLE_DMA_ELV600 = "offline_dma_elv600";
    private static final String TABLE_DATA_ELV600 = "offline_data_elv600";

    // Common columns
    private static final String COL_ID = "id";
    private static final String COL_TEMP_ID = "temp_id";
    private static final String COL_JSON_DATA = "json_data";
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_SYNC_STATUS = "sync_status";

    public OfflineDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // ========== TABEL UNTUK ELV625 ==========

        // Table untuk pengukuran ELV625
        String createPengukuranElv625 = "CREATE TABLE " + TABLE_PENGUKURAN_ELV625 + "(" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_TEMP_ID + " TEXT UNIQUE," +
                COL_JSON_DATA + " TEXT," +
                COL_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                COL_SYNC_STATUS + " INTEGER DEFAULT 0)";
        db.execSQL(createPengukuranElv625);

        // Table untuk DMA ELV625
        String createDmaElv625 = "CREATE TABLE " + TABLE_DMA_ELV625 + "(" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_TEMP_ID + " TEXT UNIQUE," +
                COL_JSON_DATA + " TEXT," +
                COL_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                COL_SYNC_STATUS + " INTEGER DEFAULT 0)";
        db.execSQL(createDmaElv625);

        // Table untuk data ELV625 (HV1-HV3)
        String createDataElv625 = "CREATE TABLE " + TABLE_DATA_ELV625 + "(" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_TEMP_ID + " TEXT UNIQUE," +
                COL_JSON_DATA + " TEXT," +
                COL_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                COL_SYNC_STATUS + " INTEGER DEFAULT 0)";
        db.execSQL(createDataElv625);

        // ========== TABEL UNTUK ELV600 ==========

        // Table untuk pengukuran ELV600
        String createPengukuranElv600 = "CREATE TABLE " + TABLE_PENGUKURAN_ELV600 + "(" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_TEMP_ID + " TEXT UNIQUE," +
                COL_JSON_DATA + " TEXT," +
                COL_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                COL_SYNC_STATUS + " INTEGER DEFAULT 0)";
        db.execSQL(createPengukuranElv600);

        // Table untuk DMA ELV600
        String createDmaElv600 = "CREATE TABLE " + TABLE_DMA_ELV600 + "(" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_TEMP_ID + " TEXT UNIQUE," +
                COL_JSON_DATA + " TEXT," +
                COL_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                COL_SYNC_STATUS + " INTEGER DEFAULT 0)";
        db.execSQL(createDmaElv600);

        // Table untuk data ELV600 (HV1-HV5)
        String createDataElv600 = "CREATE TABLE " + TABLE_DATA_ELV600 + "(" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_TEMP_ID + " TEXT UNIQUE," +
                COL_JSON_DATA + " TEXT," +
                COL_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                COL_SYNC_STATUS + " INTEGER DEFAULT 0)";
        db.execSQL(createDataElv600);

        Log.d("HDM_OfflineDB", "Database tables created for ELV625 and ELV600");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop semua tabel jika upgrade
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN_ELV625);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DMA_ELV625);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_ELV625);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN_ELV600);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DMA_ELV600);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_ELV600);
        onCreate(db);
    }


    // ========== METHOD UNTUK ELV625 ==========

    // Insert data offline ELV625
    public boolean insertDataELV625(String tableType, String tempId, String jsonData) {
        String tableName = getTableNameELV625(tableType);
        return insertData(tableName, tempId, jsonData);
    }

    // Get unsynced data ELV625
    public List<Map<String, String>> getUnsyncedDataELV625(String tableType) {
        String tableName = getTableNameELV625(tableType);
        return getUnsyncedData(tableName);
    }

    // Check if has unsynced data ELV625
    public boolean hasUnsyncedDataELV625() {
        String[] tables = {
                TABLE_PENGUKURAN_ELV625,
                TABLE_DMA_ELV625,
                TABLE_DATA_ELV625
        };
        return hasUnsyncedData(tables);
    }

    // Delete data ELV625 setelah sync berhasil
    public boolean deleteByTempIdELV625(String tableType, String tempId) {
        String tableName = getTableNameELV625(tableType);
        return deleteByTempId(tableName, tempId);
    }

    // Get offline pengukuran master data ELV625
    public List<Map<String, String>> getPengukuranMasterELV625() {
        return getPengukuranMaster(TABLE_PENGUKURAN_ELV625);
    }

    // Get count of offline data ELV625
    public int getOfflineDataCountELV625() {
        String[] tables = {
                TABLE_PENGUKURAN_ELV625,
                TABLE_DMA_ELV625,
                TABLE_DATA_ELV625
        };
        return getOfflineDataCount(tables);
    }

    // ========== METHOD UNTUK ELV600 ==========

    // Insert data offline ELV600
    public boolean insertDataELV600(String tableType, String tempId, String jsonData) {
        String tableName = getTableNameELV600(tableType);
        return insertData(tableName, tempId, jsonData);
    }

    // Get unsynced data ELV600
    public List<Map<String, String>> getUnsyncedDataELV600(String tableType) {
        String tableName = getTableNameELV600(tableType);
        return getUnsyncedData(tableName);
    }

    // Check if has unsynced data ELV600
    public boolean hasUnsyncedDataELV600() {
        String[] tables = {
                TABLE_PENGUKURAN_ELV600,
                TABLE_DMA_ELV600,
                TABLE_DATA_ELV600
        };
        return hasUnsyncedData(tables);
    }

    // Delete data ELV600 setelah sync berhasil
    public boolean deleteByTempIdELV600(String tableType, String tempId) {
        String tableName = getTableNameELV600(tableType);
        return deleteByTempId(tableName, tempId);
    }

    // Get offline pengukuran master data ELV600
    public List<Map<String, String>> getPengukuranMasterELV600() {
        return getPengukuranMaster(TABLE_PENGUKURAN_ELV600);
    }

    // Get count of offline data ELV600
    public int getOfflineDataCountELV600() {
        String[] tables = {
                TABLE_PENGUKURAN_ELV600,
                TABLE_DMA_ELV600,
                TABLE_DATA_ELV600
        };
        return getOfflineDataCount(tables);
    }

    // ========== METHOD GENERIC (PRIVATE) ==========

    private boolean insertData(String tableName, String tempId, String jsonData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TEMP_ID, tempId);
        values.put(COL_JSON_DATA, jsonData);
        values.put(COL_SYNC_STATUS, 0); // 0 = unsynced

        long result = db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();

        boolean success = result != -1;
        if (success) {
            Log.d("HDM_OfflineDB", "Data saved: " + tableName + " tempId=" + tempId);
        } else {
            Log.e("HDM_OfflineDB", "Failed to save: " + tableName + " tempId=" + tempId);
        }
        return success;
    }

    private List<Map<String, String>> getUnsyncedData(String tableName) {
        List<Map<String, String>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + tableName + " WHERE " + COL_SYNC_STATUS + " = 0 ORDER BY " + COL_CREATED_AT + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                data.put("temp_id", cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_ID)));
                data.put("json", cursor.getString(cursor.getColumnIndexOrThrow(COL_JSON_DATA)));
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        Log.d("HDM_OfflineDB", "Found " + dataList.size() + " unsynced rows in " + tableName);
        return dataList;
    }

    private boolean hasUnsyncedData(String[] tables) {
        SQLiteDatabase db = this.getReadableDatabase();

        for (String table : tables) {
            String query = "SELECT COUNT(*) FROM " + table + " WHERE " + COL_SYNC_STATUS + " = 0";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                cursor.close();
                db.close();
                return true;
            }
            cursor.close();
        }
        db.close();
        return false;
    }

    private boolean deleteByTempId(String tableName, String tempId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(tableName, COL_TEMP_ID + " = ?", new String[]{tempId});
        db.close();

        boolean success = result > 0;
        if (success) {
            Log.d("HDM_OfflineDB", "Deleted: " + tableName + " tempId=" + tempId);
        }
        return success;
    }

    private List<Map<String, String>> getPengukuranMaster(String tableName) {
        List<Map<String, String>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " + COL_TEMP_ID + " as id, json_data FROM " + tableName +
                " WHERE " + COL_SYNC_STATUS + " = 0 ORDER BY " + COL_CREATED_AT + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                try {
                    String jsonStr = cursor.getString(cursor.getColumnIndexOrThrow("json_data"));
                    JSONObject json = new JSONObject(jsonStr);
                    Map<String, String> data = new HashMap<>();
                    data.put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")));
                    data.put("tanggal", json.optString("tanggal", "Offline Data"));
                    dataList.add(data);
                } catch (Exception e) {
                    Log.e("HDM_OfflineDB", "Error parsing JSON: " + tableName, e);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return dataList;
    }

    private int getOfflineDataCount(String[] tables) {
        SQLiteDatabase db = this.getReadableDatabase();
        int total = 0;

        for (String table : tables) {
            String query = "SELECT COUNT(*) FROM " + table + " WHERE " + COL_SYNC_STATUS + " = 0";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                total += cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return total;
    }

    // ========== HELPER METHODS ==========

    private String getTableNameELV625(String tableType) {
        switch (tableType) {
            case "pengukuran": return TABLE_PENGUKURAN_ELV625;
            case "dma": return TABLE_DMA_ELV625;
            case "data": return TABLE_DATA_ELV625;
            default: return TABLE_DATA_ELV625;
        }
    }

    private String getTableNameELV600(String tableType) {
        switch (tableType) {
            case "pengukuran": return TABLE_PENGUKURAN_ELV600;
            case "dma": return TABLE_DMA_ELV600;
            case "data": return TABLE_DATA_ELV600;
            default: return TABLE_DATA_ELV600;
        }
    }
}