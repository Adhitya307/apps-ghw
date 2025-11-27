package com.example.app_rightpiezo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class RightPiezoDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "RightPiezoDB.db";
    private static final int DATABASE_VERSION = 3;
    private static final String TAG = "RightPiezoDB";

    // ==================== TABLE NAMES ====================
    private static final String TABLE_PENGUKURAN = "T_Pengukuran";
    private static final String TABLE_I_READING = "I_Reading_Atas";
    private static final String TABLE_T_PEMBACAAN = "T_Pembacaan";
    private static final String TABLE_B_PIEZO_METRIK = "B_Piezo_Metrik";
    private static final String TABLE_PERHITUNGAN_PSMETRIK = "Perhitungan_T_PsMetrik";

    // ==================== CREATE TABLE STATEMENTS ====================

    // Table: t_pengukuran_rightpiez
    private static final String CREATE_TABLE_PENGUKURAN =
            "CREATE TABLE " + TABLE_PENGUKURAN + " (" +
                    "id_pengukuran INTEGER PRIMARY KEY, " + // ❌ HAPUS AUTOINCREMENT
                    "tahun INTEGER, " +
                    "tanggal TEXT, " +
                    "periode TEXT, " +
                    "tma REAL, " +
                    "ch_hujan REAL, " +
                    "temp_id TEXT" +
                    ");";

    // Table: i_reading_atas
    private static final String CREATE_TABLE_I_READING =
            "CREATE TABLE " + TABLE_I_READING + " (" +
                    "id_reading_atas INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_pengukuran INTEGER, " +
                    "titik_piezometer TEXT, " +
                    "Elv_Piez REAL, " +
                    "kedalaman REAL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    // Table: t_pembacaan
    private static final String CREATE_TABLE_T_PEMBACAAN =
            "CREATE TABLE " + TABLE_T_PEMBACAAN + " (" +
                    "id_bacaan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_pengukuran INTEGER, " +
                    "lokasi TEXT, " +
                    "feet TEXT, " +
                    "inch TEXT" +
                    ");";

    // Table: b_piezo_metrik
    private static final String CREATE_TABLE_B_PIEZO_METRIK =
            "CREATE TABLE " + TABLE_B_PIEZO_METRIK + " (" +
                    "id_pengukuran INTEGER PRIMARY KEY, " +
                    "feet REAL DEFAULT 0.3048, " +
                    "inch REAL DEFAULT 0.0254, " +
                    "R_01 REAL, R_02 REAL, R_03 REAL, R_04 REAL, R_05 REAL, R_06 REAL, " +
                    "R_07 REAL, R_08 REAL, R_09 REAL, R_10 REAL, R_11 REAL, R_12 REAL, " +
                    "IPZ_01 REAL, PZ_04 REAL" +
                    ");";

    // Table: perhitungan_t_psmetrik
    private static final String CREATE_TABLE_PERHITUNGAN_PSMETRIK =
            "CREATE TABLE " + TABLE_PERHITUNGAN_PSMETRIK + " (" +
                    "id_pengukuran INTEGER PRIMARY KEY, " +
                    "R_01 REAL, R_02 REAL, R_03 REAL, R_04 REAL, R_05 REAL, R_06 REAL, " +
                    "R_07 REAL, R_08 REAL, R_09 REAL, R_10 REAL, R_11 REAL, R_12 REAL, " +
                    "IPZ_01 REAL, PZ_04 REAL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    public RightPiezoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating Right Piezometer database tables...");

        // Create all tables
        db.execSQL(CREATE_TABLE_PENGUKURAN);
        db.execSQL(CREATE_TABLE_I_READING);
        db.execSQL(CREATE_TABLE_T_PEMBACAAN);
        db.execSQL(CREATE_TABLE_B_PIEZO_METRIK);
        db.execSQL(CREATE_TABLE_PERHITUNGAN_PSMETRIK);

        Log.d(TAG, "All tables created successfully!");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Drop all tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_I_READING);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_T_PEMBACAAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_B_PIEZO_METRIK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERHITUNGAN_PSMETRIK);

        // Recreate tables
        onCreate(db);
    }

    // ==================== SYNC METHODS ====================

    public boolean syncPengukuranData(List<T_pengukuran_rightpiez> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            // Clear existing data
            db.delete(TABLE_PENGUKURAN, null, null);

            // Insert new data
            for (T_pengukuran_rightpiez data : dataList) {
                ContentValues values = new ContentValues();
                values.put("id_pengukuran", data.getId_pengukuran());
                values.put("tahun", data.getTahun());
                values.put("tanggal", data.getTanggal());
                values.put("periode", data.getPeriode());
                values.put("tma", data.getTma());
                values.put("ch_hujan", data.getCh_hujan());
                values.put("temp_id", data.getTemp_id());

                long result = db.insert(TABLE_PENGUKURAN, null, values);
                if (result == -1) {
                    Log.e(TAG, "Failed to insert pengukuran data: " + data.getId_pengukuran());
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ Synced " + dataList.size() + " pengukuran records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing pengukuran data: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean syncIReadingAtasData(List<I_reading_atas> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(TABLE_I_READING, null, null);

            for (I_reading_atas data : dataList) {
                ContentValues values = new ContentValues();
                values.put("id_reading_atas", data.getId_reading_atas());
                values.put("id_pengukuran", data.getId_pengukuran());
                values.put("titik_piezometer", data.getTitik_piezometer());
                values.put("Elv_Piez", data.getElv_Piez());
                values.put("kedalaman", data.getKedalaman());
                values.put("created_at", data.getCreated_at());
                values.put("updated_at", data.getUpdated_at());

                long result = db.insert(TABLE_I_READING, null, values);
                if (result == -1) {
                    Log.e(TAG, "Failed to insert i_reading data: " + data.getId_reading_atas());
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ Synced " + dataList.size() + " i_reading records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing i_reading data: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean syncTPembacaanData(List<T_pembacaan> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(TABLE_T_PEMBACAAN, null, null);

            for (T_pembacaan data : dataList) {
                ContentValues values = new ContentValues();
                values.put("id_bacaan", data.getId_bacaan());
                values.put("id_pengukuran", data.getId_pengukuran());
                values.put("lokasi", data.getLokasi());
                values.put("feet", data.getFeet());
                values.put("inch", data.getInch());

                long result = db.insert(TABLE_T_PEMBACAAN, null, values);
                if (result == -1) {
                    Log.e(TAG, "Failed to insert t_pembacaan data: " + data.getId_bacaan());
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ Synced " + dataList.size() + " t_pembacaan records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing t_pembacaan data: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean syncBPiezoMetrikData(List<B_piezo_metrik> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(TABLE_B_PIEZO_METRIK, null, null);

            for (B_piezo_metrik data : dataList) {
                ContentValues values = new ContentValues();
                values.put("id_pengukuran", data.getId_pengukuran());
                values.put("feet", data.getFeet());
                values.put("inch", data.getInch());

                // ✅ PERBAIKI INI: Gunakan mapping yang benar antara JSON field dan database column
                // Dari API: "R-01" -> Di DB: "R_01"
                // Dari API: "PZ-04" -> Di DB: "PZ_04"

                if (data.getR01() != null) values.put("R_01", data.getR01());
                if (data.getR02() != null) values.put("R_02", data.getR02());
                if (data.getR03() != null) values.put("R_03", data.getR03());
                if (data.getR04() != null) values.put("R_04", data.getR04());
                if (data.getR05() != null) values.put("R_05", data.getR05());
                if (data.getR06() != null) values.put("R_06", data.getR06());
                if (data.getR07() != null) values.put("R_07", data.getR07());
                if (data.getR08() != null) values.put("R_08", data.getR08());
                if (data.getR09() != null) values.put("R_09", data.getR09());
                if (data.getR10() != null) values.put("R_10", data.getR10());
                if (data.getR11() != null) values.put("R_11", data.getR11());
                if (data.getR12() != null) values.put("R_12", data.getR12());
                if (data.getIPZ01() != null) values.put("IPZ_01", data.getIPZ01());
                if (data.getPZ04() != null) values.put("PZ_04", data.getPZ04());

                long result = db.insert(TABLE_B_PIEZO_METRIK, null, values);
                if (result == -1) {
                    Log.e(TAG, "Failed to insert b_piezo_metrik data: " + data.getId_pengukuran());
                } else {
                    Log.d(TAG, "✅ Inserted B_Piezo_Metrik ID: " + data.getId_pengukuran() +
                            ", R01: " + data.getR01() + ", R04: " + data.getR04());
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ Synced " + dataList.size() + " b_piezo_metrik records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing b_piezo_metrik data: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean syncPerhitunganPsMetrikData(List<Perhitungan_t_psmetrik> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(TABLE_PERHITUNGAN_PSMETRIK, null, null);

            for (Perhitungan_t_psmetrik data : dataList) {
                ContentValues values = new ContentValues();
                values.put("id_pengukuran", data.getId_pengukuran());

                // Add all location values using direct method calls
                if (data.getR01() != null) values.put("R_01", data.getR01());
                if (data.getR02() != null) values.put("R_02", data.getR02());
                if (data.getR03() != null) values.put("R_03", data.getR03());
                if (data.getR04() != null) values.put("R_04", data.getR04());
                if (data.getR05() != null) values.put("R_05", data.getR05());
                if (data.getR06() != null) values.put("R_06", data.getR06());
                if (data.getR07() != null) values.put("R_07", data.getR07());
                if (data.getR08() != null) values.put("R_08", data.getR08());
                if (data.getR09() != null) values.put("R_09", data.getR09());
                if (data.getR10() != null) values.put("R_10", data.getR10());
                if (data.getR11() != null) values.put("R_11", data.getR11());
                if (data.getR12() != null) values.put("R_12", data.getR12());
                if (data.getIPZ01() != null) values.put("IPZ_01", data.getIPZ01());
                if (data.getPZ04() != null) values.put("PZ_04", data.getPZ04());

                values.put("created_at", data.getCreated_at());
                values.put("updated_at", data.getUpdated_at());

                long result = db.insert(TABLE_PERHITUNGAN_PSMETRIK, null, values);
                if (result == -1) {
                    Log.e(TAG, "Failed to insert perhitungan_psmetrik data: " + data.getId_pengukuran());
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ Synced " + dataList.size() + " perhitungan_psmetrik records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing perhitungan_psmetrik data: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // ==================== QUERY METHODS ====================

    public List<T_pengukuran_rightpiez> getAllPengukuran() {
        List<T_pengukuran_rightpiez> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_PENGUKURAN, null, null, null, null, null, "tanggal DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    T_pengukuran_rightpiez data = new T_pengukuran_rightpiez();
                    data.setId_pengukuran(cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran")));
                    data.setTahun(cursor.getInt(cursor.getColumnIndexOrThrow("tahun")));
                    data.setTanggal(cursor.getString(cursor.getColumnIndexOrThrow("tanggal")));
                    data.setPeriode(cursor.getString(cursor.getColumnIndexOrThrow("periode")));
                    data.setTma(cursor.getDouble(cursor.getColumnIndexOrThrow("tma")));
                    data.setCh_hujan(cursor.getDouble(cursor.getColumnIndexOrThrow("ch_hujan")));
                    data.setTemp_id(cursor.getString(cursor.getColumnIndexOrThrow("temp_id")));

                    dataList.add(data);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting pengukuran data: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return dataList;
    }

    public List<I_reading_atas> getIReadingByPengukuran(int idPengukuran) {
        List<I_reading_atas> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_I_READING, null, "id_pengukuran = ?",
                    new String[]{String.valueOf(idPengukuran)}, null, null, "titik_piezometer ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    I_reading_atas data = new I_reading_atas();
                    data.setId_reading_atas(cursor.getInt(cursor.getColumnIndexOrThrow("id_reading_atas")));
                    data.setId_pengukuran(cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran")));
                    data.setTitik_piezometer(cursor.getString(cursor.getColumnIndexOrThrow("titik_piezometer")));
                    data.setElv_Piez(cursor.getDouble(cursor.getColumnIndexOrThrow("Elv_Piez")));
                    data.setKedalaman(cursor.getDouble(cursor.getColumnIndexOrThrow("kedalaman")));
                    data.setCreated_at(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                    data.setUpdated_at(cursor.getString(cursor.getColumnIndexOrThrow("updated_at")));

                    dataList.add(data);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting i_reading data: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return dataList;
    }

    public List<T_pembacaan> getTPembacaanByPengukuran(int idPengukuran) {
        List<T_pembacaan> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_T_PEMBACAAN, null, "id_pengukuran = ?",
                    new String[]{String.valueOf(idPengukuran)}, null, null, "lokasi ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    T_pembacaan data = new T_pembacaan();
                    data.setId_bacaan(cursor.getInt(cursor.getColumnIndexOrThrow("id_bacaan")));
                    data.setId_pengukuran(cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran")));
                    data.setLokasi(cursor.getString(cursor.getColumnIndexOrThrow("lokasi")));
                    data.setFeet(cursor.getString(cursor.getColumnIndexOrThrow("feet")));
                    data.setInch(cursor.getString(cursor.getColumnIndexOrThrow("inch")));

                    dataList.add(data);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting t_pembacaan data: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return dataList;
    }

    public B_piezo_metrik getBPiezoMetrikByPengukuran(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_B_PIEZO_METRIK, null, "id_pengukuran = ?",
                    new String[]{String.valueOf(idPengukuran)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                B_piezo_metrik data = new B_piezo_metrik();
                data.setId_pengukuran(cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran")));
                data.setFeet(cursor.getDouble(cursor.getColumnIndexOrThrow("feet")));
                data.setInch(cursor.getDouble(cursor.getColumnIndexOrThrow("inch")));

                data.setR01(getDoubleOrNull(cursor, "R_01"));
                data.setR02(getDoubleOrNull(cursor, "R_02"));
                data.setR03(getDoubleOrNull(cursor, "R_03"));
                data.setR04(getDoubleOrNull(cursor, "R_04"));
                data.setR05(getDoubleOrNull(cursor, "R_05"));
                data.setR06(getDoubleOrNull(cursor, "R_06"));
                data.setR07(getDoubleOrNull(cursor, "R_07"));
                data.setR08(getDoubleOrNull(cursor, "R_08"));
                data.setR09(getDoubleOrNull(cursor, "R_09"));
                data.setR10(getDoubleOrNull(cursor, "R_10"));
                data.setR11(getDoubleOrNull(cursor, "R_11"));
                data.setR12(getDoubleOrNull(cursor, "R_12"));
                data.setIPZ01(getDoubleOrNull(cursor, "IPZ_01"));
                data.setPZ04(getDoubleOrNull(cursor, "PZ_04"));

                return data;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting b_piezo_metrik data: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return null;
    }

    public Perhitungan_t_psmetrik getPerhitunganPsMetrikByPengukuran(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_PERHITUNGAN_PSMETRIK, null, "id_pengukuran = ?",
                    new String[]{String.valueOf(idPengukuran)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                Perhitungan_t_psmetrik data = new Perhitungan_t_psmetrik();
                data.setId_pengukuran(cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran")));

                data.setR01(getDoubleOrNull(cursor, "R_01"));
                data.setR02(getDoubleOrNull(cursor, "R_02"));
                data.setR03(getDoubleOrNull(cursor, "R_03"));
                data.setR04(getDoubleOrNull(cursor, "R_04"));
                data.setR05(getDoubleOrNull(cursor, "R_05"));
                data.setR06(getDoubleOrNull(cursor, "R_06"));
                data.setR07(getDoubleOrNull(cursor, "R_07"));
                data.setR08(getDoubleOrNull(cursor, "R_08"));
                data.setR09(getDoubleOrNull(cursor, "R_09"));
                data.setR10(getDoubleOrNull(cursor, "R_10"));
                data.setR11(getDoubleOrNull(cursor, "R_11"));
                data.setR12(getDoubleOrNull(cursor, "R_12"));
                data.setIPZ01(getDoubleOrNull(cursor, "IPZ_01"));
                data.setPZ04(getDoubleOrNull(cursor, "PZ_04"));

                data.setCreated_at(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                data.setUpdated_at(cursor.getString(cursor.getColumnIndexOrThrow("updated_at")));

                return data;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting perhitungan_psmetrik data: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return null;
    }

    // ==================== UTILITY METHODS ====================

    private Double getDoubleOrNull(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
            return cursor.getDouble(columnIndex);
        }
        return null;
    }

    // ==================== DATA COUNT METHODS ====================

    public int getPengukuranCount() {
        return getTableCount(TABLE_PENGUKURAN);
    }

    public int getIReadingCount() {
        return getTableCount(TABLE_I_READING);
    }

    public int getTPembacaanCount() {
        return getTableCount(TABLE_T_PEMBACAAN);
    }

    public int getBPiezoMetrikCount() {
        return getTableCount(TABLE_B_PIEZO_METRIK);
    }

    public int getPerhitunganPsMetrikCount() {
        return getTableCount(TABLE_PERHITUNGAN_PSMETRIK);
    }

    private int getTableCount(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting count for " + tableName + ": " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return 0;
    }

    // ==================== CLEANUP METHODS ====================

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(TABLE_PENGUKURAN, null, null);
            db.delete(TABLE_I_READING, null, null);
            db.delete(TABLE_T_PEMBACAAN, null, null);
            db.delete(TABLE_B_PIEZO_METRIK, null, null);
            db.delete(TABLE_PERHITUNGAN_PSMETRIK, null, null);

            db.setTransactionSuccessful();
            Log.d(TAG, "All data cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing data: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void close() {
        super.close();
    }
}