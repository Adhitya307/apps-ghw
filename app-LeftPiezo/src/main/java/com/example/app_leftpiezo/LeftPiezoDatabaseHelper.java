package com.example.app_leftpiezo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

public class LeftPiezoDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "LeftPiezoDatabaseHelper";

    // Database Info
    private static final String DATABASE_NAME = "LeftPiezoDatabase.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_PENGUKURAN = "t_pengukuran_leftpiez";
    private static final String TABLE_IREADING_A = "i_reading_A_all";
    private static final String TABLE_IREADING_B = "i_reading_B_all";
    private static final String TABLE_BPIEZO_METRIK = "b_piezo_metrik";
    private static final String TABLE_PERHITUNGAN = "perhitungan_left_piez";
    private static final String TABLE_PEMBACAAN = "t_pembacaan_left_piez"; // TAMBAHAN TABEL BARU

    // Common Column Names
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_UPDATED_AT = "updated_at";

    // T_Pengukuran Table Columns
    private static final String KEY_ID_PENGUKURAN = "id_pengukuran";
    private static final String KEY_TAHUN = "tahun";
    private static final String KEY_PERIODE = "periode";
    private static final String KEY_TANGGAL = "tanggal";
    private static final String KEY_DMA = "dma";
    private static final String KEY_TEMP_ID = "temp_id";

    // I_Reading Tables Columns
    private static final String KEY_ID_READING_A = "id_reading_A";
    private static final String KEY_ID_READING_B = "id_reading_B";
    private static final String KEY_TITIK_PIEZOMETER = "titik_piezometer";
    private static final String KEY_ELV_PIEZ = "Elv_Piez";

    // T_Pembacaan Table Columns (TAMBAHAN)
    private static final String KEY_ID_PEMBACAAN = "id_pembacaan";
    private static final String KEY_TIPE_PIEZOMETER = "tipe_piezometer";
    private static final String KEY_FEET = "feet";
    private static final String KEY_INCH = "inch";

    // B_Piezo_Metrik Table Columns
    private static final String KEY_ID_BACAAN_METRIK = "id_bacaan_metrik";
    private static final String KEY_M_FEET = "M_feet";
    private static final String KEY_M_INCH = "M_inch";
    private static final String KEY_L_01 = "l_01";
    private static final String KEY_L_02 = "l_02";
    private static final String KEY_L_03 = "l_03";
    private static final String KEY_L_04 = "l_04";
    private static final String KEY_L_05 = "l_05";
    private static final String KEY_L_06 = "l_06";
    private static final String KEY_L_07 = "l_07";
    private static final String KEY_L_08 = "l_08";
    private static final String KEY_L_09 = "l_09";
    private static final String KEY_L_10 = "l_10";
    private static final String KEY_SPZ_02 = "spz_02";

    // Perhitungan Table Columns
    private static final String KEY_ID_PERHITUNGAN = "id_perhitungan";
    private static final String KEY_ELV_PIEZ_PERHITUNGAN = "elv_piez";
    private static final String KEY_KEDALAMAN = "kedalaman";
    private static final String KEY_RECORD_MAX = "record_max";
    private static final String KEY_RECORD_MIN = "record_min";
    private static final String KEY_KOORDINAT_X = "koordinat_x";
    private static final String KEY_KOORDINAT_Y = "koordinat_y";
    private static final String KEY_T_PSMETRIK = "t_psmetrik";

    // Create Table Statements
    private static final String CREATE_TABLE_PENGUKURAN =
            "CREATE TABLE " + TABLE_PENGUKURAN + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_ID_PENGUKURAN + " INTEGER UNIQUE,"
                    + KEY_TAHUN + " TEXT,"
                    + KEY_PERIODE + " TEXT,"
                    + KEY_TANGGAL + " TEXT,"
                    + KEY_DMA + " TEXT,"
                    + KEY_TEMP_ID + " TEXT,"
                    + KEY_CREATED_AT + " TEXT,"
                    + KEY_UPDATED_AT + " TEXT"
                    + ")";

    private static final String CREATE_TABLE_IREADING_A =
            "CREATE TABLE " + TABLE_IREADING_A + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_ID_READING_A + " INTEGER UNIQUE,"
                    + KEY_ID_PENGUKURAN + " INTEGER,"
                    + KEY_TITIK_PIEZOMETER + " TEXT,"
                    + KEY_ELV_PIEZ + " REAL,"
                    + KEY_CREATED_AT + " TEXT,"
                    + KEY_UPDATED_AT + " TEXT"
                    + ")";

    private static final String CREATE_TABLE_IREADING_B =
            "CREATE TABLE " + TABLE_IREADING_B + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_ID_READING_B + " INTEGER UNIQUE,"
                    + KEY_ID_PENGUKURAN + " INTEGER,"
                    + KEY_TITIK_PIEZOMETER + " TEXT,"
                    + KEY_ELV_PIEZ + " REAL,"
                    + KEY_CREATED_AT + " TEXT,"
                    + KEY_UPDATED_AT + " TEXT"
                    + ")";

    // TAMBAHAN: Create Table untuk T_Pembacaan
    private static final String CREATE_TABLE_PEMBACAAN =
            "CREATE TABLE " + TABLE_PEMBACAAN + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_ID_PEMBACAAN + " INTEGER UNIQUE,"
                    + KEY_ID_PENGUKURAN + " INTEGER,"
                    + KEY_TIPE_PIEZOMETER + " TEXT,"
                    + KEY_FEET + " TEXT,"
                    + KEY_INCH + " TEXT,"
                    + KEY_CREATED_AT + " TEXT,"
                    + KEY_UPDATED_AT + " TEXT"
                    + ")";

    private static final String CREATE_TABLE_BPIEZO_METRIK =
            "CREATE TABLE " + TABLE_BPIEZO_METRIK + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_ID_BACAAN_METRIK + " INTEGER UNIQUE,"
                    + KEY_ID_PENGUKURAN + " INTEGER,"
                    + KEY_M_FEET + " TEXT,"
                    + KEY_M_INCH + " TEXT,"
                    + KEY_L_01 + " REAL,"
                    + KEY_L_02 + " REAL,"
                    + KEY_L_03 + " REAL,"
                    + KEY_L_04 + " REAL,"
                    + KEY_L_05 + " REAL,"
                    + KEY_L_06 + " REAL,"
                    + KEY_L_07 + " REAL,"
                    + KEY_L_08 + " REAL,"
                    + KEY_L_09 + " REAL,"
                    + KEY_L_10 + " REAL,"
                    + KEY_SPZ_02 + " REAL,"
                    + KEY_CREATED_AT + " TEXT,"
                    + KEY_UPDATED_AT + " TEXT"
                    + ")";

    private static final String CREATE_TABLE_PERHITUNGAN =
            "CREATE TABLE " + TABLE_PERHITUNGAN + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_ID_PERHITUNGAN + " INTEGER UNIQUE,"
                    + KEY_ID_PENGUKURAN + " INTEGER,"
                    + KEY_TIPE_PIEZOMETER + " TEXT,"
                    + KEY_ELV_PIEZ_PERHITUNGAN + " REAL,"
                    + KEY_KEDALAMAN + " REAL,"
                    + KEY_RECORD_MAX + " REAL,"
                    + KEY_RECORD_MIN + " REAL,"
                    + KEY_KOORDINAT_X + " REAL,"
                    + KEY_KOORDINAT_Y + " REAL,"
                    + KEY_T_PSMETRIK + " REAL,"
                    + KEY_CREATED_AT + " TEXT,"
                    + KEY_UPDATED_AT + " TEXT"
                    + ")";

    public LeftPiezoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating Left Piezo database tables...");

        // Create all tables
        db.execSQL(CREATE_TABLE_PENGUKURAN);
        db.execSQL(CREATE_TABLE_IREADING_A);
        db.execSQL(CREATE_TABLE_IREADING_B);
        db.execSQL(CREATE_TABLE_PEMBACAAN); // TAMBAHAN
        db.execSQL(CREATE_TABLE_BPIEZO_METRIK);
        db.execSQL(CREATE_TABLE_PERHITUNGAN);

        Log.d(TAG, "All Left Piezo database tables created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading Left Piezo database from version " + oldVersion + " to " + newVersion);

        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENGUKURAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IREADING_A);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IREADING_B);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PEMBACAAN); // TAMBAHAN
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BPIEZO_METRIK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERHITUNGAN);

        // Create tables again
        onCreate(db);
    }

    // ==================== SYNC METHODS ====================

    /**
     * Sync T_Pengukuran data
     */
    public boolean syncPengukuranData(List<T_pengukuran_leftpiez> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (T_pengukuran_leftpiez data : dataList) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID_PENGUKURAN, data.getId_pengukuran());
                values.put(KEY_TAHUN, data.getTahun());
                values.put(KEY_PERIODE, data.getPeriode());
                values.put(KEY_TANGGAL, data.getTanggal());
                values.put(KEY_DMA, data.getDma());
                values.put(KEY_TEMP_ID, data.getTemp_id());
                values.put(KEY_CREATED_AT, data.getCreated_at());
                values.put(KEY_UPDATED_AT, data.getUpdated_at());

                // Insert or replace
                db.insertWithOnConflict(TABLE_PENGUKURAN, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ T_Pengukuran sync completed: " + dataList.size() + " records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing T_Pengukuran: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Sync I_Reading_A data
     */
    public boolean syncIReadingAData(List<I_reading_a> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (I_reading_a data : dataList) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID_READING_A, data.getId_reading_A());
                values.put(KEY_ID_PENGUKURAN, data.getId_pengukuran());
                values.put(KEY_TITIK_PIEZOMETER, data.getTitik_piezometer());
                values.put(KEY_ELV_PIEZ, data.getElv_Piez());
                values.put(KEY_CREATED_AT, data.getCreated_at());
                values.put(KEY_UPDATED_AT, data.getUpdated_at());

                db.insertWithOnConflict(TABLE_IREADING_A, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ I_Reading_A sync completed: " + dataList.size() + " records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing I_Reading_A: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Sync I_Reading_B data
     */
    public boolean syncIReadingBData(List<I_reading_b> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (I_reading_b data : dataList) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID_READING_B, data.getId_reading_B());
                values.put(KEY_ID_PENGUKURAN, data.getId_pengukuran());
                values.put(KEY_TITIK_PIEZOMETER, data.getTitik_piezometer());
                values.put(KEY_ELV_PIEZ, data.getElv_Piez());
                values.put(KEY_CREATED_AT, data.getCreated_at());
                values.put(KEY_UPDATED_AT, data.getUpdated_at());

                db.insertWithOnConflict(TABLE_IREADING_B, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ I_Reading_B sync completed: " + dataList.size() + " records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing I_Reading_B: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Sync T_Pembacaan data
     */
    public boolean syncTPembacaanData(List<TPembacaanLeftPiez> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (TPembacaanLeftPiez data : dataList) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID_PEMBACAAN, data.getId_pembacaan());
                values.put(KEY_ID_PENGUKURAN, data.getId_pengukuran());
                values.put(KEY_TIPE_PIEZOMETER, data.getTipe_piezometer());
                values.put(KEY_FEET, data.getFeet());
                values.put(KEY_INCH, data.getInch());
                values.put(KEY_CREATED_AT, data.getCreated_at());
                values.put(KEY_UPDATED_AT, data.getUpdated_at());

                db.insertWithOnConflict(TABLE_PEMBACAAN, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ T_Pembacaan sync completed: " + dataList.size() + " records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing T_Pembacaan: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Sync B_Piezo_Metrik data
     */
    public boolean syncBPiezoMetrikData(List<B_piezo_metrik> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (B_piezo_metrik data : dataList) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID_BACAAN_METRIK, data.getId_bacaan_metrik());
                values.put(KEY_ID_PENGUKURAN, data.getId_pengukuran());
                values.put(KEY_M_FEET, data.getM_feet());
                values.put(KEY_M_INCH, data.getM_inch());
                values.put(KEY_L_01, data.getL_01());
                values.put(KEY_L_02, data.getL_02());
                values.put(KEY_L_03, data.getL_03());
                values.put(KEY_L_04, data.getL_04());
                values.put(KEY_L_05, data.getL_05());
                values.put(KEY_L_06, data.getL_06());
                values.put(KEY_L_07, data.getL_07());
                values.put(KEY_L_08, data.getL_08());
                values.put(KEY_L_09, data.getL_09());
                values.put(KEY_L_10, data.getL_10());
                values.put(KEY_SPZ_02, data.getSpz_02());
                values.put(KEY_CREATED_AT, data.getCreated_at());
                values.put(KEY_UPDATED_AT, data.getUpdated_at());

                db.insertWithOnConflict(TABLE_BPIEZO_METRIK, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ B_Piezo_Metrik sync completed: " + dataList.size() + " records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing B_Piezo_Metrik: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Sync Perhitungan data
     */
    public boolean syncPerhitunganLeftPiezData(List<Perhitungan_left_piez> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            for (Perhitungan_left_piez data : dataList) {
                ContentValues values = new ContentValues();
                values.put(KEY_ID_PERHITUNGAN, data.getId_perhitungan());
                values.put(KEY_ID_PENGUKURAN, data.getId_pengukuran());
                values.put(KEY_TIPE_PIEZOMETER, data.getTipe_piezometer());
                values.put(KEY_ELV_PIEZ_PERHITUNGAN, data.getElv_piez());
                values.put(KEY_KEDALAMAN, data.getKedalaman());
                values.put(KEY_RECORD_MAX, data.getRecord_max());
                values.put(KEY_RECORD_MIN, data.getRecord_min());
                values.put(KEY_KOORDINAT_X, data.getKoordinat_x());
                values.put(KEY_KOORDINAT_Y, data.getKoordinat_y());
                values.put(KEY_T_PSMETRIK, data.getT_psmetrik());
                values.put(KEY_CREATED_AT, data.getCreated_at());
                values.put(KEY_UPDATED_AT, data.getUpdated_at());

                db.insertWithOnConflict(TABLE_PERHITUNGAN, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "✅ Perhitungan_Left_Piez sync completed: " + dataList.size() + " records");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error syncing Perhitungan_Left_Piez: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // ==================== COUNT METHODS ====================

    /**
     * Get count of records in each table for statistics
     */
    public int getPengukuranCount() {
        return getTableCount(TABLE_PENGUKURAN);
    }

    public int getIReadingACount() {
        return getTableCount(TABLE_IREADING_A);
    }

    public int getIReadingBCount() {
        return getTableCount(TABLE_IREADING_B);
    }

    public int getTPembacaanCount() {
        return getTableCount(TABLE_PEMBACAAN);
    }

    public int getBPiezoMetrikCount() {
        return getTableCount(TABLE_BPIEZO_METRIK);
    }

    public int getPerhitunganLeftPiezCount() {
        return getTableCount(TABLE_PERHITUNGAN);
    }

    private int getTableCount(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String countQuery = "SELECT COUNT(*) FROM " + tableName;
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }

        return count;
    }

    // ==================== GET DATA METHODS ====================

    /**
     * Get all pengukuran data
     */
    public Cursor getAllPengukuran() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PENGUKURAN,
                null, null, null, null, null,
                KEY_ID_PENGUKURAN + " DESC");
    }

    /**
     * Get I_Reading_A data by pengukuran ID
     */
    public Cursor getIReadingAByPengukuran(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_IREADING_A,
                null,
                KEY_ID_PENGUKURAN + " = ?",
                new String[]{String.valueOf(idPengukuran)},
                null, null,
                KEY_TITIK_PIEZOMETER + " ASC");
    }

    /**
     * Get I_Reading_B data by pengukuran ID
     */
    public Cursor getIReadingBByPengukuran(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_IREADING_B,
                null,
                KEY_ID_PENGUKURAN + " = ?",
                new String[]{String.valueOf(idPengukuran)},
                null, null,
                KEY_TITIK_PIEZOMETER + " ASC");
    }

    /**
     * Get T_Pembacaan data by pengukuran ID
     */
    public Cursor getTPembacaanByPengukuran(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PEMBACAAN,
                null,
                KEY_ID_PENGUKURAN + " = ?",
                new String[]{String.valueOf(idPengukuran)},
                null, null,
                KEY_TIPE_PIEZOMETER + " ASC");
    }

    /**
     * Get B_Piezo_Metrik data by pengukuran ID
     */
    public Cursor getBPiezoMetrikByPengukuran(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_BPIEZO_METRIK,
                null,
                KEY_ID_PENGUKURAN + " = ?",
                new String[]{String.valueOf(idPengukuran)},
                null, null, null);
    }

    /**
     * Get Perhitungan data by pengukuran ID and type
     */
    public Cursor getPerhitunganByPengukuranAndType(int idPengukuran, String tipePiezometer) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PERHITUNGAN,
                null,
                KEY_ID_PENGUKURAN + " = ? AND " + KEY_TIPE_PIEZOMETER + " = ?",
                new String[]{String.valueOf(idPengukuran), tipePiezometer},
                null, null, null);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear all data from all tables
     */
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            db.delete(TABLE_PENGUKURAN, null, null);
            db.delete(TABLE_IREADING_A, null, null);
            db.delete(TABLE_IREADING_B, null, null);
            db.delete(TABLE_PEMBACAAN, null, null);
            db.delete(TABLE_BPIEZO_METRIK, null, null);
            db.delete(TABLE_PERHITUNGAN, null, null);

            db.setTransactionSuccessful();
            Log.d(TAG, "All Left Piezo data cleared successfully");
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Get database info
     */
    public String getDatabaseInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Left Piezo Database Info:\n");
        info.append("Pengukuran: ").append(getPengukuranCount()).append(" records\n");
        info.append("I_Reading_A: ").append(getIReadingACount()).append(" records\n");
        info.append("I_Reading_B: ").append(getIReadingBCount()).append(" records\n");
        info.append("T_Pembacaan: ").append(getTPembacaanCount()).append(" records\n");
        info.append("B_Piezo_Metrik: ").append(getBPiezoMetrikCount()).append(" records\n");
        info.append("Perhitungan: ").append(getPerhitunganLeftPiezCount()).append(" records\n");

        return info.toString();
    }

    @Override
    public synchronized void close() {
        super.close();
        Log.d(TAG, "LeftPiezoDatabaseHelper closed");
    }
}