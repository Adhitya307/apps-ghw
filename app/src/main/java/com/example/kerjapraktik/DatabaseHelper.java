package com.example.kerjapraktik;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "db_saguling.db";
    public static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS t_data_pengukuran (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "tahun TEXT, bulan TEXT, periode TEXT, tanggal TEXT, " +
                "tma_waduk REAL, curah_hujan REAL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS t_thomson_weir (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, " +
                "a1_r REAL, a1_l REAL, b1 REAL, b3 REAL, b5 REAL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS t_sr (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, titik TEXT, arah TEXT, nilai REAL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS t_bocoran_baru (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pengukuran_id INTEGER, elv_624_t1 REAL, elv_615_t2 REAL, pipa_p1 REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS t_data_pengukuran");
        db.execSQL("DROP TABLE IF EXISTS t_thomson_weir");
        db.execSQL("DROP TABLE IF EXISTS t_sr");
        db.execSQL("DROP TABLE IF EXISTS t_bocoran_baru");
        onCreate(db);
    }

    // === CEK DUPLIKASI ===
    public boolean isThomsonWeirExist(SQLiteDatabase db, int pengukuranId) {
        Cursor cursor = db.rawQuery("SELECT 1 FROM t_thomson_weir WHERE pengukuran_id = ?", new String[]{String.valueOf(pengukuranId)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean isSRExist(SQLiteDatabase db, int pengukuranId, String titik, String arah) {
        Cursor cursor = db.rawQuery("SELECT 1 FROM t_sr WHERE pengukuran_id = ? AND titik = ? AND arah = ?", new String[]{String.valueOf(pengukuranId), titik, arah});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean isBocoranExist(SQLiteDatabase db, int pengukuranId) {
        Cursor cursor = db.rawQuery("SELECT 1 FROM t_bocoran_baru WHERE pengukuran_id = ?", new String[]{String.valueOf(pengukuranId)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }
}
