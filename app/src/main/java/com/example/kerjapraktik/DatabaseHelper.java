package com.example.kerjapraktik;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.volley.BuildConfig;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "db_saguling.db";
    public static final int DB_VERSION = 13;
    private static final String TAG = "DBHelper";

    private static SQLiteDatabase instance;

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

        // === Tabel Bocoran Baru ===
        db.execSQL("CREATE TABLE IF NOT EXISTS t_bocoran_baru (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "elv_624_t1 REAL, elv_624_t1_kode TEXT, " +
                "elv_615_t2 REAL, elv_615_t2_kode TEXT, " +
                "pipa_p1 REAL, pipa_p1_kode TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        // ========== TABEL BARU ==========
        db.execSQL("CREATE TABLE IF NOT EXISTS p_batasmaksimal (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "batas_maksimal REAL, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS p_bocoran_baru (" +
                "id INTEGER PRIMARY KEY, " +
                "pengukuran_id INTEGER, " +
                "talang1 REAL DEFAULT 0, " +
                "talang2 REAL DEFAULT 0, " +
                "pipa REAL DEFAULT 0, " +
                "created_at TEXT, " +
                "updated_at TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS p_intigalery (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "a1 REAL, " +
                "ambang_a1 REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS p_spillway (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "B3 REAL, " +
                "ambang REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS p_sr (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "sr_1_q REAL, " +
                "sr_40_q REAL, " +
                "sr_66_q REAL, " +
                "sr_68_q REAL, " +
                "sr_70_q REAL, " +
                "sr_79_q REAL, " +
                "sr_81_q REAL, " +
                "sr_83_q REAL, " +
                "sr_85_q REAL, " +
                "sr_92_q REAL, " +
                "sr_94_q REAL, " +
                "sr_96_q REAL, " +
                "sr_98_q REAL, " +
                "sr_100_q REAL, " +
                "sr_102_q REAL, " +
                "sr_104_q REAL, " +
                "sr_106_q REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS p_tebingkanan (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "sr REAL DEFAULT 0, " +
                "ambang REAL DEFAULT 0, " +
                "created_at TEXT, " +
                "updated_at TEXT, " +
                "b5 REAL, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS p_thomson_weir (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "a1_r REAL, " +
                "a1_l REAL, " +
                "b1 REAL, " +
                "b3 REAL, " +
                "b5 REAL, " +
                "pengukuran_id INTEGER, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS p_totalbocoran (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "R1 REAL, " +
                "created_at TEXT, " +
                "updated_at TEXT, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS analisa_look_burt (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER UNIQUE, " +
                "rembesan_bendungan REAL, " +
                "panjang_bendungan REAL, " +
                "rembesan_per_m REAL, " +
                "nilai_ambang_ok REAL DEFAULT 0.28, " +
                "nilai_ambang_notok REAL DEFAULT 0.56, " +
                "keterangan TEXT, " +
                "is_synced INTEGER DEFAULT 0, " +
                "FOREIGN KEY (pengukuran_id) REFERENCES t_data_pengukuran(id) ON DELETE CASCADE" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS t_data_pengukuran");
        db.execSQL("DROP TABLE IF EXISTS t_thomson_weir");
        db.execSQL("DROP TABLE IF EXISTS t_sr");
        db.execSQL("DROP TABLE IF EXISTS t_bocoran_baru");
        db.execSQL("DROP TABLE IF EXISTS p_batasmaksimal");
        db.execSQL("DROP TABLE IF EXISTS p_bocoran_baru");
        db.execSQL("DROP TABLE IF EXISTS p_intigalery");
        db.execSQL("DROP TABLE IF EXISTS p_spillway");
        db.execSQL("DROP TABLE IF EXISTS p_sr");
        db.execSQL("DROP TABLE IF EXISTS p_tebingkanan");
        db.execSQL("DROP TABLE IF EXISTS p_thomson_weir");
        db.execSQL("DROP TABLE IF EXISTS p_totalbocoran");
        db.execSQL("DROP TABLE IF EXISTS analisa_look_burt");
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

    @Override
    public void close() {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "close() diabaikan karena DEBUG mode -> DB tetap terbuka");
        } else {
            super.close();
        }
    }

    public void closeDB() {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "closeDB() diabaikan karena DEBUG mode -> DB tetap terbuka");
        } else {
            if (instance != null && instance.isOpen()) {
                instance.close();
                instance = null;
                Log.i(TAG, "✅ Database ditutup (Release mode)");
            }
        }
    }

    // =======================================
    // INSERT OR UPDATE METHODS
    // =======================================

    // =======================================
    // Insert / Update Pengukuran
    // =======================================
    public void insertOrUpdatePengukuran(int id,
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

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("t_data_pengukuran", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.i(TAG, "✅ Insert/Update Pengukuran | ID=" + id + ", Tanggal=" + tanggal);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update Pengukuran | ID=" + id);
        }
    }

    // =======================================
    // Insert / Update Thomson Weir
    // =======================================
    public void insertOrUpdateThomsonWeir(int pengukuranId, Double a1r, Double a1l,
                                          Double b1, Double b3, Double b5) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (a1r != null) values.put("a1_r", a1r);
        if (a1l != null) values.put("a1_l", a1l);
        if (b1 != null) values.put("b1", b1);
        if (b3 != null) values.put("b3", b3);
        if (b5 != null) values.put("b5", b5);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("t_thomson_weir", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update Thomson | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update Thomson | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update SR
    // =======================================
    public void insertOrUpdateSRFull(int pengukuranId,
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

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("t_sr", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update SR | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update SR | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update Bocoran
    // =======================================
    public void insertOrUpdateBocoranFull(int pengukuranId,
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

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("t_bocoran_baru", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update Bocoran | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update Bocoran | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update p_batasmaksimal
    // =======================================
    public void insertOrUpdatePBatasMaksimal(int pengukuranId, Double batasMaksimal) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (batasMaksimal != null) values.put("batas_maksimal", batasMaksimal);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("p_batasmaksimal", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update p_batasmaksimal | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update p_batasmaksimal | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update p_bocoran_baru
    // =======================================
    public void insertOrUpdatePBocoranBaru(int id, int pengukuranId, Double talang1,
                                           Double talang2, Double pipa,
                                           String createdAt, String updatedAt) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("pengukuran_id", pengukuranId);
        if (talang1 != null) values.put("talang1", talang1);
        if (talang2 != null) values.put("talang2", talang2);
        if (pipa != null) values.put("pipa", pipa);
        if (createdAt != null) values.put("created_at", createdAt);
        if (updatedAt != null) values.put("updated_at", updatedAt);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("p_bocoran_baru", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update p_bocoran_baru | id=" + id);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update p_bocoran_baru | id=" + id);
        }
    }

    // =======================================
    // Insert / Update p_intigalery
    // =======================================
    public void insertOrUpdatePIntiGallery(int pengukuranId, Double a1, Double ambangA1,
                                           String createdAt, String updatedAt) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (a1 != null) values.put("a1", a1);
        if (ambangA1 != null) values.put("ambang_a1", ambangA1);
        if (createdAt != null) values.put("created_at", createdAt);
        if (updatedAt != null) values.put("updated_at", updatedAt);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("p_intigalery", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update p_intigalery | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update p_intigalery | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update p_spillway
    // =======================================
    public void insertOrUpdatePSpillway(int pengukuranId, Double b3, Double ambang,
                                        String createdAt, String updatedAt) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (b3 != null) values.put("B3", b3);
        if (ambang != null) values.put("ambang", ambang);
        if (createdAt != null) values.put("created_at", createdAt);
        if (updatedAt != null) values.put("updated_at", updatedAt);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("p_spillway", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update p_spillway | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update p_spillway | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update p_sr
    // =======================================
    public void insertOrUpdatePSr(int pengukuranId,
                                  Double sr1q, Double sr40q, Double sr66q, Double sr68q,
                                  Double sr70q, Double sr79q, Double sr81q, Double sr83q,
                                  Double sr85q, Double sr92q, Double sr94q, Double sr96q,
                                  Double sr98q, Double sr100q, Double sr102q, Double sr104q,
                                  Double sr106q, String createdAt, String updatedAt) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (sr1q != null) values.put("sr_1_q", sr1q);
        if (sr40q != null) values.put("sr_40_q", sr40q);
        if (sr66q != null) values.put("sr_66_q", sr66q);
        if (sr68q != null) values.put("sr_68_q", sr68q);
        if (sr70q != null) values.put("sr_70_q", sr70q);
        if (sr79q != null) values.put("sr_79_q", sr79q);
        if (sr81q != null) values.put("sr_81_q", sr81q);
        if (sr83q != null) values.put("sr_83_q", sr83q);
        if (sr85q != null) values.put("sr_85_q", sr85q);
        if (sr92q != null) values.put("sr_92_q", sr92q);
        if (sr94q != null) values.put("sr_94_q", sr94q);
        if (sr96q != null) values.put("sr_96_q", sr96q);
        if (sr98q != null) values.put("sr_98_q", sr98q);
        if (sr100q != null) values.put("sr_100_q", sr100q);
        if (sr102q != null) values.put("sr_102_q", sr102q);
        if (sr104q != null) values.put("sr_104_q", sr104q);
        if (sr106q != null) values.put("sr_106_q", sr106q);
        if (createdAt != null) values.put("created_at", createdAt);
        if (updatedAt != null) values.put("updated_at", updatedAt);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("p_sr", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update p_sr | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update p_sr | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update p_tebingkanan
    // =======================================
    public void insertOrUpdatePTebingKanan(int pengukuranId, Double sr, Double ambang,
                                           Double b5, String createdAt, String updatedAt) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (sr != null) values.put("sr", sr);
        if (ambang != null) values.put("ambang", ambang);
        if (b5 != null) values.put("b5", b5);
        if (createdAt != null) values.put("created_at", createdAt);
        if (updatedAt != null) values.put("updated_at", updatedAt);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("p_tebingkanan", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update p_tebingkanan | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update p_tebingkanan | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update p_thomson_weir
    // =======================================
    public void insertOrUpdatePThomsonWeir(int pengukuranId, Double a1r, Double a1l,
                                           Double b1, Double b3, Double b5) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (a1r != null) values.put("a1_r", a1r);
        if (a1l != null) values.put("a1_l", a1l);
        if (b1 != null) values.put("b1", b1);
        if (b3 != null) values.put("b3", b3);
        if (b5 != null) values.put("b5", b5);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("p_thomson_weir", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update p_thomson_weir | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update p_thomson_weir | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update p_totalbocoran
    // =======================================
    public void insertOrUpdatePTotalBocoran(int pengukuranId, Double r1,
                                            String createdAt, String updatedAt) {
        SQLiteDatabase db = getDB();

        ContentValues values = new ContentValues();
        values.put("pengukuran_id", pengukuranId);
        if (r1 != null) values.put("R1", r1);
        if (createdAt != null) values.put("created_at", createdAt);
        if (updatedAt != null) values.put("updated_at", updatedAt);
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("p_totalbocoran", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update p_totalbocoran | pengukuranId=" + pengukuranId);
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update p_totalbocoran | pengukuranId=" + pengukuranId);
        }
    }

    // =======================================
    // Insert / Update AnalisaLookBurt
    // =======================================
    public void insertOrUpdateAnalisaLookBurt(AnalisaLookBurtModel model) {
        SQLiteDatabase db = getDB();
        ContentValues values = new ContentValues();

        values.put("pengukuran_id", model.getPengukuranId());
        values.put("rembesan_bendungan", model.getRembesanBendungan() != null ? model.getRembesanBendungan() : 0.0);
        values.put("panjang_bendungan", model.getPanjangBendungan() != null ? model.getPanjangBendungan() : 0.0);
        values.put("rembesan_per_m", model.getRembesanPerM() != null ? model.getRembesanPerM() : 0.0);
        values.put("nilai_ambang_ok", model.getNilaiAmbangOk() != null ? model.getNilaiAmbangOk() : 0.28);
        values.put("nilai_ambang_notok", model.getNilaiAmbangNotok() != null ? model.getNilaiAmbangNotok() : 0.56);
        values.put("keterangan", model.getKeterangan() != null ? model.getKeterangan() : "");
        values.put("is_synced", 1);

        // INSERT OR REPLACE
        long result = db.insertWithOnConflict("analisa_look_burt", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            Log.d(TAG, "✅ Insert/Update analisa_look_burt | pengukuran_id=" + model.getPengukuranId());
        } else {
            Log.e(TAG, "❌ Gagal Insert/Update analisa_look_burt | pengukuran_id=" + model.getPengukuranId());
        }
    }

    // ============================================
    // ======= CEK DUPLIKASI (KEEP EXISTING) =====
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

    // =======================================
    // ORIGINAL METHODS (KEEP FOR BACKWARD COMPATIBILITY)
    // =======================================

    public void insertPengukuran(int id, String tahun, String bulan, String periode,
                                 String tanggal, Double tma_waduk, Double curah_hujan, String temp_id) {
        insertOrUpdatePengukuran(id, tahun, bulan, periode, tanggal, tma_waduk, curah_hujan, temp_id);
    }

    public void insertThomsonWeir(int pengukuranId, Double a1r, Double a1l,
                                  Double b1, Double b3, Double b5) {
        insertOrUpdateThomsonWeir(pengukuranId, a1r, a1l, b1, b3, b5);
    }

    public void insertSRFull(int pengukuranId, String sr1Kode, Double sr1Nilai,
                             String sr40Kode, Double sr40Nilai, String sr66Kode, Double sr66Nilai,
                             String sr68Kode, Double sr68Nilai, String sr70Kode, Double sr70Nilai,
                             String sr79Kode, Double sr79Nilai, String sr81Kode, Double sr81Nilai,
                             String sr83Kode, Double sr83Nilai, String sr85Kode, Double sr85Nilai,
                             String sr92Kode, Double sr92Nilai, String sr94Kode, Double sr94Nilai,
                             String sr96Kode, Double sr96Nilai, String sr98Kode, Double sr98Nilai,
                             String sr100Kode, Double sr100Nilai, String sr102Kode, Double sr102Nilai,
                             String sr104Kode, Double sr104Nilai, String sr106Kode, Double sr106Nilai) {
        insertOrUpdateSRFull(pengukuranId, sr1Kode, sr1Nilai, sr40Kode, sr40Nilai, sr66Kode, sr66Nilai,
                sr68Kode, sr68Nilai, sr70Kode, sr70Nilai, sr79Kode, sr79Nilai, sr81Kode, sr81Nilai,
                sr83Kode, sr83Nilai, sr85Kode, sr85Nilai, sr92Kode, sr92Nilai, sr94Kode, sr94Nilai,
                sr96Kode, sr96Nilai, sr98Kode, sr98Nilai, sr100Kode, sr100Nilai, sr102Kode, sr102Nilai,
                sr104Kode, sr104Nilai, sr106Kode, sr106Nilai);
    }

    public void insertBocoranFull(int pengukuranId, Double elv624, String elv624Kode,
                                  Double elv615, String elv615Kode, Double pipa, String pipaKode) {
        insertOrUpdateBocoranFull(pengukuranId, elv624, elv624Kode, elv615, elv615Kode, pipa, pipaKode);
    }

    public void insertPBatasMaksimal(int pengukuranId, Double batasMaksimal) {
        insertOrUpdatePBatasMaksimal(pengukuranId, batasMaksimal);
    }

    public void insertPBocoranBaru(int id, int pengukuranId, Double talang1, Double talang2,
                                   Double pipa, String createdAt, String updatedAt) {
        insertOrUpdatePBocoranBaru(id, pengukuranId, talang1, talang2, pipa, createdAt, updatedAt);
    }

    public void insertPIntiGallery(int pengukuranId, Double a1, Double ambangA1,
                                   String createdAt, String updatedAt) {
        insertOrUpdatePIntiGallery(pengukuranId, a1, ambangA1, createdAt, updatedAt);
    }

    public void insertPSpillway(int pengukuranId, Double b3, Double ambang,
                                String createdAt, String updatedAt) {
        insertOrUpdatePSpillway(pengukuranId, b3, ambang, createdAt, updatedAt);
    }

    public void insertPSr(int pengukuranId, Double sr1q, Double sr40q, Double sr66q, Double sr68q,
                          Double sr70q, Double sr79q, Double sr81q, Double sr83q, Double sr85q,
                          Double sr92q, Double sr94q, Double sr96q, Double sr98q, Double sr100q,
                          Double sr102q, Double sr104q, Double sr106q, String createdAt, String updatedAt) {
        insertOrUpdatePSr(pengukuranId, sr1q, sr40q, sr66q, sr68q, sr70q, sr79q, sr81q, sr83q, sr85q,
                sr92q, sr94q, sr96q, sr98q, sr100q, sr102q, sr104q, sr106q, createdAt, updatedAt);
    }

    public void insertPTebingKanan(int pengukuranId, Double sr, Double ambang,
                                   Double b5, String createdAt, String updatedAt) {
        insertOrUpdatePTebingKanan(pengukuranId, sr, ambang, b5, createdAt, updatedAt);
    }

    public void insertPThomsonWeir(int pengukuranId, Double a1r, Double a1l,
                                   Double b1, Double b3, Double b5) {
        insertOrUpdatePThomsonWeir(pengukuranId, a1r, a1l, b1, b3, b5);
    }

    public void insertPTotalBocoran(int pengukuranId, Double r1,
                                    String createdAt, String updatedAt) {
        insertOrUpdatePTotalBocoran(pengukuranId, r1, createdAt, updatedAt);
    }

    public void insertAnalisaLookBurt(AnalisaLookBurtModel model) {
        insertOrUpdateAnalisaLookBurt(model);
    }

    // ===== Ambil AnalisaLookBurt by pengukuranId =====
    public AnalisaLookBurtModel getAnalisaLookBurtByPengukuranId(int pengukuranId) {
        SQLiteDatabase db = this.getReadableDatabase();
        AnalisaLookBurtModel model = null;
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM analisa_look_burt WHERE pengukuran_id = ?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                model = new AnalisaLookBurtModel();
                model.setPengukuranId(cursor.getInt(cursor.getColumnIndexOrThrow("pengukuran_id")));
                model.setRembesanBendungan(!cursor.isNull(cursor.getColumnIndexOrThrow("rembesan_bendungan"))
                        ? cursor.getDouble(cursor.getColumnIndexOrThrow("rembesan_bendungan")) : 0.0);
                model.setPanjangBendungan(!cursor.isNull(cursor.getColumnIndexOrThrow("panjang_bendungan"))
                        ? cursor.getDouble(cursor.getColumnIndexOrThrow("panjang_bendungan")) : 0.0);
                model.setRembesanPerM(!cursor.isNull(cursor.getColumnIndexOrThrow("rembesan_per_m"))
                        ? cursor.getDouble(cursor.getColumnIndexOrThrow("rembesan_per_m")) : 0.0);
                model.setNilaiAmbangOk(!cursor.isNull(cursor.getColumnIndexOrThrow("nilai_ambang_ok"))
                        ? cursor.getDouble(cursor.getColumnIndexOrThrow("nilai_ambang_ok")) : 0.28);
                model.setNilaiAmbangNotok(!cursor.isNull(cursor.getColumnIndexOrThrow("nilai_ambang_notok"))
                        ? cursor.getDouble(cursor.getColumnIndexOrThrow("nilai_ambang_notok")) : 0.56);
                model.setKeterangan(cursor.getString(cursor.getColumnIndexOrThrow("keterangan")));
                model.setIsSynced(cursor.getInt(cursor.getColumnIndexOrThrow("is_synced")));
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return model;
    }
}