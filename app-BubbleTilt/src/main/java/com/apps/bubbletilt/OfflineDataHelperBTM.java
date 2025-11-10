package com.apps.bubbletilt;

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

public class OfflineDataHelperBTM extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bubbletilt_offline.db";
    private static final int DATABASE_VERSION = 1;

    // Table untuk data offline BTM
    private static final String TABLE_OFFLINE_BTM = "offline_data_btm";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TABLE_TYPE = "table_type";
    private static final String COLUMN_TEMP_ID = "temp_id";
    private static final String COLUMN_JSON_DATA = "json_data";
    private static final String COLUMN_IS_SYNCED = "is_synced";
    private static final String COLUMN_CREATED_AT = "created_at";

    // Table untuk master pengukuran offline
    private static final String TABLE_PENGUKURAN_MASTER = "pengukuran_master_btm";
    private static final String COLUMN_PENGUKURAN_ID = "pengukuran_id";
    private static final String COLUMN_TAHUN = "tahun";
    private static final String COLUMN_BULAN = "bulan";
    private static final String COLUMN_PERIODE = "periode";
    private static final String COLUMN_TANGGAL = "tanggal";

    // Table untuk data BT1-BT8 offline
    private static final String TABLE_BT1 = "bt1_data";
    private static final String TABLE_BT2 = "bt2_data";
    private static final String TABLE_BT3 = "bt3_data";
    private static final String TABLE_BT4 = "bt4_data";
    private static final String TABLE_BT5 = "bt5_data";
    private static final String TABLE_BT6 = "bt6_data";
    private static final String TABLE_BT7 = "bt7_data";
    private static final String TABLE_BT8 = "bt8_data";

    // Common columns untuk semua table BT
    private static final String COLUMN_BT_ID = "bt_id";
    private static final String COLUMN_US_GP = "us_gp";
    private static final String COLUMN_US_ARAH = "us_arah";
    private static final String COLUMN_TB_GP = "tb_gp";
    private static final String COLUMN_TB_ARAH = "tb_arah";

    public OfflineDataHelperBTM(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table untuk data offline BTM
        String CREATE_OFFLINE_BTM_TABLE = "CREATE TABLE " + TABLE_OFFLINE_BTM + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TABLE_TYPE + " TEXT,"
                + COLUMN_TEMP_ID + " TEXT,"
                + COLUMN_JSON_DATA + " TEXT,"
                + COLUMN_IS_SYNCED + " INTEGER DEFAULT 0,"
                + COLUMN_CREATED_AT + " INTEGER" + ")";
        db.execSQL(CREATE_OFFLINE_BTM_TABLE);

        // Create table untuk master pengukuran
        String CREATE_PENGUKURAN_MASTER_TABLE = "CREATE TABLE " + TABLE_PENGUKURAN_MASTER + "("
                + COLUMN_PENGUKURAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TAHUN + " TEXT,"
                + COLUMN_BULAN + " TEXT,"
                + COLUMN_PERIODE + " TEXT,"
                + COLUMN_TANGGAL + " TEXT,"
                + COLUMN_IS_SYNCED + " INTEGER DEFAULT 0,"
                + COLUMN_CREATED_AT + " INTEGER" + ")";
        db.execSQL(CREATE_PENGUKURAN_MASTER_TABLE);

        // Create tables untuk BT1-BT8
        createBTTable(db, TABLE_BT1);
        createBTTable(db, TABLE_BT2);
        createBTTable(db, TABLE_BT3);
        createBTTable(db, TABLE_BT4);
        createBTTable(db, TABLE_BT5);
        createBTTable(db, TABLE_BT6);
        createBTTable(db, TABLE_BT7);
        createBTTable(db, TABLE_BT8);
    }

    private void createBTTable(SQLiteDatabase db, String tableName) {
        String CREATE_BT_TABLE = "CREATE TABLE " + tableName + "("
                + COLUMN_BT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PENGUKURAN_ID + " INTEGER,"
                + COLUMN_US_GP + " REAL,"
                + COLUMN_US_ARAH + " TEXT,"
                + COLUMN_TB_GP + " REAL,"
                + COLUMN_TB_ARAH + " TEXT,"
                + COLUMN_IS_SYNCED + " INTEGER DEFAULT 0,"
                + COLUMN_CREATED_AT + " INTEGER" + ")";
        db.execSQL(CREATE_BT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFLINE_BTM);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN_MASTER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT1);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT2);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT3);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT4);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT5);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT6);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT7);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT8);
        onCreate(db);
    }

    // ==================== METHODS UNTUK OFFLINE DATA BTM ====================

    public boolean insertDataBTM(String tableType, String tempId, String jsonData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TABLE_TYPE, tableType);
        values.put(COLUMN_TEMP_ID, tempId);
        values.put(COLUMN_JSON_DATA, jsonData);
        values.put(COLUMN_IS_SYNCED, 0);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());

        long result = db.insert(TABLE_OFFLINE_BTM, null, values);
        return result != -1;
    }

    public List<Map<String, String>> getUnsyncedDataBTM(String tableType) {
        List<Map<String, String>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_OFFLINE_BTM + " WHERE " +
                COLUMN_TABLE_TYPE + " = ? AND " + COLUMN_IS_SYNCED + " = 0 ORDER BY " + COLUMN_CREATED_AT + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{tableType});

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                data.put("temp_id", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEMP_ID)));
                data.put("json", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_JSON_DATA)));
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dataList;
    }

    public boolean hasUnsyncedDataBTM() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_OFFLINE_BTM + " WHERE " + COLUMN_IS_SYNCED + " = 0";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }

    public void deleteByTempIdBTM(String tableType, String tempId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_OFFLINE_BTM, COLUMN_TABLE_TYPE + " = ? AND " + COLUMN_TEMP_ID + " = ?",
                new String[]{tableType, tempId});
    }

    public int getOfflineDataCountBTM() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_OFFLINE_BTM + " WHERE " + COLUMN_IS_SYNCED + " = 0";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public List<Map<String, String>> getPengukuranMasterBTM() {
        List<Map<String, String>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_OFFLINE_BTM + " WHERE " + COLUMN_TABLE_TYPE + " = 'pengukuran' ORDER BY " + COLUMN_CREATED_AT + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                try {
                    String jsonStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_JSON_DATA));
                    JSONObject json = new JSONObject(jsonStr);
                    Map<String, String> data = new HashMap<>();
                    data.put("id_pengukuran", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEMP_ID)));
                    data.put("tanggal", json.optString("tanggal", "Unknown"));
                    dataList.add(data);
                } catch (Exception e) {
                    Log.e("GET_PENGUKURAN_BTM", "Error parsing JSON: " + e.getMessage());
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dataList;
    }

    public Map<String, String> getBTMData(int pengukuranId, int btNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, String> data = new HashMap<>();

        String query = "SELECT * FROM " + TABLE_OFFLINE_BTM + " WHERE " + COLUMN_TABLE_TYPE + " = 'data' AND " +
                COLUMN_JSON_DATA + " LIKE ? ORDER BY " + COLUMN_CREATED_AT + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{"%\"pengukuran_id\":\"" + pengukuranId + "\",\"bt_number\":\"" + btNumber + "\"%"});

        if (cursor.moveToFirst()) {
            try {
                String jsonStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_JSON_DATA));
                JSONObject json = new JSONObject(jsonStr);
                data.put("US_GP", json.optString("us_gp", ""));
                data.put("US_Arah", json.optString("us_arah", ""));
                data.put("TB_GP", json.optString("tb_gp", ""));
                data.put("TB_Arah", json.optString("tb_arah", ""));
            } catch (Exception e) {
                Log.e("GET_BTM_DATA", "Error parsing JSON: " + e.getMessage());
            }
        }
        cursor.close();
        return data;
    }

    // ==================== METHODS UNTUK PENGUKURAN MASTER ====================

    public long insertPengukuranMasterBTM(String tahun, String bulan, String periode, String tanggal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TAHUN, tahun);
        values.put(COLUMN_BULAN, bulan);
        values.put(COLUMN_PERIODE, periode);
        values.put(COLUMN_TANGGAL, tanggal);
        values.put(COLUMN_IS_SYNCED, 0);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());

        return db.insert(TABLE_PENGUKURAN_MASTER, null, values);
    }

    public List<Map<String, String>> getAllPengukuranMasterBTM() {
        List<Map<String, String>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_PENGUKURAN_MASTER + " ORDER BY " + COLUMN_CREATED_AT + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                data.put("pengukuran_id", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PENGUKURAN_ID))));
                data.put("tahun", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAHUN)));
                data.put("bulan", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BULAN)));
                data.put("periode", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIODE)));
                data.put("tanggal", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TANGGAL)));
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dataList;
    }

    // ==================== METHODS UNTUK BT1-BT8 ====================

    public long insertBTDataBTM(int btNumber, int pengukuranId, double usGp, String usArah, double tbGp, String tbArah) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tableName = getBTTableName(btNumber);

        ContentValues values = new ContentValues();
        values.put(COLUMN_PENGUKURAN_ID, pengukuranId);
        values.put(COLUMN_US_GP, usGp);
        values.put(COLUMN_US_ARAH, usArah);
        values.put(COLUMN_TB_GP, tbGp);
        values.put(COLUMN_TB_ARAH, tbArah);
        values.put(COLUMN_IS_SYNCED, 0);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());

        return db.insert(tableName, null, values);
    }

    public Map<String, Object> getBTDataBTM(int btNumber, int pengukuranId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String tableName = getBTTableName(btNumber);
        Map<String, Object> data = new HashMap<>();

        String query = "SELECT * FROM " + tableName + " WHERE " + COLUMN_PENGUKURAN_ID + " = ? ORDER BY " + COLUMN_CREATED_AT + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(pengukuranId)});

        if (cursor.moveToFirst()) {
            data.put("us_gp", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_US_GP)));
            data.put("us_arah", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_US_ARAH)));
            data.put("tb_gp", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TB_GP)));
            data.put("tb_arah", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TB_ARAH)));
        }
        cursor.close();
        return data;
    }

    public List<Map<String, Object>> getAllBTDataBTM(int btNumber) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String tableName = getBTTableName(btNumber);

        String query = "SELECT * FROM " + tableName + " ORDER BY " + COLUMN_CREATED_AT + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> data = new HashMap<>();
                data.put("bt_id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BT_ID)));
                data.put("pengukuran_id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PENGUKURAN_ID)));
                data.put("us_gp", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_US_GP)));
                data.put("us_arah", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_US_ARAH)));
                data.put("tb_gp", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TB_GP)));
                data.put("tb_arah", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TB_ARAH)));
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dataList;
    }

    public boolean updateBTDataBTM(int btNumber, int btId, double usGp, String usArah, double tbGp, String tbArah) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tableName = getBTTableName(btNumber);

        ContentValues values = new ContentValues();
        values.put(COLUMN_US_GP, usGp);
        values.put(COLUMN_US_ARAH, usArah);
        values.put(COLUMN_TB_GP, tbGp);
        values.put(COLUMN_TB_ARAH, tbArah);
        values.put(COLUMN_IS_SYNCED, 0);

        int result = db.update(tableName, values, COLUMN_BT_ID + " = ?", new String[]{String.valueOf(btId)});
        return result > 0;
    }

    public boolean deleteBTDataBTM(int btNumber, int btId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tableName = getBTTableName(btNumber);

        int result = db.delete(tableName, COLUMN_BT_ID + " = ?", new String[]{String.valueOf(btId)});
        return result > 0;
    }

    public boolean hasUnsyncedBTDataBTM(int btNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        String tableName = getBTTableName(btNumber);

        String query = "SELECT COUNT(*) FROM " + tableName + " WHERE " + COLUMN_IS_SYNCED + " = 0";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }

    public List<Map<String, Object>> getUnsyncedBTDataBTM(int btNumber) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String tableName = getBTTableName(btNumber);

        String query = "SELECT * FROM " + tableName + " WHERE " + COLUMN_IS_SYNCED + " = 0 ORDER BY " + COLUMN_CREATED_AT + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> data = new HashMap<>();
                data.put("bt_id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BT_ID)));
                data.put("pengukuran_id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PENGUKURAN_ID)));
                data.put("us_gp", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_US_GP)));
                data.put("us_arah", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_US_ARAH)));
                data.put("tb_gp", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TB_GP)));
                data.put("tb_arah", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TB_ARAH)));
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dataList;
    }

    public void markBTSyncedBTM(int btNumber, int btId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tableName = getBTTableName(btNumber);

        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_SYNCED, 1);

        db.update(tableName, values, COLUMN_BT_ID + " = ?", new String[]{String.valueOf(btId)});
    }

    // ==================== HELPER METHODS ====================

    private String getBTTableName(int btNumber) {
        switch (btNumber) {
            case 1: return TABLE_BT1;
            case 2: return TABLE_BT2;
            case 3: return TABLE_BT3;
            case 4: return TABLE_BT4;
            case 5: return TABLE_BT5;
            case 6: return TABLE_BT6;
            case 7: return TABLE_BT7;
            case 8: return TABLE_BT8;
            default: return TABLE_BT1;
        }
    }

    // Method untuk mendapatkan semua data dari semua BT
    public Map<Integer, List<Map<String, Object>>> getAllBTDataAllBTM() {
        Map<Integer, List<Map<String, Object>>> allData = new HashMap<>();

        for (int i = 1; i <= 8; i++) {
            List<Map<String, Object>> btData = getAllBTDataBTM(i);
            allData.put(i, btData);
        }

        return allData;
    }

    // Method untuk menghapus semua data
    public void clearAllDataBTM() {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_OFFLINE_BTM, null, null);
        db.delete(TABLE_PENGUKURAN_MASTER, null, null);

        for (int i = 1; i <= 8; i++) {
            db.delete(getBTTableName(i), null, null);
        }
    }

    // Method untuk mendapatkan statistik data
    public Map<String, Integer> getDataStatisticsBTM() {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, Integer> stats = new HashMap<>();

        // Hitung total data di setiap table
        for (int i = 1; i <= 8; i++) {
            String tableName = getBTTableName(i);
            String query = "SELECT COUNT(*) FROM " + tableName;
            Cursor cursor = db.rawQuery(query, null);
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            stats.put("BT" + i, count);
        }

        // Hitung total data yang belum sync
        String unsyncedQuery = "SELECT COUNT(*) FROM " + TABLE_OFFLINE_BTM + " WHERE " + COLUMN_IS_SYNCED + " = 0";
        Cursor unsyncedCursor = db.rawQuery(unsyncedQuery, null);
        unsyncedCursor.moveToFirst();
        stats.put("Unsynced", unsyncedCursor.getInt(0));
        unsyncedCursor.close();

        return stats;
    }
}