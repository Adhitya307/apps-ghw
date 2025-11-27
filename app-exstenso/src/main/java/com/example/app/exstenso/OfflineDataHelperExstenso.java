package com.example.app.exstenso;

import android.annotation.SuppressLint;
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

public class OfflineDataHelperExstenso extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "exstenso_offline.db";
    private static final int DATABASE_VERSION = 2;

    // Tabel untuk data pengukuran
    private static final String TABLE_PENGUKURAN = "pengukuran_offline";
    private static final String TABLE_DATA = "data_offline";

    // Kolom umum
    private static final String KEY_ID = "id";
    private static final String KEY_TEMP_ID = "temp_id";
    private static final String KEY_JSON_DATA = "json";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_SYNC_STATUS = "sync_status";

    // Kolom khusus untuk query data
    private static final String KEY_PENGUKURAN_ID = "pengukuran_id";
    private static final String KEY_EX_TYPE = "ex_type";

    // SQL untuk membuat tabel pengukuran
    private static final String CREATE_TABLE_PENGUKURAN = "CREATE TABLE " + TABLE_PENGUKURAN + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TEMP_ID + " TEXT UNIQUE,"
            + KEY_JSON_DATA + " TEXT,"
            + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + KEY_SYNC_STATUS + " INTEGER DEFAULT 0" + ")";

    // SQL untuk membuat tabel data
    private static final String CREATE_TABLE_DATA = "CREATE TABLE " + TABLE_DATA + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TEMP_ID + " TEXT UNIQUE,"
            + KEY_PENGUKURAN_ID + " INTEGER,"
            + KEY_EX_TYPE + " TEXT,"
            + KEY_JSON_DATA + " TEXT,"
            + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + KEY_SYNC_STATUS + " INTEGER DEFAULT 0" + ")";

    public OfflineDataHelperExstenso(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PENGUKURAN);
        db.execSQL(CREATE_TABLE_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);
            onCreate(db);
        }
    }

    // ==================== OPERASI CRUD UNTUK PENGUKURAN ====================

    public boolean insertDataExstenso(String tableType, String tempId, String jsonData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TEMP_ID, tempId);
        values.put(KEY_JSON_DATA, jsonData);
        values.put(KEY_SYNC_STATUS, 0);

        long result;
        if (tableType.equals("pengukuran")) {
            result = db.insert(TABLE_PENGUKURAN, null, values);
        } else {
            result = db.insert(TABLE_DATA, null, values);
        }

        db.close();
        return result != -1;
    }

    public boolean insertDataExstensoWithType(String tableType, String tempId, String jsonData,
                                              int pengukuranId, String exType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TEMP_ID, tempId);
        values.put(KEY_JSON_DATA, jsonData);
        values.put(KEY_PENGUKURAN_ID, pengukuranId);
        values.put(KEY_EX_TYPE, exType);
        values.put(KEY_SYNC_STATUS, 0);

        long result = db.insert(TABLE_DATA, null, values);
        db.close();
        return result != -1;
    }

    @SuppressLint("Range")
    public List<Map<String, String>> getUnsyncedDataExstenso(String tableType) {
        List<Map<String, String>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String tableName = tableType.equals("pengukuran") ? TABLE_PENGUKURAN : TABLE_DATA;
        String query = "SELECT * FROM " + tableName + " WHERE " + KEY_SYNC_STATUS + " = 0 ORDER BY " + KEY_CREATED_AT + " ASC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                data.put("temp_id", cursor.getString(cursor.getColumnIndex(KEY_TEMP_ID)));
                data.put("json", cursor.getString(cursor.getColumnIndex(KEY_JSON_DATA)));

                if (tableType.equals("data")) {
                    data.put("pengukuran_id", cursor.getString(cursor.getColumnIndex(KEY_PENGUKURAN_ID)));
                    data.put("ex_type", cursor.getString(cursor.getColumnIndex(KEY_EX_TYPE)));
                }

                dataList.add(data);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataList;
    }

    public Map<Integer, List<Map<String, String>>> getUnsyncedDataGroupedByPengukuran() {
        Map<Integer, List<Map<String, String>>> groupedData = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_DATA + " WHERE " + KEY_SYNC_STATUS + " = 0 ORDER BY " + KEY_CREATED_AT + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                String tempId = cursor.getString(cursor.getColumnIndex(KEY_TEMP_ID));
                String jsonData = cursor.getString(cursor.getColumnIndex(KEY_JSON_DATA));
                int pengukuranId = cursor.getInt(cursor.getColumnIndex(KEY_PENGUKURAN_ID));
                String exType = cursor.getString(cursor.getColumnIndex(KEY_EX_TYPE));

                data.put("temp_id", tempId);
                data.put("json", jsonData);
                data.put("ex_type", exType);
                data.put("pengukuran_id", String.valueOf(pengukuranId));

                if (!groupedData.containsKey(pengukuranId)) {
                    groupedData.put(pengukuranId, new ArrayList<Map<String, String>>());
                }
                groupedData.get(pengukuranId).add(data);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return groupedData;
    }

    public void deleteByTempIdExstenso(String tableType, String tempId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tableName = tableType.equals("pengukuran") ? TABLE_PENGUKURAN : TABLE_DATA;
        db.delete(tableName, KEY_TEMP_ID + " = ?", new String[]{tempId});
        db.close();
    }

    public boolean hasUnsyncedDataExstenso() {
        SQLiteDatabase db = this.getReadableDatabase();

        String queryPengukuran = "SELECT COUNT(*) FROM " + TABLE_PENGUKURAN + " WHERE " + KEY_SYNC_STATUS + " = 0";
        String queryData = "SELECT COUNT(*) FROM " + TABLE_DATA + " WHERE " + KEY_SYNC_STATUS + " = 0";

        Cursor cursor1 = db.rawQuery(queryPengukuran, null);
        Cursor cursor2 = db.rawQuery(queryData, null);

        int count1 = 0;
        int count2 = 0;

        if (cursor1.moveToFirst()) {
            count1 = cursor1.getInt(0);
        }
        if (cursor2.moveToFirst()) {
            count2 = cursor2.getInt(0);
        }

        cursor1.close();
        cursor2.close();
        db.close();

        return (count1 + count2) > 0;
    }

    public int getOfflineDataCountExstenso() {
        SQLiteDatabase db = this.getReadableDatabase();

        String queryPengukuran = "SELECT COUNT(*) FROM " + TABLE_PENGUKURAN + " WHERE " + KEY_SYNC_STATUS + " = 0";
        String queryData = "SELECT COUNT(*) FROM " + TABLE_DATA + " WHERE " + KEY_SYNC_STATUS + " = 0";

        Cursor cursor1 = db.rawQuery(queryPengukuran, null);
        Cursor cursor2 = db.rawQuery(queryData, null);

        int count1 = 0;
        int count2 = 0;

        if (cursor1.moveToFirst()) {
            count1 = cursor1.getInt(0);
        }
        if (cursor2.moveToFirst()) {
            count2 = cursor2.getInt(0);
        }

        cursor1.close();
        cursor2.close();
        db.close();

        return count1 + count2;
    }

    // ==================== OPERASI KHUSUS UNTUK DATA EXSTENSO ====================

    public List<Map<String, String>> getPengukuranMasterExstenso() {
        List<Map<String, String>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_PENGUKURAN + " ORDER BY " + KEY_CREATED_AT + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                data.put("temp_id", cursor.getString(cursor.getColumnIndex(KEY_TEMP_ID)));
                data.put("json", cursor.getString(cursor.getColumnIndex(KEY_JSON_DATA)));
                try {
                    JSONObject json = new JSONObject(cursor.getString(cursor.getColumnIndex(KEY_JSON_DATA)));
                    if (json.has("tanggal")) {
                        data.put("tanggal", json.getString("tanggal"));
                    }
                    if (json.has("id_pengukuran")) {
                        data.put("id_pengukuran", json.getString("id_pengukuran"));
                    } else if (json.has("temp_id")) {
                        data.put("id_pengukuran", json.getString("temp_id"));
                    }
                } catch (Exception e) {
                    Log.e("GET_PENGUKURAN_MASTER", "Error parsing JSON: " + e.getMessage());
                }
                dataList.add(data);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataList;
    }

    public Map<String, String> getExstensoData(int pengukuranId, String exType) {
        Map<String, String> data = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_DATA + " WHERE " + KEY_PENGUKURAN_ID + " = ? AND " + KEY_EX_TYPE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(pengukuranId), exType});

        if (cursor.moveToFirst()) {
            try {
                String jsonStr = cursor.getString(cursor.getColumnIndex(KEY_JSON_DATA));
                JSONObject json = new JSONObject(jsonStr);

                if (json.has("dma")) data.put("dma", json.getString("dma"));
                if (json.has("pembacaan_10")) data.put("pembacaan_10", json.getString("pembacaan_10"));
                if (json.has("pembacaan_20")) data.put("pembacaan_20", json.getString("pembacaan_20"));
                if (json.has("pembacaan_30")) data.put("pembacaan_30", json.getString("pembacaan_30"));

            } catch (Exception e) {
                Log.e("GET_EXSTENSO_DATA", "Error parsing JSON: " + e.getMessage());
            }
        }

        cursor.close();
        db.close();
        return data;
    }

    public boolean updateDataWithIds(String tempId, int pengukuranId, String exType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PENGUKURAN_ID, pengukuranId);
        values.put(KEY_EX_TYPE, exType);

        int result = db.update(TABLE_DATA, values, KEY_TEMP_ID + " = ?", new String[]{tempId});
        db.close();
        return result > 0;
    }

    @SuppressLint("Range")
    public List<String> getExTypesForPengukuran(int pengukuranId) {
        List<String> exTypes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT DISTINCT " + KEY_EX_TYPE + " FROM " + TABLE_DATA + " WHERE " + KEY_PENGUKURAN_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(pengukuranId)});

        if (cursor.moveToFirst()) {
            do {
                exTypes.add(cursor.getString(cursor.getColumnIndex(KEY_EX_TYPE)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return exTypes;
    }

    public void clearAllOfflineData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PENGUKURAN, null, null);
        db.delete(TABLE_DATA, null, null);
        db.close();
    }

    @SuppressLint("Range")
    public List<Map<String, String>> getDataForPengukuran(int pengukuranId) {
        List<Map<String, String>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_DATA + " WHERE " + KEY_PENGUKURAN_ID + " = ? ORDER BY " + KEY_CREATED_AT + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(pengukuranId)});

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> data = new HashMap<>();
                data.put("temp_id", cursor.getString(cursor.getColumnIndex(KEY_TEMP_ID)));
                data.put("json", cursor.getString(cursor.getColumnIndex(KEY_JSON_DATA)));
                data.put("ex_type", cursor.getString(cursor.getColumnIndex(KEY_EX_TYPE)));
                data.put("pengukuran_id", String.valueOf(pengukuranId));
                dataList.add(data);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataList;
    }
}