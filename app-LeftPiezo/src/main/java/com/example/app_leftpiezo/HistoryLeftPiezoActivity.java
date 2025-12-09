package com.example.app_leftpiezo;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryLeftPiezoActivity extends AppCompatActivity {

    private static final String TAG = "HistoryLeftPiezo";

    private Spinner spinnerPengukuran;
    private Button btnTampilkanData;
    private Button btnExportDB;
    private TextView tvEmptyState;

    // CardView - HILANGKAN I_Reading A dan I_Reading B
    private CardView cardPengukuran, cardTPembacaan, cardBPiezoMetrik, cardPerhitunganLeftPiez;

    // Containers - HILANGKAN I_Reading A dan I_Reading B
    private LinearLayout containerTPembacaan, containerBPiezoMetrik, containerPerhitunganLeftPiez;

    // TextViews untuk Pengukuran
    private TextView tvIdPengukuran, tvTanggalPengukuran, tvTahunPengukuran, tvDmaPengukuran;

    private RequestQueue requestQueue;
    private ArrayList<Integer> pengukuranIds = new ArrayList<>();
    private ArrayList<String> pengukuranLabels = new ArrayList<>();

    private static final String BASE_URL = "http://192.168.1.11/GHW/api-apps/public/api/leftpiezo/";

    private LeftPiezoDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_leftpiezo);

        dbHelper = new LeftPiezoDatabaseHelper(this);
        requestQueue = Volley.newRequestQueue(this);

        initViews();
        loadPengukuranList();

        checkDatabaseTables();

        btnTampilkanData.setOnClickListener(v -> {
            int pos = spinnerPengukuran.getSelectedItemPosition();
            if (pos >= 0 && pos < pengukuranIds.size()) {
                int pengukuranId = pengukuranIds.get(pos);
                if (isOnline()) {
                    tampilkanDataOnline(pengukuranId);
                } else {
                    tampilkanDataOffline(pengukuranId);
                }
            }
        });

        btnExportDB.setOnClickListener(v -> exportDatabaseToSQL());
    }

    private void initViews() {
        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnTampilkanData = findViewById(R.id.btnTampilkanData);
        btnExportDB = findViewById(R.id.btnExportDB);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // HILANGKAN I_Reading A dan I_Reading B
        cardPengukuran = findViewById(R.id.cardPengukuran);
        cardTPembacaan = findViewById(R.id.cardTPembacaan);
        cardBPiezoMetrik = findViewById(R.id.cardBPiezoMetrik);
        cardPerhitunganLeftPiez = findViewById(R.id.cardPerhitunganLeftPiez);

        // HILANGKAN container I_Reading A dan I_Reading B
        containerTPembacaan = findViewById(R.id.containerTPembacaan);
        containerBPiezoMetrik = findViewById(R.id.containerBPiezoMetrik);
        containerPerhitunganLeftPiez = findViewById(R.id.containerPerhitunganLeftPiez);

        tvIdPengukuran = findViewById(R.id.tvIdPengukuran);
        tvTanggalPengukuran = findViewById(R.id.tvTanggalPengukuran);
        tvTahunPengukuran = findViewById(R.id.tvTahunPengukuran);
        tvDmaPengukuran = findViewById(R.id.tvDmaPengukuran);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void loadPengukuranList() {
        if (isOnline()) {
            String url = BASE_URL + "pengukuran-leftpiez";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray data = response.getJSONArray("data");
                            pengukuranIds.clear();
                            pengukuranLabels.clear();

                            for (int i = 0; i < data.length(); i++) {
                                JSONObject obj = data.getJSONObject(i);
                                int id = obj.getInt("id_pengukuran");
                                String tgl = obj.getString("tanggal");
                                pengukuranIds.add(id);
                                pengukuranLabels.add("ID: " + id + " - " + tgl);
                            }

                            setupSpinnerAdapter();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            requestQueue.add(request);
        } else {
            // Offline: ambil list pengukuran dari SQLite
            pengukuranIds.clear();
            pengukuranLabels.clear();

            Cursor cursor = dbHelper.getAllPengukuran();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran"));
                    String tanggal = cursor.getString(cursor.getColumnIndexOrThrow("tanggal"));
                    pengukuranIds.add(id);
                    pengukuranLabels.add("ID: " + id + " - " + tanggal);
                } while (cursor.moveToNext());
                cursor.close();
            }

            setupSpinnerAdapter();
        }
    }

    private void setupSpinnerAdapter() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, pengukuranLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(spinnerAdapter);

        if (pengukuranIds.isEmpty()) {
            Toast.makeText(this, "Tidak ada data pengukuran", Toast.LENGTH_SHORT).show();
        }
    }

    // ============ Tampilkan data online ============
    private void tampilkanDataOnline(int pengukuranId) {
        String url = BASE_URL + "detail/" + pengukuranId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject data = response.getJSONObject("data");
                        tampilkanDataJson(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error loading detail: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(request);
    }

    // ============ Tampilkan data offline ============
    private void tampilkanDataOffline(int pengukuranId) {
        hideAllCards();
        tvEmptyState.setVisibility(View.GONE);

        Log.d(TAG, "=== TAMPILKAN DATA OFFLINE UNTUK ID: " + pengukuranId + " ===");

        // === Data Pengukuran ===
        Cursor pengukuranCursor = dbHelper.getAllPengukuran();
        boolean hasPengukuranData = false;

        if (pengukuranCursor != null && pengukuranCursor.moveToFirst()) {
            do {
                int id = pengukuranCursor.getInt(pengukuranCursor.getColumnIndexOrThrow("id_pengukuran"));
                if (id == pengukuranId) {
                    cardPengukuran.setVisibility(View.VISIBLE);
                    tvIdPengukuran.setText(String.valueOf(id));
                    tvTanggalPengukuran.setText(pengukuranCursor.getString(pengukuranCursor.getColumnIndexOrThrow("tanggal")));
                    tvTahunPengukuran.setText(pengukuranCursor.getString(pengukuranCursor.getColumnIndexOrThrow("tahun")));
                    tvDmaPengukuran.setText(pengukuranCursor.getString(pengukuranCursor.getColumnIndexOrThrow("dma")));
                    hasPengukuranData = true;
                    Log.d(TAG, "✅ Data Pengukuran ditemukan");
                    break;
                }
            } while (pengukuranCursor.moveToNext());
            pengukuranCursor.close();
        }

        if (!hasPengukuranData) {
            Log.d(TAG, "❌ Data Pengukuran TIDAK ditemukan");
        }

        // === Data T_Pembacaan ===
        showTPembacaanData(pengukuranId);

        // === Data B_Piezo_Metrik ===
        showBPiezoMetrikData(pengukuranId);

        // === Data Perhitungan Left Piez ===
        showPerhitunganLeftPiezData(pengukuranId);

        // Debug: Cek visibility semua card
        Log.d(TAG, "📊 STATUS CARD - Pengukuran: " + (cardPengukuran.getVisibility() == View.VISIBLE) +
                ", T_Pembacaan: " + (cardTPembacaan.getVisibility() == View.VISIBLE) +
                ", B_Piezo_Metrik: " + (cardBPiezoMetrik.getVisibility() == View.VISIBLE) +
                ", Perhitungan: " + (cardPerhitunganLeftPiez.getVisibility() == View.VISIBLE));

        // Show empty state jika tidak ada data sama sekali
        if (cardPengukuran.getVisibility() != View.VISIBLE &&
                cardTPembacaan.getVisibility() != View.VISIBLE &&
                cardBPiezoMetrik.getVisibility() != View.VISIBLE &&
                cardPerhitunganLeftPiez.getVisibility() != View.VISIBLE) {
            tvEmptyState.setVisibility(View.VISIBLE);
            Log.d(TAG, "📛 Menampilkan empty state");
        }
    }

    // HAPUS METODE showIReadingAData() DAN showIReadingBData()

    private void showTPembacaanData(int pengukuranId) {
        boolean hasTPembacaanData = false;
        containerTPembacaan.removeAllViews();

        Log.d(TAG, "🔍 Mencari data T_Pembacaan untuk ID: " + pengukuranId);

        Cursor cursor = dbHelper.getTPembacaanByPengukuran(pengukuranId);

        if (cursor != null && cursor.moveToFirst()) {
            hasTPembacaanData = true;
            Log.d(TAG, "✅ Data T_Pembacaan ditemukan: " + cursor.getCount() + " records");

            do {
                String tipePiezometer = cursor.getString(cursor.getColumnIndexOrThrow("tipe_piezometer"));
                String feet = cursor.getString(cursor.getColumnIndexOrThrow("feet"));
                String inch = cursor.getString(cursor.getColumnIndexOrThrow("inch"));

                // Header untuk tipe piezometer
                TextView header = createHeaderTextView("Tipe: " + tipePiezometer);
                containerTPembacaan.addView(header);

                // Data T_Pembacaan
                addDataRow(containerTPembacaan, "Feet", feet != null ? feet : "--");
                addDataRow(containerTPembacaan, "Inch", inch != null ? inch : "--");

                // Separator
                containerTPembacaan.addView(createSeparator());
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            Log.d(TAG, "❌ Tidak ada data T_Pembacaan");
        }

        if (hasTPembacaanData) {
            cardTPembacaan.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card T_Pembacaan DITAMPILKAN");
        }
    }

    private void showBPiezoMetrikData(int pengukuranId) {
        boolean hasBPiezoMetrikData = false;
        containerBPiezoMetrik.removeAllViews();

        Log.d(TAG, "🔍 Mencari data B_Piezo_Metrik untuk ID: " + pengukuranId);

        Cursor cursor = dbHelper.getBPiezoMetrikByPengukuran(pengukuranId);

        if (cursor != null && cursor.moveToFirst()) {
            hasBPiezoMetrikData = true;
            Log.d(TAG, "✅ Data B_Piezo_Metrik ditemukan");

            // Header
            TextView header = createHeaderTextView("DATA B_PIEZO_METRIK");
            containerBPiezoMetrik.addView(header);

            // Data konversi
            addDataRow(containerBPiezoMetrik, "Feet Konversi",
                    cursor.getString(cursor.getColumnIndexOrThrow("M_feet")));
            addDataRow(containerBPiezoMetrik, "Inch Konversi",
                    cursor.getString(cursor.getColumnIndexOrThrow("M_inch")));

            // Data readings untuk semua titik L01 sampai L10 dan SPZ02
            String[] titikList = {"l_01", "l_02", "l_03", "l_04", "l_05", "l_06", "l_07", "l_08", "l_09", "l_10", "spz_02"};
            String[] titikLabels = {"L-01", "L-02", "L-03", "L-04", "L-05", "L-06", "L-07", "L-08", "L-09", "L-10", "SPZ-02"};

            for (int i = 0; i < titikList.length; i++) {
                double value = cursor.getDouble(cursor.getColumnIndexOrThrow(titikList[i]));
                addDataRow(containerBPiezoMetrik, titikLabels[i], getDoubleOrDash(value));
            }

            cursor.close();
        } else {
            Log.d(TAG, "❌ Tidak ada data B_Piezo_Metrik");
        }

        if (hasBPiezoMetrikData) {
            cardBPiezoMetrik.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card B_Piezo_Metrik DITAMPILKAN");
        }
    }

    private void showPerhitunganLeftPiezData(int pengukuranId) {
        boolean hasPerhitunganData = false;
        containerPerhitunganLeftPiez.removeAllViews();

        Log.d(TAG, "🔍 Mencari data Perhitungan Left Piez untuk ID: " + pengukuranId);

        // Ambil semua data perhitungan untuk pengukuran ini
        String[] tipePiezometers = {"L01", "L02", "L03", "L04", "L05", "L06", "L07", "L08", "L09", "L10", "SPZ02"};

        for (String tipe : tipePiezometers) {
            Cursor cursor = dbHelper.getPerhitunganByPengukuranAndType(pengukuranId, tipe);

            if (cursor != null && cursor.moveToFirst()) {
                hasPerhitunganData = true;

                // Header untuk tipe piezometer
                TextView header = createHeaderTextView("Tipe: " + tipe);
                containerPerhitunganLeftPiez.addView(header);

                // Data perhitungan
                addDataRow(containerPerhitunganLeftPiez, "Elevasi Piez",
                        getDoubleOrDash(cursor.getDouble(cursor.getColumnIndexOrThrow("elv_piez"))));
                addDataRow(containerPerhitunganLeftPiez, "Kedalaman",
                        getDoubleOrDash(cursor.getDouble(cursor.getColumnIndexOrThrow("kedalaman"))));
                addDataRow(containerPerhitunganLeftPiez, "T PS Metrik",
                        getDoubleOrDash(cursor.getDouble(cursor.getColumnIndexOrThrow("t_psmetrik"))));
                addDataRow(containerPerhitunganLeftPiez, "Record Max",
                        getDoubleOrDash(cursor.getDouble(cursor.getColumnIndexOrThrow("record_max"))));
                addDataRow(containerPerhitunganLeftPiez, "Record Min",
                        getDoubleOrDash(cursor.getDouble(cursor.getColumnIndexOrThrow("record_min"))));

                // Separator
                containerPerhitunganLeftPiez.addView(createSeparator());

                cursor.close();
            }
        }

        if (hasPerhitunganData) {
            cardPerhitunganLeftPiez.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card Perhitungan Left Piez DITAMPILKAN");
        } else {
            Log.d(TAG, "❌ Tidak ada data Perhitungan Left Piez");
        }
    }

    // ============ Online JSON ============
    private void tampilkanDataJson(JSONObject data) throws Exception {
        hideAllCards();
        tvEmptyState.setVisibility(View.GONE);

        Log.d(TAG, "=== TAMPILKAN DATA ONLINE ===");
        Log.d(TAG, "JSON Keys: " + data.toString());

        // Data Pengukuran
        if (data.has("pengukuran")) {
            cardPengukuran.setVisibility(View.VISIBLE);
            JSONObject pengukuran = data.getJSONObject("pengukuran");
            tvIdPengukuran.setText(pengukuran.optString("id_pengukuran", "--"));
            tvTanggalPengukuran.setText(pengukuran.optString("tanggal", "--"));
            tvTahunPengukuran.setText(pengukuran.optString("tahun", "--"));
            tvDmaPengukuran.setText(pengukuran.optString("dma", "--"));
            Log.d(TAG, "✅ Data Pengukuran ditemukan");
        }

        // HAPUS BAGIAN I_Reading_A dan I_Reading_B

        // Data T_Pembacaan
        if (data.has("t_pembacaan")) {
            cardTPembacaan.setVisibility(View.VISIBLE);
            containerTPembacaan.removeAllViews();
            JSONArray tPembacaanArr = data.getJSONArray("t_pembacaan");

            for (int i = 0; i < tPembacaanArr.length(); i++) {
                JSONObject tPembacaan = tPembacaanArr.getJSONObject(i);

                TextView header = createHeaderTextView("Tipe: " + tPembacaan.optString("tipe_piezometer", "--"));
                containerTPembacaan.addView(header);

                addDataRow(containerTPembacaan, "Feet", tPembacaan.optString("feet", "--"));
                addDataRow(containerTPembacaan, "Inch", tPembacaan.optString("inch", "--"));
                containerTPembacaan.addView(createSeparator());
            }
            Log.d(TAG, "✅ Data T_Pembacaan ditemukan: " + tPembacaanArr.length() + " records");
        }

        // Data B_Piezo_Metrik
        if (data.has("b_piezo_metrik")) {
            cardBPiezoMetrik.setVisibility(View.VISIBLE);
            containerBPiezoMetrik.removeAllViews();
            JSONObject bPiezoMetrik = data.getJSONObject("b_piezo_metrik");

            Log.d(TAG, "📊 Data B_Piezo_Metrik: " + bPiezoMetrik.toString());

            TextView header = createHeaderTextView("DATA B_PIEZO_METRIK");
            containerBPiezoMetrik.addView(header);

            addDataRow(containerBPiezoMetrik, "Feet Konversi", bPiezoMetrik.optString("M_feet", "--"));
            addDataRow(containerBPiezoMetrik, "Inch Konversi", bPiezoMetrik.optString("M_inch", "--"));

            // Data untuk semua titik L01 sampai L10 dan SPZ02
            String[] titikLabels = {"L-01", "L-02", "L-03", "L-04", "L-05", "L-06", "L-07", "L-08", "L-09", "L-10", "SPZ-02"};
            String[] titikKeys = {"l_01", "l_02", "l_03", "l_04", "l_05", "l_06", "l_07", "l_08", "l_09", "l_10", "spz_02"};

            for (int i = 0; i < titikKeys.length; i++) {
                String value = bPiezoMetrik.optString(titikKeys[i], "--");
                addDataRow(containerBPiezoMetrik, titikLabels[i], value);
            }

            Log.d(TAG, "✅ Data B_Piezo_Metrik ditampilkan");
        }

        // Data Perhitungan Left Piez
        if (data.has("perhitungan_left_piez")) {
            cardPerhitunganLeftPiez.setVisibility(View.VISIBLE);
            containerPerhitunganLeftPiez.removeAllViews();
            JSONArray perhitunganArr = data.getJSONArray("perhitungan_left_piez");

            for (int i = 0; i < perhitunganArr.length(); i++) {
                JSONObject perhitungan = perhitunganArr.getJSONObject(i);

                TextView header = createHeaderTextView("Tipe: " + perhitungan.optString("tipe_piezometer", "--"));
                containerPerhitunganLeftPiez.addView(header);

                addDataRow(containerPerhitunganLeftPiez, "Elevasi Piez", perhitungan.optString("elv_piez", "--"));
                addDataRow(containerPerhitunganLeftPiez, "Kedalaman", perhitungan.optString("kedalaman", "--"));
                addDataRow(containerPerhitunganLeftPiez, "T PS Metrik", perhitungan.optString("t_psmetrik", "--"));
                addDataRow(containerPerhitunganLeftPiez, "Record Max", perhitungan.optString("record_max", "--"));
                addDataRow(containerPerhitunganLeftPiez, "Record Min", perhitungan.optString("record_min", "--"));

                containerPerhitunganLeftPiez.addView(createSeparator());
            }
            Log.d(TAG, "✅ Data Perhitungan Left Piez ditemukan: " + perhitunganArr.length() + " records");
        }

        // Debug: Cek visibility semua card
        Log.d(TAG, "📊 STATUS CARD ONLINE - Pengukuran: " + (cardPengukuran.getVisibility() == View.VISIBLE) +
                ", T_Pembacaan: " + (cardTPembacaan.getVisibility() == View.VISIBLE) +
                ", B_Piezo_Metrik: " + (cardBPiezoMetrik.getVisibility() == View.VISIBLE) +
                ", Perhitungan: " + (cardPerhitunganLeftPiez.getVisibility() == View.VISIBLE));

        // Show empty state jika tidak ada data sama sekali
        if (cardPengukuran.getVisibility() != View.VISIBLE &&
                cardTPembacaan.getVisibility() != View.VISIBLE &&
                cardBPiezoMetrik.getVisibility() != View.VISIBLE &&
                cardPerhitunganLeftPiez.getVisibility() != View.VISIBLE) {
            tvEmptyState.setVisibility(View.VISIBLE);
            Log.d(TAG, "📛 Menampilkan empty state untuk online mode");
        }
    }

    // ============ Export Database ============
    private void exportDatabaseToSQL() {
        try {
            checkDatabaseTables();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "db_leftpiezo_export_" + timeStamp + ".sql";

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File exportFile = new File(downloadDir, fileName);

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            StringBuilder sqlContent = new StringBuilder();

            sqlContent.append("-- Database Export: ").append(new Date().toString()).append("\n");
            sqlContent.append("-- Database: LeftPiezoDB\n");
            sqlContent.append("-- App: Left Piezometer\n\n");

            // Ekspor semua tabel yang ada
            Cursor tableCursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table'", null);

            int exportedCount = 0;
            while (tableCursor.moveToNext()) {
                String actualTableName = tableCursor.getString(0);

                // Skip table system SQLite
                if (actualTableName.equals("android_metadata") ||
                        actualTableName.equals("sqlite_sequence")) {
                    continue;
                }

                // Ekspor tabel ini
                exportTableData(actualTableName, sqlContent);
                exportedCount++;
            }
            tableCursor.close();

            sqlContent.append("-- Total tables exported: ").append(exportedCount).append("\n");

            // Tulis ke file
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(exportFile), StandardCharsets.UTF_8)) {
                writer.write(sqlContent.toString());
                writer.flush();
            }

            Toast.makeText(this, "Database berhasil diexport (" + exportedCount + " tables) ke: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "✅ Export berhasil: " + exportedCount + " tables");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting database: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "❌ Export error: " + e.getMessage());
        }
    }

    private void exportTableData(String tableName, StringBuilder sqlContent) {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{tableName});

            if (!cursor.moveToFirst()) {
                sqlContent.append("-- Table ").append(tableName).append(" does not exist\n\n");
                return;
            }
            cursor.close();

            cursor = dbHelper.getReadableDatabase().rawQuery(
                    "PRAGMA table_info(" + tableName + ")", null);

            if (cursor.getCount() == 0) {
                sqlContent.append("-- Table ").append(tableName).append(" has no columns\n\n");
                return;
            }

            List<String> columns = new ArrayList<>();
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            cursor.close();

            cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + tableName, null);

            sqlContent.append("-- ===========================================\n");
            sqlContent.append("-- Data untuk tabel: ").append(tableName).append("\n");
            sqlContent.append("-- ===========================================\n");

            int rowCount = 0;
            while (cursor.moveToNext()) {
                StringBuilder insertStatement = new StringBuilder();
                insertStatement.append("INSERT INTO ").append(tableName).append(" (");

                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) insertStatement.append(", ");
                    insertStatement.append(columns.get(i));
                }
                insertStatement.append(") VALUES(");

                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) insertStatement.append(", ");

                    int columnType = cursor.getType(i);
                    switch (columnType) {
                        case Cursor.FIELD_TYPE_NULL:
                            insertStatement.append("NULL");
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            insertStatement.append(cursor.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            insertStatement.append(cursor.getDouble(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            String value = cursor.getString(i);
                            if (value != null) {
                                String escapedValue = value.replace("'", "''");
                                insertStatement.append("'").append(escapedValue).append("'");
                            } else {
                                insertStatement.append("NULL");
                            }
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            insertStatement.append("NULL");
                            break;
                    }
                }

                insertStatement.append(");\n");
                sqlContent.append(insertStatement.toString());
                rowCount++;
            }

            sqlContent.append("-- Total rows: ").append(rowCount).append("\n\n");

        } catch (Exception e) {
            e.printStackTrace();
            sqlContent.append("-- Error exporting table: ").append(tableName)
                    .append(" - ").append(e.getMessage()).append("\n\n");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    private void checkDatabaseTables() {
        try {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table'", null);

            Log.d(TAG, "=== TABEL YANG ADA DI DATABASE LEFT PIEZO ===");
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                Log.d(TAG, "Table: " + tableName);
            }
            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error checking tables: " + e.getMessage());
        }
    }

    // ============ UI UTILITY METHODS ============
    private void hideAllCards() {
        cardPengukuran.setVisibility(View.GONE);
        cardTPembacaan.setVisibility(View.GONE);
        cardBPiezoMetrik.setVisibility(View.GONE);
        cardPerhitunganLeftPiez.setVisibility(View.GONE);
    }

    private void addDataRow(LinearLayout container, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvLabel = createLabelTextView(label);
        TextView tvValue = createValueTextView(value);

        row.addView(tvLabel);
        row.addView(tvValue);

        container.addView(row);
    }

    private TextView createHeaderTextView(String text) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 16, 0, 12);
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setPadding(16, 12, 16, 12);
        textView.setBackgroundColor(0xFFE8F4FF);

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFFE8F4FF);
        background.setCornerRadius(16f);
        background.setStroke(1, 0xFF0054A6);
        textView.setBackground(background);

        return textView;
    }

    private TextView createLabelTextView(String text) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(14);
        textView.setTextColor(0xFF333333);
        textView.setPadding(16, 8, 16, 8);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        return textView;
    }

    private TextView createValueTextView(String text) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(14);
        textView.setTextColor(0xFF0054A6);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setPadding(16, 8, 16, 8);
        textView.setGravity(android.view.Gravity.END);
        return textView;
    }

    private View createSeparator() {
        View separator = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(16, 12, 16, 12);
        separator.setLayoutParams(params);
        separator.setBackgroundColor(0xFFE0E0E0);
        return separator;
    }

    private String getDoubleOrDash(Double value) {
        return value != null ? String.valueOf(value) : "--";
    }

    private String getDoubleOrDash(double value) {
        return String.valueOf(value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}