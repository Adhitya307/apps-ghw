package com.example.app.exstenso;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
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

public class HistoryExstensoActivity extends AppCompatActivity {

    private static final String TAG = "HistoryExstensoActivity";

    private Spinner spinnerPengukuran;
    private Button btnTampilkanData;
    private Button btnExportDB;
    private TextView tvEmptyState;

    // CardView
    private CardView cardPengukuran, cardPembacaan, cardDeformasi, cardReadings;

    // Containers
    private LinearLayout containerPembacaan, containerDeformasi, containerReadings;

    // TextViews untuk Pengukuran
    private TextView tvIdPengukuran, tvTanggalPengukuran;

    private RequestQueue requestQueue;
    private ArrayList<Integer> pengukuranIds = new ArrayList<>();
    private ArrayList<String> pengukuranLabels = new ArrayList<>();

    private static final String BASE_URL = "http://192.168.1.11/GHW/api-apps/public/";

    private ExstensoDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_exstenso);

        dbHelper = new ExstensoDatabaseHelper(this);
        requestQueue = Volley.newRequestQueue(this);

        initViews();
        loadPengukuranList();

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

        cardPengukuran = findViewById(R.id.cardPengukuran);
        cardPembacaan = findViewById(R.id.cardPembacaan);
        cardDeformasi = findViewById(R.id.cardDeformasi);
        cardReadings = findViewById(R.id.cardReadings);

        containerPembacaan = findViewById(R.id.containerPembacaan);
        containerDeformasi = findViewById(R.id.containerDeformasi);
        containerReadings = findViewById(R.id.containerReadings);

        tvIdPengukuran = findViewById(R.id.tvIdPengukuran);
        tvTanggalPengukuran = findViewById(R.id.tvTanggalPengukuran);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void loadPengukuranList() {
        if (isOnline()) {
            String url = BASE_URL + "api/exstenso/pengukuran-eks";
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

            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT id_pengukuran, tanggal FROM t_pengukuran_eks ORDER BY tanggal DESC",
                    null);
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id_pengukuran"));
                    String tgl = cursor.getString(cursor.getColumnIndexOrThrow("tanggal"));
                    pengukuranIds.add(id);
                    pengukuranLabels.add("ID: " + id + " - " + tgl);
                } while (cursor.moveToNext());
            }
            cursor.close();

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
        String url = BASE_URL + "api/exstenso/detail/" + pengukuranId;
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
        Cursor cursorPengukuran = dbHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM t_pengukuran_eks WHERE id_pengukuran=?",
                new String[]{String.valueOf(pengukuranId)});
        if (cursorPengukuran.moveToFirst()) {
            cardPengukuran.setVisibility(View.VISIBLE);
            tvIdPengukuran.setText(String.valueOf(pengukuranId));
            tvTanggalPengukuran.setText(cursorPengukuran.getString(
                    cursorPengukuran.getColumnIndexOrThrow("tanggal")));
            Log.d(TAG, "✅ Data Pengukuran ditemukan");
        } else {
            Log.d(TAG, "❌ Data Pengukuran TIDAK ditemukan");
        }
        cursorPengukuran.close();

        // === Data Pembacaan ===
        showPembacaanData(pengukuranId);

        // === Data Deformasi ===
        showDeformasiData(pengukuranId);

        // === Data Readings ===
        showReadingsData(pengukuranId);

        // Debug: Cek visibility semua card
        Log.d(TAG, "📊 STATUS CARD - Pengukuran: " + (cardPengukuran.getVisibility() == View.VISIBLE) +
                ", Pembacaan: " + (cardPembacaan.getVisibility() == View.VISIBLE) +
                ", Deformasi: " + (cardDeformasi.getVisibility() == View.VISIBLE) +
                ", Readings: " + (cardReadings.getVisibility() == View.VISIBLE));

        // Show empty state jika tidak ada data sama sekali
        if (cardPengukuran.getVisibility() != View.VISIBLE &&
                cardPembacaan.getVisibility() != View.VISIBLE &&
                cardDeformasi.getVisibility() != View.VISIBLE &&
                cardReadings.getVisibility() != View.VISIBLE) {
            tvEmptyState.setVisibility(View.VISIBLE);
            Log.d(TAG, "📛 Menampilkan empty state");
        }
    }

    private void showPembacaanData(int pengukuranId) {
        String[] tabelPembacaan = {
                "t_pembacaan_ex1", "t_pembacaan_ex2", "t_pembacaan_ex3", "t_pembacaan_ex4"
        };

        boolean hasPembacaanData = false;
        containerPembacaan.removeAllViews();

        Log.d(TAG, "🔍 Mencari data Pembacaan untuk ID: " + pengukuranId);

        for (String tabel : tabelPembacaan) {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + tabel + " WHERE id_pengukuran=?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                hasPembacaanData = true;
                String sensorName = tabel.replace("t_pembacaan_", "Sensor ").toUpperCase();

                Log.d(TAG, "✅ Data ditemukan di " + tabel);

                // Header untuk sensor
                TextView header = createHeaderTextView(sensorName);
                containerPembacaan.addView(header);

                // Data pembacaan
                addDataRow(containerPembacaan, "Pembacaan 10",
                        getDoubleOrDash(cursor, "pembacaan_10"));
                addDataRow(containerPembacaan, "Pembacaan 20",
                        getDoubleOrDash(cursor, "pembacaan_20"));
                addDataRow(containerPembacaan, "Pembacaan 30",
                        getDoubleOrDash(cursor, "pembacaan_30"));

                // Separator
                containerPembacaan.addView(createSeparator());
            } else {
                Log.d(TAG, "❌ Tidak ada data di " + tabel);
            }
            cursor.close();
        }

        if (hasPembacaanData) {
            cardPembacaan.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card Pembacaan DITAMPILKAN");
        } else {
            Log.d(TAG, "📛 Tidak ada data pembacaan untuk ditampilkan");
        }
    }

    private void showDeformasiData(int pengukuranId) {
        String[] tabelDeformasi = {
                "p_deformasi_ex1", "p_deformasi_ex2", "p_deformasi_ex3", "p_deformasi_ex4"
        };

        boolean hasDeformasiData = false;
        containerDeformasi.removeAllViews();

        Log.d(TAG, "🔍 Mencari data Deformasi untuk ID: " + pengukuranId);

        for (String tabel : tabelDeformasi) {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + tabel + " WHERE id_pengukuran=?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                hasDeformasiData = true;
                String sensorName = tabel.replace("p_deformasi_", "Deformasi ").toUpperCase();

                Log.d(TAG, "✅ Data ditemukan di " + tabel);

                // Header untuk sensor
                TextView header = createHeaderTextView(sensorName);
                containerDeformasi.addView(header);

                // Data deformasi
                addDataRow(containerDeformasi, "Deformasi 10",
                        getDoubleOrDash(cursor, "deformasi_10"));
                addDataRow(containerDeformasi, "Deformasi 20",
                        getDoubleOrDash(cursor, "deformasi_20"));
                addDataRow(containerDeformasi, "Deformasi 30",
                        getDoubleOrDash(cursor, "deformasi_30"));
                addDataRow(containerDeformasi, "Pemb. Awal 10",
                        getDoubleOrDash(cursor, "pemb_awal10"));
                addDataRow(containerDeformasi, "Pemb. Awal 20",
                        getDoubleOrDash(cursor, "pemb_awal20"));
                addDataRow(containerDeformasi, "Pemb. Awal 30",
                        getDoubleOrDash(cursor, "pemb_awal30"));

                // Separator
                containerDeformasi.addView(createSeparator());
            } else {
                Log.d(TAG, "❌ Tidak ada data di " + tabel);
            }
            cursor.close();
        }

        if (hasDeformasiData) {
            cardDeformasi.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card Deformasi DITAMPILKAN");
        } else {
            Log.d(TAG, "📛 Tidak ada data deformasi untuk ditampilkan");
        }
    }

    private void showReadingsData(int pengukuranId) {
        String[] tabelReadings = {
                "i_readings_ex1", "i_readings_ex2", "i_readings_ex3", "i_readings_ex4"
        };

        boolean hasReadingsData = false;
        containerReadings.removeAllViews();

        Log.d(TAG, "🔍 Mencari data Readings untuk ID: " + pengukuranId);

        for (String tabel : tabelReadings) {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + tabel + " WHERE id_pengukuran=?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                hasReadingsData = true;
                String sensorName = tabel.replace("i_readings_", "Readings ").toUpperCase();

                Log.d(TAG, "✅ Data ditemukan di " + tabel);

                // Header untuk sensor
                TextView header = createHeaderTextView(sensorName);
                containerReadings.addView(header);

                // Data readings
                addDataRow(containerReadings, "Reading 10",
                        getDoubleOrDash(cursor, "reading_10"));
                addDataRow(containerReadings, "Reading 20",
                        getDoubleOrDash(cursor, "reading_20"));
                addDataRow(containerReadings, "Reading 30",
                        getDoubleOrDash(cursor, "reading_30"));

                // Separator
                containerReadings.addView(createSeparator());
            } else {
                Log.d(TAG, "❌ Tidak ada data di " + tabel);
            }
            cursor.close();
        }

        if (hasReadingsData) {
            cardReadings.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card Readings DITAMPILKAN");
        } else {
            Log.d(TAG, "📛 Tidak ada data readings untuk ditampilkan");
        }
    }

    // ============ Online JSON ============
    private void tampilkanDataJson(JSONObject data) throws Exception {
        hideAllCards();
        tvEmptyState.setVisibility(View.GONE);

        // Data Pengukuran
        if (data.has("pengukuran")) {
            cardPengukuran.setVisibility(View.VISIBLE);
            JSONObject pengukuran = data.getJSONObject("pengukuran");
            tvIdPengukuran.setText(pengukuran.optString("id_pengukuran", "--"));
            tvTanggalPengukuran.setText(pengukuran.optString("tanggal", "--"));
        }

        // Data Pembacaan
        if (data.has("pembacaan")) {
            cardPembacaan.setVisibility(View.VISIBLE);
            containerPembacaan.removeAllViews();
            JSONArray pembacaanArr = data.getJSONArray("pembacaan");

            for (int i = 0; i < pembacaanArr.length(); i++) {
                JSONObject pembacaan = pembacaanArr.getJSONObject(i);

                TextView header = createHeaderTextView("Sensor " + pembacaan.optString("sensor_name", "EX" + (i+1)));
                containerPembacaan.addView(header);

                addDataRow(containerPembacaan, "Pembacaan 10", pembacaan.optString("pembacaan_10", "--"));
                addDataRow(containerPembacaan, "Pembacaan 20", pembacaan.optString("pembacaan_20", "--"));
                addDataRow(containerPembacaan, "Pembacaan 30", pembacaan.optString("pembacaan_30", "--"));

                containerPembacaan.addView(createSeparator());
            }
        }

        // Data Deformasi
        if (data.has("deformasi")) {
            cardDeformasi.setVisibility(View.VISIBLE);
            containerDeformasi.removeAllViews();
            JSONArray deformasiArr = data.getJSONArray("deformasi");

            for (int i = 0; i < deformasiArr.length(); i++) {
                JSONObject deformasi = deformasiArr.getJSONObject(i);

                TextView header = createHeaderTextView("Deformasi " + deformasi.optString("sensor_name", "EX" + (i+1)));
                containerDeformasi.addView(header);

                addDataRow(containerDeformasi, "Deformasi 10", deformasi.optString("deformasi_10", "--"));
                addDataRow(containerDeformasi, "Deformasi 20", deformasi.optString("deformasi_20", "--"));
                addDataRow(containerDeformasi, "Deformasi 30", deformasi.optString("deformasi_30", "--"));
                addDataRow(containerDeformasi, "Pemb. Awal 10", deformasi.optString("pemb_awal10", "--"));
                addDataRow(containerDeformasi, "Pemb. Awal 20", deformasi.optString("pemb_awal20", "--"));
                addDataRow(containerDeformasi, "Pemb. Awal 30", deformasi.optString("pemb_awal30", "--"));

                containerDeformasi.addView(createSeparator());
            }
        }

        // Data Readings
        if (data.has("readings")) {
            cardReadings.setVisibility(View.VISIBLE);
            containerReadings.removeAllViews();
            JSONArray readingsArr = data.getJSONArray("readings");

            for (int i = 0; i < readingsArr.length(); i++) {
                JSONObject reading = readingsArr.getJSONObject(i);

                TextView header = createHeaderTextView("Readings " + reading.optString("sensor_name", "EX" + (i+1)));
                containerReadings.addView(header);

                addDataRow(containerReadings, "Reading 10", reading.optString("reading_10", "--"));
                addDataRow(containerReadings, "Reading 20", reading.optString("reading_20", "--"));
                addDataRow(containerReadings, "Reading 30", reading.optString("reading_30", "--"));

                containerReadings.addView(createSeparator());
            }
        }

        // Show empty state jika tidak ada data sama sekali
        if (cardPengukuran.getVisibility() != View.VISIBLE &&
                cardPembacaan.getVisibility() != View.VISIBLE &&
                cardDeformasi.getVisibility() != View.VISIBLE &&
                cardReadings.getVisibility() != View.VISIBLE) {
            tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    // ============ Export Database ============
    private void exportDatabaseToSQL() {
        try {
            File databasePath = getDatabasePath(ExstensoDatabaseHelper.DATABASE_NAME);

            if (!databasePath.exists()) {
                Toast.makeText(this, "Database tidak ditemukan", Toast.LENGTH_SHORT).show();
                return;
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "db_exstenso_export_" + timeStamp + ".sql";

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File exportFile = new File(downloadDir, fileName);

            exportDatabase(databasePath, exportFile);

            Toast.makeText(this, "Database berhasil diexport ke: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportDatabase(File databasePath, File exportFile) throws Exception {
        StringBuilder sqlContent = new StringBuilder();

        sqlContent.append("-- Database Export: ").append(new Date().toString()).append("\n");
        sqlContent.append("-- Database: ").append(ExstensoDatabaseHelper.DATABASE_NAME).append("\n\n");

        // Daftar semua tabel Exstenso
        String[] allTables = {
                "t_pengukuran_eks",
                "t_pembacaan_ex1", "t_pembacaan_ex2", "t_pembacaan_ex3", "t_pembacaan_ex4",
                "p_deformasi_ex1", "p_deformasi_ex2", "p_deformasi_ex3", "p_deformasi_ex4",
                "i_readings_ex1", "i_readings_ex2", "i_readings_ex3", "i_readings_ex4"
        };

        for (String tableName : allTables) {
            try {
                exportTableData(tableName, sqlContent);
            } catch (Exception e) {
                sqlContent.append("-- Error exporting table: ").append(tableName)
                        .append(" - ").append(e.getMessage()).append("\n\n");
            }
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(exportFile), StandardCharsets.UTF_8)) {
            writer.write(sqlContent.toString());
            writer.flush();
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

    // ============ UI UTILITY METHODS (UPDATED FOR NEW DESIGN) ============
    private void hideAllCards() {
        cardPengukuran.setVisibility(View.GONE);
        cardPembacaan.setVisibility(View.GONE);
        cardDeformasi.setVisibility(View.GONE);
        cardReadings.setVisibility(View.GONE);
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
        textView.setTextColor(getResources().getColor(R.color.colorPrimary));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setPadding(16, 12, 16, 12);
        textView.setBackgroundColor(0xFFE8F4FF); // Light blue background

        // Untuk corner radius, kita buat background drawable programmatically
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFFE8F4FF); // Light blue background
        background.setCornerRadius(16f); // Rounded corners 16dp
        background.setStroke(1, 0xFF0054A6); // Border with PLN blue
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
        textView.setTextColor(0xFF333333); // Dark gray
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
        textView.setTextColor(0xFF0054A6); // PLN Blue
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
        separator.setBackgroundColor(0xFFE0E0E0); // Light gray separator
        return separator;
    }

    private String getDoubleOrDash(Cursor c, String column) {
        try {
            int columnIndex = c.getColumnIndex(column);
            if (columnIndex == -1) {
                Log.d(TAG, "⚠️ Kolom " + column + " tidak ditemukan!");
                return "--";
            }
            if (c.isNull(columnIndex)) return "--";
            return String.valueOf(c.getDouble(columnIndex));
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getDoubleOrDash: " + e.getMessage());
            return "--";
        }
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