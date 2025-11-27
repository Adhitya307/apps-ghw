package com.example.app.exstenso;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;



import java.util.ArrayList;
import java.util.List;

public class ExstensoDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ExstensoDBHelper";
    public static final String DATABASE_NAME = "exstenso.db";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    private static final String TABLE_PENGUKURAN = "t_pengukuran_eks";
    private static final String TABLE_PEMBACAAN_EX1 = "t_pembacaan_ex1";
    private static final String TABLE_PEMBACAAN_EX2 = "t_pembacaan_ex2";
    private static final String TABLE_PEMBACAAN_EX3 = "t_pembacaan_ex3";
    private static final String TABLE_PEMBACAAN_EX4 = "t_pembacaan_ex4";
    private static final String TABLE_DEFORMASI_EX1 = "p_deformasi_ex1";
    private static final String TABLE_DEFORMASI_EX2 = "p_deformasi_ex2";
    private static final String TABLE_DEFORMASI_EX3 = "p_deformasi_ex3";
    private static final String TABLE_DEFORMASI_EX4 = "p_deformasi_ex4";
    private static final String TABLE_READINGS_EX1 = "i_readings_ex1";
    private static final String TABLE_READINGS_EX2 = "i_readings_ex2";
    private static final String TABLE_READINGS_EX3 = "i_readings_ex3";
    private static final String TABLE_READINGS_EX4 = "i_readings_ex4";

    public ExstensoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createPengukuranTable(db);
        createPembacaanTables(db);
        createDeformasiTables(db);
        createReadingsTables(db);
        Log.d(TAG, "✅ Database created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropAllTables(db);
        onCreate(db);
    }

    // ==================== TABLE CREATION METHODS ====================

    private void createPengukuranTable(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_PENGUKURAN + " (" +
                "id_pengukuran INTEGER PRIMARY KEY, " +
                "tahun TEXT, " +
                "periode TEXT, " +
                "tanggal TEXT, " +
                "dma TEXT, " +
                "temp_id TEXT, " +
                "created_at TEXT, " +
                "updated_at TEXT)";
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "✅ Created table: " + TABLE_PENGUKURAN);
    }

    private void createPembacaanTables(SQLiteDatabase db) {
        String[] tables = {TABLE_PEMBACAAN_EX1, TABLE_PEMBACAAN_EX2, TABLE_PEMBACAAN_EX3, TABLE_PEMBACAAN_EX4};

        for (String table : tables) {
            String CREATE_TABLE = "CREATE TABLE " + table + " (" +
                    "id_pembacaan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_pengukuran INTEGER, " +
                    "pembacaan_10 REAL, " +
                    "pembacaan_20 REAL, " +
                    "pembacaan_30 REAL, " +
                    "FOREIGN KEY (id_pengukuran) REFERENCES " + TABLE_PENGUKURAN + " (id_pengukuran))";
            db.execSQL(CREATE_TABLE);
            Log.d(TAG, "✅ Created table: " + table);
        }
    }

    private void createDeformasiTables(SQLiteDatabase db) {
        String[] tables = {TABLE_DEFORMASI_EX1, TABLE_DEFORMASI_EX2, TABLE_DEFORMASI_EX3, TABLE_DEFORMASI_EX4};

        for (String table : tables) {
            String CREATE_TABLE = "CREATE TABLE " + table + " (" +
                    "id_deformasi INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_pengukuran INTEGER, " +
                    "deformasi_10 REAL, " +
                    "deformasi_20 REAL, " +
                    "deformasi_30 REAL, " +
                    "pemb_awal10 REAL DEFAULT 0, " +
                    "pemb_awal20 REAL DEFAULT 0, " +
                    "pemb_awal30 REAL DEFAULT 0, " +
                    "FOREIGN KEY (id_pengukuran) REFERENCES " + TABLE_PENGUKURAN + " (id_pengukuran))";
            db.execSQL(CREATE_TABLE);
            Log.d(TAG, "✅ Created table: " + table);
        }
    }

    private void createReadingsTables(SQLiteDatabase db) {
        String[] tables = {TABLE_READINGS_EX1, TABLE_READINGS_EX2, TABLE_READINGS_EX3, TABLE_READINGS_EX4};

        for (String table : tables) {
            String CREATE_TABLE = "CREATE TABLE " + table + " (" +
                    "id_reading INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_pengukuran INTEGER, " +
                    "reading_10 REAL, " +
                    "reading_20 REAL, " +
                    "reading_30 REAL, " +
                    "FOREIGN KEY (id_pengukuran) REFERENCES " + TABLE_PENGUKURAN + " (id_pengukuran))";
            db.execSQL(CREATE_TABLE);
            Log.d(TAG, "✅ Created table: " + table);
        }
    }

    private void dropAllTables(SQLiteDatabase db) {
        String[] tables = {
                TABLE_PENGUKURAN, TABLE_PEMBACAAN_EX1, TABLE_PEMBACAAN_EX2,
                TABLE_PEMBACAAN_EX3, TABLE_PEMBACAAN_EX4, TABLE_DEFORMASI_EX1,
                TABLE_DEFORMASI_EX2, TABLE_DEFORMASI_EX3, TABLE_DEFORMASI_EX4,
                TABLE_READINGS_EX1, TABLE_READINGS_EX2, TABLE_READINGS_EX3, TABLE_READINGS_EX4
        };

        for (String table : tables) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
            Log.d(TAG, "🗑️ Dropped table: " + table);
        }
    }

    // ==================== PENGUKURAN METHODS ====================

    public long insertOrUpdatePengukuran(PengukuranEksModel data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("id_pengukuran", data.getIdPengukuran());
        values.put("tahun", data.getTahun());
        values.put("periode", data.getPeriode());
        values.put("tanggal", data.getTanggal());
        values.put("dma", data.getDma());
        values.put("temp_id", data.getTempId());
        values.put("created_at", data.getCreatedAt());
        values.put("updated_at", data.getUpdatedAt());

        String whereClause = "id_pengukuran = ?";
        String[] whereArgs = {String.valueOf(data.getIdPengukuran())};

        Cursor cursor = db.query(TABLE_PENGUKURAN, null, whereClause, whereArgs, null, null, null);

        long result;
        if (cursor != null && cursor.getCount() > 0) {
            result = db.update(TABLE_PENGUKURAN, values, whereClause, whereArgs);
            Log.d(TAG, "🔄 Updated pengukuran: " + data.getIdPengukuran());
        } else {
            result = db.insert(TABLE_PENGUKURAN, null, values);
            Log.d(TAG, "✅ Inserted pengukuran: " + data.getIdPengukuran());
        }

        if (cursor != null) cursor.close();
        db.close();
        return result;
    }

    public List<PengukuranEksModel> getAllPengukuran() {
        List<PengukuranEksModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PENGUKURAN, null, null, null, null, null, "id_pengukuran DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                PengukuranEksModel model = new PengukuranEksModel();
                model.setIdPengukuran(cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran")));
                model.setTahun(cursor.getString(cursor.getColumnIndexOrThrow("tahun")));
                model.setPeriode(cursor.getString(cursor.getColumnIndexOrThrow("periode")));
                model.setTanggal(cursor.getString(cursor.getColumnIndexOrThrow("tanggal")));
                model.setDma(cursor.getString(cursor.getColumnIndexOrThrow("dma")));
                model.setTempId(cursor.getString(cursor.getColumnIndexOrThrow("temp_id")));
                model.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                model.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow("updated_at")));
                list.add(model);
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();
        db.close();
        return list;
    }

    // ==================== PEMBACAAN METHODS ====================

    public long insertOrUpdatePembacaanEx1(PembacaanEx1Model data) {
        return insertOrUpdatePembacaan(TABLE_PEMBACAAN_EX1, data);
    }

    public long insertOrUpdatePembacaanEx2(PembacaanEx2Model data) {
        return insertOrUpdatePembacaan(TABLE_PEMBACAAN_EX2, data);
    }

    public long insertOrUpdatePembacaanEx3(PembacaanEx3Model data) {
        return insertOrUpdatePembacaan(TABLE_PEMBACAAN_EX3, data);
    }

    public long insertOrUpdatePembacaanEx4(PembacaanEx4Model data) {
        return insertOrUpdatePembacaan(TABLE_PEMBACAAN_EX4, data);
    }

    private long insertOrUpdatePembacaan(String tableName, Object data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (data instanceof PembacaanEx1Model) {
            PembacaanEx1Model model = (PembacaanEx1Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("pembacaan_10", model.getPembacaan10());
            values.put("pembacaan_20", model.getPembacaan20());
            values.put("pembacaan_30", model.getPembacaan30());
        } else if (data instanceof PembacaanEx2Model) {
            PembacaanEx2Model model = (PembacaanEx2Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("pembacaan_10", model.getPembacaan10());
            values.put("pembacaan_20", model.getPembacaan20());
            values.put("pembacaan_30", model.getPembacaan30());
        } else if (data instanceof PembacaanEx3Model) {
            PembacaanEx3Model model = (PembacaanEx3Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("pembacaan_10", model.getPembacaan10());
            values.put("pembacaan_20", model.getPembacaan20());
            values.put("pembacaan_30", model.getPembacaan30());
        } else if (data instanceof PembacaanEx4Model) {
            PembacaanEx4Model model = (PembacaanEx4Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("pembacaan_10", model.getPembacaan10());
            values.put("pembacaan_20", model.getPembacaan20());
            values.put("pembacaan_30", model.getPembacaan30());
        }

        String whereClause = "id_pengukuran = ?";
        String[] whereArgs = {String.valueOf(values.getAsInteger("id_pengukuran"))};

        Cursor cursor = db.query(tableName, null, whereClause, whereArgs, null, null, null);

        long result;
        if (cursor != null && cursor.getCount() > 0) {
            result = db.update(tableName, values, whereClause, whereArgs);
            Log.d(TAG, "🔄 Updated " + tableName + " for pengukuran: " + values.getAsInteger("id_pengukuran"));
        } else {
            result = db.insert(tableName, null, values);
            Log.d(TAG, "✅ Inserted " + tableName + " for pengukuran: " + values.getAsInteger("id_pengukuran"));
        }

        if (cursor != null) cursor.close();
        db.close();
        return result;
    }

    // ==================== DEFORMASI METHODS ====================

    public long insertOrUpdateDeformasiEx1(DeformasiEx1Model data) {
        return insertOrUpdateDeformasi(TABLE_DEFORMASI_EX1, data);
    }

    public long insertOrUpdateDeformasiEx2(DeformasiEx2Model data) {
        return insertOrUpdateDeformasi(TABLE_DEFORMASI_EX2, data);
    }

    public long insertOrUpdateDeformasiEx3(DeformasiEx3Model data) {
        return insertOrUpdateDeformasi(TABLE_DEFORMASI_EX3, data);
    }

    public long insertOrUpdateDeformasiEx4(DeformasiEx4Model data) {
        return insertOrUpdateDeformasi(TABLE_DEFORMASI_EX4, data);
    }

    private long insertOrUpdateDeformasi(String tableName, Object data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (data instanceof DeformasiEx1Model) {
            DeformasiEx1Model model = (DeformasiEx1Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("deformasi_10", model.getDeformasi10());
            values.put("deformasi_20", model.getDeformasi20());
            values.put("deformasi_30", model.getDeformasi30());
            values.put("pemb_awal10", model.getPembAwal10());
            values.put("pemb_awal20", model.getPembAwal20());
            values.put("pemb_awal30", model.getPembAwal30());
        } else if (data instanceof DeformasiEx2Model) {
            DeformasiEx2Model model = (DeformasiEx2Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("deformasi_10", model.getDeformasi10());
            values.put("deformasi_20", model.getDeformasi20());
            values.put("deformasi_30", model.getDeformasi30());
            values.put("pemb_awal10", model.getPembAwal10());
            values.put("pemb_awal20", model.getPembAwal20());
            values.put("pemb_awal30", model.getPembAwal30());
        } else if (data instanceof DeformasiEx3Model) {
            DeformasiEx3Model model = (DeformasiEx3Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("deformasi_10", model.getDeformasi10());
            values.put("deformasi_20", model.getDeformasi20());
            values.put("deformasi_30", model.getDeformasi30());
            values.put("pemb_awal10", model.getPembAwal10());
            values.put("pemb_awal20", model.getPembAwal20());
            values.put("pemb_awal30", model.getPembAwal30());
        } else if (data instanceof DeformasiEx4Model) {
            DeformasiEx4Model model = (DeformasiEx4Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("deformasi_10", model.getDeformasi10());
            values.put("deformasi_20", model.getDeformasi20());
            values.put("deformasi_30", model.getDeformasi30());
            values.put("pemb_awal10", model.getPembAwal10());
            values.put("pemb_awal20", model.getPembAwal20());
            values.put("pemb_awal30", model.getPembAwal30());
        }

        String whereClause = "id_pengukuran = ?";
        String[] whereArgs = {String.valueOf(values.getAsInteger("id_pengukuran"))};

        Cursor cursor = db.query(tableName, null, whereClause, whereArgs, null, null, null);

        long result;
        if (cursor != null && cursor.getCount() > 0) {
            result = db.update(tableName, values, whereClause, whereArgs);
            Log.d(TAG, "🔄 Updated " + tableName + " for pengukuran: " + values.getAsInteger("id_pengukuran"));
        } else {
            result = db.insert(tableName, null, values);
            Log.d(TAG, "✅ Inserted " + tableName + " for pengukuran: " + values.getAsInteger("id_pengukuran"));
        }

        if (cursor != null) cursor.close();
        db.close();
        return result;
    }

    // ==================== READINGS METHODS ====================

    public long insertOrUpdateReadingsEx1(ReadingsEx1Model data) {
        return insertOrUpdateReadings(TABLE_READINGS_EX1, data);
    }

    public long insertOrUpdateReadingsEx2(ReadingsEx2Model data) {
        return insertOrUpdateReadings(TABLE_READINGS_EX2, data);
    }

    public long insertOrUpdateReadingsEx3(ReadingsEx3Model data) {
        return insertOrUpdateReadings(TABLE_READINGS_EX3, data);
    }

    public long insertOrUpdateReadingsEx4(ReadingsEx4Model data) {
        return insertOrUpdateReadings(TABLE_READINGS_EX4, data);
    }

    private long insertOrUpdateReadings(String tableName, Object data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (data instanceof ReadingsEx1Model) {
            ReadingsEx1Model model = (ReadingsEx1Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("reading_10", model.getReading10());
            values.put("reading_20", model.getReading20());
            values.put("reading_30", model.getReading30());
        } else if (data instanceof ReadingsEx2Model) {
            ReadingsEx2Model model = (ReadingsEx2Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("reading_10", model.getReading10());
            values.put("reading_20", model.getReading20());
            values.put("reading_30", model.getReading30());
        } else if (data instanceof ReadingsEx3Model) {
            ReadingsEx3Model model = (ReadingsEx3Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("reading_10", model.getReading10());
            values.put("reading_20", model.getReading20());
            values.put("reading_30", model.getReading30());
        } else if (data instanceof ReadingsEx4Model) {
            ReadingsEx4Model model = (ReadingsEx4Model) data;
            values.put("id_pengukuran", model.getIdPengukuran());
            values.put("reading_10", model.getReading10());
            values.put("reading_20", model.getReading20());
            values.put("reading_30", model.getReading30());
        }

        String whereClause = "id_pengukuran = ?";
        String[] whereArgs = {String.valueOf(values.getAsInteger("id_pengukuran"))};

        Cursor cursor = db.query(tableName, null, whereClause, whereArgs, null, null, null);

        long result;
        if (cursor != null && cursor.getCount() > 0) {
            result = db.update(tableName, values, whereClause, whereArgs);
            Log.d(TAG, "🔄 Updated " + tableName + " for pengukuran: " + values.getAsInteger("id_pengukuran"));
        } else {
            result = db.insert(tableName, null, values);
            Log.d(TAG, "✅ Inserted " + tableName + " for pengukuran: " + values.getAsInteger("id_pengukuran"));
        }

        if (cursor != null) cursor.close();
        db.close();
        return result;
    }

    // ==================== COUNT METHODS ====================

    public int getTableCount(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return count;
    }

    public int getPengukuranCount() {
        return getTableCount(TABLE_PENGUKURAN);
    }

    public int getPembacaanEx1Count() {
        return getTableCount(TABLE_PEMBACAAN_EX1);
    }

    public int getPembacaanEx2Count() {
        return getTableCount(TABLE_PEMBACAAN_EX2);
    }

    public int getPembacaanEx3Count() {
        return getTableCount(TABLE_PEMBACAAN_EX3);
    }

    public int getPembacaanEx4Count() {
        return getTableCount(TABLE_PEMBACAAN_EX4);
    }

    public int getDeformasiEx1Count() {
        return getTableCount(TABLE_DEFORMASI_EX1);
    }

    public int getDeformasiEx2Count() {
        return getTableCount(TABLE_DEFORMASI_EX2);
    }

    public int getDeformasiEx3Count() {
        return getTableCount(TABLE_DEFORMASI_EX3);
    }

    public int getDeformasiEx4Count() {
        return getTableCount(TABLE_DEFORMASI_EX4);
    }

    public int getReadingsEx1Count() {
        return getTableCount(TABLE_READINGS_EX1);
    }

    public int getReadingsEx2Count() {
        return getTableCount(TABLE_READINGS_EX2);
    }

    public int getReadingsEx3Count() {
        return getTableCount(TABLE_READINGS_EX3);
    }

    public int getReadingsEx4Count() {
        return getTableCount(TABLE_READINGS_EX4);
    }

    // ==================== DELETE METHODS ====================

    public int deletePengukuran(int idPengukuran) {
        SQLiteDatabase db = this.getWritableDatabase();

        deleteRelatedData(idPengukuran);

        int result = db.delete(TABLE_PENGUKURAN, "id_pengukuran = ?",
                new String[]{String.valueOf(idPengukuran)});

        db.close();
        Log.d(TAG, "🗑️ Deleted pengukuran: " + idPengukuran + " (result: " + result + ")");
        return result;
    }

    private void deleteRelatedData(int idPengukuran) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] tables = {
                TABLE_PEMBACAAN_EX1, TABLE_PEMBACAAN_EX2, TABLE_PEMBACAAN_EX3, TABLE_PEMBACAAN_EX4,
                TABLE_DEFORMASI_EX1, TABLE_DEFORMASI_EX2, TABLE_DEFORMASI_EX3, TABLE_DEFORMASI_EX4,
                TABLE_READINGS_EX1, TABLE_READINGS_EX2, TABLE_READINGS_EX3, TABLE_READINGS_EX4
        };

        for (String table : tables) {
            db.delete(table, "id_pengukuran = ?", new String[]{String.valueOf(idPengukuran)});
        }
        db.close();
    }

    // ==================== CLEAR METHODS ====================

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] tables = {
                TABLE_PENGUKURAN, TABLE_PEMBACAAN_EX1, TABLE_PEMBACAAN_EX2,
                TABLE_PEMBACAAN_EX3, TABLE_PEMBACAAN_EX4, TABLE_DEFORMASI_EX1,
                TABLE_DEFORMASI_EX2, TABLE_DEFORMASI_EX3, TABLE_DEFORMASI_EX4,
                TABLE_READINGS_EX1, TABLE_READINGS_EX2, TABLE_READINGS_EX3, TABLE_READINGS_EX4
        };

        for (String table : tables) {
            db.delete(table, null, null);
        }
        db.close();
        Log.d(TAG, "🧹 Cleared all data from all tables");
    }

    // ==================== DEBUG METHODS ====================

    public void logAllTableCounts() {
        String[] tables = {
                TABLE_PENGUKURAN, TABLE_PEMBACAAN_EX1, TABLE_PEMBACAAN_EX2,
                TABLE_PEMBACAAN_EX3, TABLE_PEMBACAAN_EX4, TABLE_DEFORMASI_EX1,
                TABLE_DEFORMASI_EX2, TABLE_DEFORMASI_EX3, TABLE_DEFORMASI_EX4,
                TABLE_READINGS_EX1, TABLE_READINGS_EX2, TABLE_READINGS_EX3, TABLE_READINGS_EX4
        };

        Log.d(TAG, "📊 === EXSTENSO DATABASE COUNTS ===");
        for (String table : tables) {
            int count = getTableCount(table);
            Log.d(TAG, "📋 " + table + ": " + count + " records");
        }
        Log.d(TAG, "📊 ===============================");
    }

    public void logDatabaseInfo() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        Log.d(TAG, "🔍 === DATABASE STRUCTURE ===");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
                int count = 0;
                if (countCursor != null && countCursor.moveToFirst()) {
                    count = countCursor.getInt(0);
                    countCursor.close();
                }
                Log.d(TAG, "🏷️ Table: " + tableName + " | Records: " + count);
            }
            cursor.close();
        }
        Log.d(TAG, "🔍 ==========================");
        db.close();
    }

    // ==================== BATCH OPERATIONS ====================

    public void batchInsertPengukuran(List<PengukuranEksModel> dataList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (PengukuranEksModel data : dataList) {
                ContentValues values = new ContentValues();
                values.put("id_pengukuran", data.getIdPengukuran());
                values.put("tahun", data.getTahun());
                values.put("periode", data.getPeriode());
                values.put("tanggal", data.getTanggal());
                values.put("dma", data.getDma());
                values.put("temp_id", data.getTempId());
                values.put("created_at", data.getCreatedAt());
                values.put("updated_at", data.getUpdatedAt());

                db.insertWithOnConflict(TABLE_PENGUKURAN, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
            Log.d(TAG, "✅ Batch inserted " + dataList.size() + " pengukuran records");
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // ==================== CHECK METHODS ====================

    public boolean isPengukuranExists(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PENGUKURAN,
                new String[]{"id_pengukuran"},
                "id_pengukuran = ?",
                new String[]{String.valueOf(idPengukuran)},
                null, null, null);

        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    public boolean isDatabaseEmpty() {
        return getPengukuranCount() == 0;
    }

    // ==================== DATA RETRIEVAL METHODS ====================

    public PengukuranEksModel getPengukuranById(int idPengukuran) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PENGUKURAN, null,
                "id_pengukuran = ?", new String[]{String.valueOf(idPengukuran)},
                null, null, null);

        PengukuranEksModel model = null;
        if (cursor != null && cursor.moveToFirst()) {
            model = new PengukuranEksModel();
            model.setIdPengukuran(cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran")));
            model.setTahun(cursor.getString(cursor.getColumnIndexOrThrow("tahun")));
            model.setPeriode(cursor.getString(cursor.getColumnIndexOrThrow("periode")));
            model.setTanggal(cursor.getString(cursor.getColumnIndexOrThrow("tanggal")));
            model.setDma(cursor.getString(cursor.getColumnIndexOrThrow("dma")));
            model.setTempId(cursor.getString(cursor.getColumnIndexOrThrow("temp_id")));
            model.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
            model.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow("updated_at")));
        }

        if (cursor != null) cursor.close();
        db.close();
        return model;
    }

    public List<Integer> getAllPengukuranIds() {
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PENGUKURAN,
                new String[]{"id_pengukuran"}, null, null, null, null, "id_pengukuran DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ids.add(cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran")));
            } while (cursor.moveToNext());
        }

        if (cursor != null) cursor.close();
        db.close();
        return ids;
    }
}