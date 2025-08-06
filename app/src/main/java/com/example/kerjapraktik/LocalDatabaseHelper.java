package com.example.kerjapraktik;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "offline_data.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "offline_data";

    public LocalDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "json_data TEXT NOT NULL)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // ✅ Insert satu data (dipakai saat tidak ada internet)
    public boolean insertData(String jsonData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("json_data", jsonData);
        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }

    // ✅ Ambil semua data (dipakai saat sinkronisasi)
    public List<String> getAllData() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT json_data FROM " + TABLE_NAME, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursor.getString(cursor.getColumnIndexOrThrow("json_data")));
            }
            cursor.close();
        }
        return list;
    }

    // ✅ Hapus berdasarkan isi JSON (dipanggil jika sinkron berhasil)
    public void deleteData(String jsonData) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "json_data = ?", new String[]{jsonData});
    }

    // (Optional) Bersihkan semua data
    public void deleteAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }
}
