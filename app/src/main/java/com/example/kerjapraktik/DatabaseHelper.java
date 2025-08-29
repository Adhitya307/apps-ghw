package com.example.kerjapraktik;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "db_saguling.db";
    public static final int DB_VERSION = 9; // naikin versi biar drop & create ulang
    private static final String TAG = "DBHelper";

    private static SQLiteDatabase instance; // cache connection

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // === Tabel Data Pengukuran ===
        db.execSQL("CREATE TABLE IF NOT EXISTS t_data_pengukuran (" +
                "id INTEGER PRIMARY KEY, " +
                "tahun TEXT, bulan TEXT, periode TEXT, tanggal TEXT, " +
                "tma_waduk REAL, curah_hujan REAL, " +
                "temp_id TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        // === Tabel Thomson Weir ===
        db.execSQL("CREATE TABLE IF NOT EXISTS t_thomson_weir (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "a1_r REAL, a1_l REAL, b1 REAL, b3 REAL, b5 REAL, " +
                "is_synced INTEGER DEFAULT 0)");

        // === Tabel SR ===
        db.execSQL("CREATE TABLE IF NOT EXISTS t_sr (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "sr_1_kode TEXT, sr_1_nilai REAL, " +
                "sr_40_kode TEXT, sr_40_nilai REAL, " +
                "sr_66_kode TEXT, sr_66_nilai REAL, " +
                "sr_68_kode TEXT, sr_68_nilai REAL, " +
                "sr_70_kode TEXT, sr_70_nilai REAL, " +
                "sr_79_kode TEXT, sr_79_nilai REAL, " +
                "sr_81_kode TEXT, sr_81_nilai REAL, " +
                "sr_83_kode TEXT, sr_83_nilai REAL, " +
                "sr_85_kode TEXT, sr_85_nilai REAL, " +
                "sr_92_kode TEXT, sr_92_nilai REAL, " +
                "sr_94_kode TEXT, sr_94_nilai REAL, " +
                "sr_96_kode TEXT, sr_96_nilai REAL, " +
                "sr_98_kode TEXT, sr_98_nilai REAL, " +
                "sr_100_kode TEXT, sr_100_nilai REAL, " +
                "sr_102_kode TEXT, sr_102_nilai REAL, " +
                "sr_104_kode TEXT, sr_104_nilai REAL, " +
                "sr_106_kode TEXT, sr_106_nilai REAL, " +
                "is_synced INTEGER DEFAULT 0)");

        // === Tabel Bocoran Baru (pakai nilai + kode) ===
        db.execSQL("CREATE TABLE IF NOT EXISTS t_bocoran_baru (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "elv_624_t1 REAL, elv_624_t1_kode TEXT, " +
                "elv_615_t2 REAL, elv_615_t2_kode TEXT, " +
                "pipa_p1 REAL, pipa_p1_kode TEXT, " +
                "is_synced INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS t_data_pengukuran");
        db.execSQL("DROP TABLE IF EXISTS t_thomson_weir");
        db.execSQL("DROP TABLE IF EXISTS t_sr");
        db.execSQL("DROP TABLE IF EXISTS t_bocoran_baru");
        onCreate(db);
    }

    // =======================================
    // Get writable database (singleton)
    // =======================================
    private SQLiteDatabase getDB() {
        if (instance == null || !instance.isOpen()) {
            instance = this.getWritableDatabase();
        }
        return instance;
    }

    // Tutup DB manual jika perlu
    public void closeDB() {
        if (instance != null && instance.isOpen()) {
            instance.close();
            instance = null;
        }
    }

    // =======================================
    // Insert / Update Pengukuran
    // =======================================
    public void insertPengukuran(int id,
                                 String tahun,
                                 String bulan,
                                 String periode,
                                 String tanggal,
                                 Double tma_waduk,
                                 Double curah_hujan,
                                 String temp_id) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("tahun", tahun);
        values.put("bulan", bulan);
        values.put("periode", periode);
        values.put("tanggal", tanggal);
        if (tma_waduk != null) values.put("tma_waduk", tma_waduk);
        if (curah_hujan != null) values.put("curah_hujan", curah_hujan);
        if (temp_id != null) values.put("temp_id", temp_id);
        values.put("is_synced", 1);

        Cursor cursor = db.rawQuery("SELECT id FROM t_data_pengukuran WHERE id = ?", new String[]{String.valueOf(id)});
        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (exists) {
            db.update("t_data_pengukuran", values, "id = ?", new String[]{String.valueOf(id)});
            Log.i(TAG, "✅ Update Pengukuran | ID=" + id + ", Tanggal=" + tanggal);
        } else {
            db.insert("t_data_pengukuran", null, values);
            Log.i(TAG, "✅ Insert Pengukuran | ID=" + id + ", Tanggal=" + tanggal);
        }
    }

    // =======================================
    // Insert Thomson
    // =======================================
    public void insertThomsonWeir(int pengukuranId, Double a1r, Double a1l,
                                  Double b1, Double b3, Double b5) {
        SQLiteDatabase db = getDB();

        if (!isThomsonWeirExist(db, pengukuranId)) {
            ContentValues values = new ContentValues();
            values.put("pengukuran_id", pengukuranId);
            if (a1r != null) values.put("a1_r", a1r);
            if (a1l != null) values.put("a1_l", a1l);
            if (b1 != null) values.put("b1", b1);
            if (b3 != null) values.put("b3", b3);
            if (b5 != null) values.put("b5", b5);
            values.put("is_synced", 1);

            db.insert("t_thomson_weir", null, values);
            Log.d(TAG, "✅ Insert Thomson OK | pengukuranId=" + pengukuranId);
        } else {
            Log.w(TAG, "⚠️ Thomson sudah ada, skip | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert SR
    // =======================================
    public void insertSRFull(int pengukuranId,
                             String sr1Kode, Double sr1Nilai,
                             String sr40Kode, Double sr40Nilai,
                             String sr66Kode, Double sr66Nilai,
                             String sr68Kode, Double sr68Nilai,
                             String sr70Kode, Double sr70Nilai,
                             String sr79Kode, Double sr79Nilai,
                             String sr81Kode, Double sr81Nilai,
                             String sr83Kode, Double sr83Nilai,
                             String sr85Kode, Double sr85Nilai,
                             String sr92Kode, Double sr92Nilai,
                             String sr94Kode, Double sr94Nilai,
                             String sr96Kode, Double sr96Nilai,
                             String sr98Kode, Double sr98Nilai,
                             String sr100Kode, Double sr100Nilai,
                             String sr102Kode, Double sr102Nilai,
                             String sr104Kode, Double sr104Nilai,
                             String sr106Kode, Double sr106Nilai) {

        SQLiteDatabase db = getDB();
        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);

        if (sr1Kode != null) values.put("sr_1_kode", sr1Kode);
        if (sr1Nilai != null) values.put("sr_1_nilai", sr1Nilai);
        if (sr40Kode != null) values.put("sr_40_kode", sr40Kode);
        if (sr40Nilai != null) values.put("sr_40_nilai", sr40Nilai);
        if (sr66Kode != null) values.put("sr_66_kode", sr66Kode);
        if (sr66Nilai != null) values.put("sr_66_nilai", sr66Nilai);
        if (sr68Kode != null) values.put("sr_68_kode", sr68Kode);
        if (sr68Nilai != null) values.put("sr_68_nilai", sr68Nilai);
        if (sr70Kode != null) values.put("sr_70_kode", sr70Kode);
        if (sr70Nilai != null) values.put("sr_70_nilai", sr70Nilai);
        if (sr79Kode != null) values.put("sr_79_kode", sr79Kode);
        if (sr79Nilai != null) values.put("sr_79_nilai", sr79Nilai);
        if (sr81Kode != null) values.put("sr_81_kode", sr81Kode);
        if (sr81Nilai != null) values.put("sr_81_nilai", sr81Nilai);
        if (sr83Kode != null) values.put("sr_83_kode", sr83Kode);
        if (sr83Nilai != null) values.put("sr_83_nilai", sr83Nilai);
        if (sr85Kode != null) values.put("sr_85_kode", sr85Kode);
        if (sr85Nilai != null) values.put("sr_85_nilai", sr85Nilai);
        if (sr92Kode != null) values.put("sr_92_kode", sr92Kode);
        if (sr92Nilai != null) values.put("sr_92_nilai", sr92Nilai);
        if (sr94Kode != null) values.put("sr_94_kode", sr94Kode);
        if (sr94Nilai != null) values.put("sr_94_nilai", sr94Nilai);
        if (sr96Kode != null) values.put("sr_96_kode", sr96Kode);
        if (sr96Nilai != null) values.put("sr_96_nilai", sr96Nilai);
        if (sr98Kode != null) values.put("sr_98_kode", sr98Kode);
        if (sr98Nilai != null) values.put("sr_98_nilai", sr98Nilai);
        if (sr100Kode != null) values.put("sr_100_kode", sr100Kode);
        if (sr100Nilai != null) values.put("sr_100_nilai", sr100Nilai);
        if (sr102Kode != null) values.put("sr_102_kode", sr102Kode);
        if (sr102Nilai != null) values.put("sr_102_nilai", sr102Nilai);
        if (sr104Kode != null) values.put("sr_104_kode", sr104Kode);
        if (sr104Nilai != null) values.put("sr_104_nilai", sr104Nilai);
        if (sr106Kode != null) values.put("sr_106_kode", sr106Kode);
        if (sr106Nilai != null) values.put("sr_106_nilai", sr106Nilai);

        values.put("is_synced", 1);

        Cursor c = db.rawQuery("SELECT id FROM t_sr WHERE pengukuran_id = ?", new String[]{String.valueOf(pengukuranId)});
        boolean exists = c.moveToFirst();
        c.close();

        if (exists) {
            db.update("t_sr", values, "pengukuran_id = ?", new String[]{String.valueOf(pengukuranId)});
            Log.d(TAG, "✅ Update SR | pengukuranId=" + pengukuranId);
        } else {
            db.insert("t_sr", null, values);
            Log.d(TAG, "✅ Insert SR | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert Bocoran
    // =======================================
    public void insertBocoranFull(int pengukuranId,
                                  Double elv624, String elv624Kode,
                                  Double elv615, String elv615Kode,
                                  Double pipa, String pipaKode) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (elv624 != null) values.put("elv_624_t1", elv624);
        if (elv624Kode != null) values.put("elv_624_t1_kode", elv624Kode);
        if (elv615 != null) values.put("elv_615_t2", elv615);
        if (elv615Kode != null) values.put("elv_615_t2_kode", elv615Kode);
        if (pipa != null) values.put("pipa_p1", pipa);
        if (pipaKode != null) values.put("pipa_p1_kode", pipaKode);
        values.put("is_synced", 1);

        Cursor c = db.rawQuery("SELECT id FROM t_bocoran_baru WHERE pengukuran_id = ?", new String[]{String.valueOf(pengukuranId)});
        boolean exists = c.moveToFirst();
        c.close();

        if (exists) {
            db.update("t_bocoran_baru", values, "pengukuran_id = ?", new String[]{String.valueOf(pengukuranId)});
            Log.d(TAG, "✅ Update Bocoran | pengukuranId=" + pengukuranId);
        } else {
            db.insert("t_bocoran_baru", null, values);
            Log.d(TAG, "✅ Insert Bocoran | pengukuranId=" + pengukuranId);
        }
    }

    // ============================================
    // ======= CEK DUPLIKASI ======================
    // ============================================
    public boolean isThomsonWeirExist(SQLiteDatabase db, int pengukuranId) {
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM t_thomson_weir WHERE pengukuran_id = ?",
                new String[]{String.valueOf(pengukuranId)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean isBocoranExist(SQLiteDatabase db, int pengukuranId) {
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM t_bocoran_baru WHERE pengukuran_id = ?",
                new String[]{String.valueOf(pengukuranId)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // ============================================
    // ========== DEBUG : PRINT ALL DATA ==========
    // ============================================
    public void debugPrintAllPengukuran() {
        SQLiteDatabase db = getDB();
        Cursor cursor = db.rawQuery("SELECT * FROM t_data_pengukuran", null);

        Log.d(TAG, "==== Isi t_data_pengukuran ====");
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String tahun = cursor.getString(cursor.getColumnIndexOrThrow("tahun"));
                String bulan = cursor.getString(cursor.getColumnIndexOrThrow("bulan"));
                String periode = cursor.getString(cursor.getColumnIndexOrThrow("periode"));
                String tanggal = cursor.getString(cursor.getColumnIndexOrThrow("tanggal"));
                double tma = cursor.isNull(cursor.getColumnIndexOrThrow("tma_waduk")) ? -1 : cursor.getDouble(cursor.getColumnIndexOrThrow("tma_waduk"));
                double hujan = cursor.isNull(cursor.getColumnIndexOrThrow("curah_hujan")) ? -1 : cursor.getDouble(cursor.getColumnIndexOrThrow("curah_hujan"));
                String tempId = cursor.getString(cursor.getColumnIndexOrThrow("temp_id"));

                Log.d(TAG, "Row -> ID=" + id + ", Tahun=" + tahun +
                        ", Bulan=" + bulan + ", Periode=" + periode +
                        ", Tanggal=" + tanggal + ", TMA=" + tma +
                        ", Hujan=" + hujan + ", TempId=" + tempId);
            } while (cursor.moveToNext());
        } else {
            Log.w(TAG, "⚠️ Tidak ada data di t_data_pengukuran");
        }

        cursor.close();
    }
}
