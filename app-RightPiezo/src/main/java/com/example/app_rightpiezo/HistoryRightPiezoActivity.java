package com.example.app_rightpiezo;

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

public class HistoryRightPiezoActivity extends AppCompatActivity {

    private static final String TAG = "HistoryRightPiezo";

    private Spinner spinnerPengukuran;
    private Button btnTampilkanData;
    private Button btnExportDB;
    private TextView tvEmptyState;

    // CardView
    private CardView cardPengukuran, cardIReading, cardTPembacaan, cardBPiezoMetrik, cardPerhitunganPsMetrik;

    // Containers
    private LinearLayout containerIReading, containerTPembacaan, containerBPiezoMetrik, containerPerhitunganPsMetrik;

    // TextViews untuk Pengukuran
    private TextView tvIdPengukuran, tvTanggalPengukuran, tvTahunPengukuran, tvTmaPengukuran;

    private RequestQueue requestQueue;
    private ArrayList<Integer> pengukuranIds = new ArrayList<>();
    private ArrayList<String> pengukuranLabels = new ArrayList<>();

    private static final String BASE_URL = "http://10.73.69.30/GHW/api-apps/public/api/rightpiezo/";

    private RightPiezoDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_rightpiezo);

        dbHelper = new RightPiezoDatabaseHelper(this);
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

        cardPengukuran = findViewById(R.id.cardPengukuran);
        cardIReading = findViewById(R.id.cardIReading);
        cardTPembacaan = findViewById(R.id.cardTPembacaan);
        cardBPiezoMetrik = findViewById(R.id.cardBPiezoMetrik);
        cardPerhitunganPsMetrik = findViewById(R.id.cardPerhitunganPsMetrik);

        containerIReading = findViewById(R.id.containerIReading);
        containerTPembacaan = findViewById(R.id.containerTPembacaan);
        containerBPiezoMetrik = findViewById(R.id.containerBPiezoMetrik);
        containerPerhitunganPsMetrik = findViewById(R.id.containerPerhitunganPsMetrik);

        tvIdPengukuran = findViewById(R.id.tvIdPengukuran);
        tvTanggalPengukuran = findViewById(R.id.tvTanggalPengukuran);
        tvTahunPengukuran = findViewById(R.id.tvTahunPengukuran);
        tvTmaPengukuran = findViewById(R.id.tvTmaPengukuran);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void loadPengukuranList() {
        if (isOnline()) {
            String url = BASE_URL + "pengukuran";
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

            List<T_pengukuran_rightpiez> pengukuranList = dbHelper.getAllPengukuran();
            for (T_pengukuran_rightpiez pengukuran : pengukuranList) {
                pengukuranIds.add(pengukuran.getId_pengukuran());
                pengukuranLabels.add("ID: " + pengukuran.getId_pengukuran() + " - " + pengukuran.getTanggal());
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
        List<T_pengukuran_rightpiez> pengukuranList = dbHelper.getAllPengukuran();
        T_pengukuran_rightpiez pengukuranData = null;
        for (T_pengukuran_rightpiez data : pengukuranList) {
            if (data.getId_pengukuran() == pengukuranId) {
                pengukuranData = data;
                break;
            }
        }

        if (pengukuranData != null) {
            cardPengukuran.setVisibility(View.VISIBLE);
            tvIdPengukuran.setText(String.valueOf(pengukuranData.getId_pengukuran()));
            tvTanggalPengukuran.setText(pengukuranData.getTanggal());
            tvTahunPengukuran.setText(String.valueOf(pengukuranData.getTahun()));
            tvTmaPengukuran.setText(String.valueOf(pengukuranData.getTma()));
            Log.d(TAG, "✅ Data Pengukuran ditemukan");
        } else {
            Log.d(TAG, "❌ Data Pengukuran TIDAK ditemukan");
        }



        // === Data T_Pembacaan ===
        showTPembacaanData(pengukuranId);

        // === Data B_Piezo_Metrik ===
        showBPiezoMetrikData(pengukuranId);

        // === Data Perhitungan Ps Metrik ===
        showPerhitunganPsMetrikData(pengukuranId);

        // Debug: Cek visibility semua card
        Log.d(TAG, "📊 STATUS CARD - Pengukuran: " + (cardPengukuran.getVisibility() == View.VISIBLE) +
                ", I_Reading: " + (cardIReading.getVisibility() == View.VISIBLE) +
                ", T_Pembacaan: " + (cardTPembacaan.getVisibility() == View.VISIBLE) +
                ", B_Piezo_Metrik: " + (cardBPiezoMetrik.getVisibility() == View.VISIBLE) +
                ", Perhitungan: " + (cardPerhitunganPsMetrik.getVisibility() == View.VISIBLE));

        // Show empty state jika tidak ada data sama sekali
        if (cardPengukuran.getVisibility() != View.VISIBLE &&
                cardIReading.getVisibility() != View.VISIBLE &&
                cardTPembacaan.getVisibility() != View.VISIBLE &&
                cardBPiezoMetrik.getVisibility() != View.VISIBLE &&
                cardPerhitunganPsMetrik.getVisibility() != View.VISIBLE) {
            tvEmptyState.setVisibility(View.VISIBLE);
            Log.d(TAG, "📛 Menampilkan empty state");
        }
    }



    private void showTPembacaanData(int pengukuranId) {
        boolean hasTPembacaanData = false;
        containerTPembacaan.removeAllViews();

        Log.d(TAG, "🔍 Mencari data T_Pembacaan untuk ID: " + pengukuranId);

        List<T_pembacaan> tPembacaanList = dbHelper.getTPembacaanByPengukuran(pengukuranId);

        if (!tPembacaanList.isEmpty()) {
            hasTPembacaanData = true;
            Log.d(TAG, "✅ Data T_Pembacaan ditemukan: " + tPembacaanList.size() + " records");

            for (T_pembacaan data : tPembacaanList) {
                // Header untuk lokasi
                TextView header = createHeaderTextView("Lokasi: " + data.getLokasi());
                containerTPembacaan.addView(header);

                // Data T_Pembacaan
                addDataRow(containerTPembacaan, "Feet",
                        data.getFeet() != null ? data.getFeet() : "--");
                addDataRow(containerTPembacaan, "Inch",
                        data.getInch() != null ? data.getInch() : "--");

                // Separator
                containerTPembacaan.addView(createSeparator());
            }
        } else {
            Log.d(TAG, "❌ Tidak ada data T_Pembacaan");
        }

        if (hasTPembacaanData) {
            cardTPembacaan.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card T_Pembacaan DITAMPILKAN");
        } else {
            Log.d(TAG, "📛 Tidak ada data T_Pembacaan untuk ditampilkan");
        }
    }

    private void showBPiezoMetrikData(int pengukuranId) {
        boolean hasBPiezoMetrikData = false;
        containerBPiezoMetrik.removeAllViews();

        Log.d(TAG, "🔍 Mencari data B_Piezo_Metrik untuk ID: " + pengukuranId);

        B_piezo_metrik data = dbHelper.getBPiezoMetrikByPengukuran(pengukuranId);

        if (data != null) {
            hasBPiezoMetrikData = true;
            Log.d(TAG, "✅ Data B_Piezo_Metrik ditemukan");

            // Header
            TextView header = createHeaderTextView("DATA B_PIEZO_METRIK");
            containerBPiezoMetrik.addView(header);

            // Data konversi
            addDataRow(containerBPiezoMetrik, "Feet Konversi",
                    String.valueOf(data.getFeet()));
            addDataRow(containerBPiezoMetrik, "Inch Konversi",
                    String.valueOf(data.getInch()));

            // Data readings
            addDataRow(containerBPiezoMetrik, "R-01", getDoubleOrDash(data.getR01()));
            addDataRow(containerBPiezoMetrik, "R-02", getDoubleOrDash(data.getR02()));
            addDataRow(containerBPiezoMetrik, "R-03", getDoubleOrDash(data.getR03()));
            addDataRow(containerBPiezoMetrik, "R-04", getDoubleOrDash(data.getR04()));
            addDataRow(containerBPiezoMetrik, "R-05", getDoubleOrDash(data.getR05()));
            addDataRow(containerBPiezoMetrik, "R-06", getDoubleOrDash(data.getR06()));
            addDataRow(containerBPiezoMetrik, "R-07", getDoubleOrDash(data.getR07()));
            addDataRow(containerBPiezoMetrik, "R-08", getDoubleOrDash(data.getR08()));
            addDataRow(containerBPiezoMetrik, "R-09", getDoubleOrDash(data.getR09()));
            addDataRow(containerBPiezoMetrik, "R-10", getDoubleOrDash(data.getR10()));
            addDataRow(containerBPiezoMetrik, "R-11", getDoubleOrDash(data.getR11()));
            addDataRow(containerBPiezoMetrik, "R-12", getDoubleOrDash(data.getR12()));
            addDataRow(containerBPiezoMetrik, "IPZ-01", getDoubleOrDash(data.getIPZ01()));
            addDataRow(containerBPiezoMetrik, "PZ-04", getDoubleOrDash(data.getPZ04()));

        } else {
            Log.d(TAG, "❌ Tidak ada data B_Piezo_Metrik");
        }

        if (hasBPiezoMetrikData) {
            cardBPiezoMetrik.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card B_Piezo_Metrik DITAMPILKAN");
        } else {
            Log.d(TAG, "📛 Tidak ada data B_Piezo_Metrik untuk ditampilkan");
        }
    }

    private void showPerhitunganPsMetrikData(int pengukuranId) {
        boolean hasPerhitunganData = false;
        containerPerhitunganPsMetrik.removeAllViews();

        Log.d(TAG, "🔍 Mencari data Perhitungan Ps Metrik untuk ID: " + pengukuranId);

        Perhitungan_t_psmetrik data = dbHelper.getPerhitunganPsMetrikByPengukuran(pengukuranId);

        if (data != null) {
            hasPerhitunganData = true;
            Log.d(TAG, "✅ Data Perhitungan Ps Metrik ditemukan");

            // Header
            TextView header = createHeaderTextView("DATA PERHITUNGAN PS METRIK");
            containerPerhitunganPsMetrik.addView(header);

            // Data perhitungan
            addDataRow(containerPerhitunganPsMetrik, "R-01", getDoubleOrDash(data.getR01()));
            addDataRow(containerPerhitunganPsMetrik, "R-02", getDoubleOrDash(data.getR02()));
            addDataRow(containerPerhitunganPsMetrik, "R-03", getDoubleOrDash(data.getR03()));
            addDataRow(containerPerhitunganPsMetrik, "R-04", getDoubleOrDash(data.getR04()));
            addDataRow(containerPerhitunganPsMetrik, "R-05", getDoubleOrDash(data.getR05()));
            addDataRow(containerPerhitunganPsMetrik, "R-06", getDoubleOrDash(data.getR06()));
            addDataRow(containerPerhitunganPsMetrik, "R-07", getDoubleOrDash(data.getR07()));
            addDataRow(containerPerhitunganPsMetrik, "R-08", getDoubleOrDash(data.getR08()));
            addDataRow(containerPerhitunganPsMetrik, "R-09", getDoubleOrDash(data.getR09()));
            addDataRow(containerPerhitunganPsMetrik, "R-10", getDoubleOrDash(data.getR10()));
            addDataRow(containerPerhitunganPsMetrik, "R-11", getDoubleOrDash(data.getR11()));
            addDataRow(containerPerhitunganPsMetrik, "R-12", getDoubleOrDash(data.getR12()));
            addDataRow(containerPerhitunganPsMetrik, "IPZ-01", getDoubleOrDash(data.getIPZ01()));
            addDataRow(containerPerhitunganPsMetrik, "PZ-04", getDoubleOrDash(data.getPZ04()));



        } else {
            Log.d(TAG, "❌ Tidak ada data Perhitungan Ps Metrik");
        }

        if (hasPerhitunganData) {
            cardPerhitunganPsMetrik.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Card Perhitungan Ps Metrik DITAMPILKAN");
        } else {
            Log.d(TAG, "📛 Tidak ada data Perhitungan Ps Metrik untuk ditampilkan");
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
            tvTmaPengukuran.setText(pengukuran.optString("tma", "--"));
            Log.d(TAG, "✅ Data Pengukuran ditemukan");
        }

        // Data T_Pembacaan
        if (data.has("t_pembacaan")) {
            cardTPembacaan.setVisibility(View.VISIBLE);
            containerTPembacaan.removeAllViews();
            JSONArray tPembacaanArr = data.getJSONArray("t_pembacaan");

            for (int i = 0; i < tPembacaanArr.length(); i++) {
                JSONObject tPembacaan = tPembacaanArr.getJSONObject(i);

                TextView header = createHeaderTextView("Lokasi: " + tPembacaan.optString("lokasi", "--"));
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

            addDataRow(containerBPiezoMetrik, "Feet Konversi", bPiezoMetrik.optString("feet", "--"));
            addDataRow(containerBPiezoMetrik, "Inch Konversi", bPiezoMetrik.optString("inch", "--"));

            // PERBAIKAN: Gunakan format "R-01" bukan "R_01"
            for (int i = 1; i <= 12; i++) {
                String rKey = "R-" + String.format("%02d", i); // Ganti underscore dengan dash
                String value = bPiezoMetrik.optString(rKey, "--");
                addDataRow(containerBPiezoMetrik, rKey, value);
                Log.d(TAG, "✅ B_Piezo - Added " + rKey + ": " + value);
            }

            // PERBAIKAN: Gunakan "IPZ-01" bukan "IPZ_01"
            addDataRow(containerBPiezoMetrik, "IPZ-01", bPiezoMetrik.optString("IPZ-01", "--"));
            addDataRow(containerBPiezoMetrik, "PZ-04", bPiezoMetrik.optString("PZ-04", "--"));

            Log.d(TAG, "✅ Data B_Piezo_Metrik ditampilkan - Child count: " + containerBPiezoMetrik.getChildCount());
        }

        // PERBAIKAN: Data Perhitungan Ps Metrik - DENGAN KEY FORMAT YANG BENAR
        if (data.has("perhitungan_t_psmetrik")) {
            Log.d(TAG, "✅ Data Perhitungan ditemukan dengan key: perhitungan_t_psmetrik");
            cardPerhitunganPsMetrik.setVisibility(View.VISIBLE);
            containerPerhitunganPsMetrik.removeAllViews();

            JSONObject perhitungan = data.getJSONObject("perhitungan_t_psmetrik");
            Log.d(TAG, "📊 Data Perhitungan: " + perhitungan.toString());

            TextView header = createHeaderTextView("DATA PERHITUNGAN PS METRIK");
            containerPerhitunganPsMetrik.addView(header);
            Log.d(TAG, "✅ Header Perhitungan ditambahkan");

            // PERBAIKAN: Gunakan format "R-01" bukan "R_01" - SAMA SEPERTI B_PIEZO_METRIK
            for (int i = 1; i <= 12; i++) {
                String rKey = "R-" + String.format("%02d", i); // Ganti underscore dengan dash
                String value = perhitungan.optString(rKey, "--");
                addDataRow(containerPerhitunganPsMetrik, rKey, value);
                Log.d(TAG, "✅ Perhitungan - Added " + rKey + ": " + value);
            }

            // PERBAIKAN: Gunakan "IPZ-01" bukan "IPZ_01" - SAMA SEPERTI B_PIEZO_METRIK
            addDataRow(containerPerhitunganPsMetrik, "IPZ-01", perhitungan.optString("IPZ-01", "--"));
            addDataRow(containerPerhitunganPsMetrik, "PZ-04", perhitungan.optString("PZ-04", "--"));
            Log.d(TAG, "✅ Added IPZ-01 & PZ-04");



            // DEBUG: Cek jumlah child views
            Log.d(TAG, "👶 Jumlah child views di containerPerhitunganPsMetrik: " + containerPerhitunganPsMetrik.getChildCount());
        } else {
            Log.d(TAG, "❌ Data Perhitungan Ps Metrik TIDAK ditemukan");
            Log.d(TAG, "Available keys: " + data.keys().toString());
        }

        // Debug: Cek visibility semua card
        Log.d(TAG, "📊 STATUS CARD ONLINE - Pengukuran: " + (cardPengukuran.getVisibility() == View.VISIBLE) +
                ", T_Pembacaan: " + (cardTPembacaan.getVisibility() == View.VISIBLE) +
                ", B_Piezo_Metrik: " + (cardBPiezoMetrik.getVisibility() == View.VISIBLE) +
                ", Perhitungan: " + (cardPerhitunganPsMetrik.getVisibility() == View.VISIBLE));

        // Show empty state jika tidak ada data sama sekali
        if (cardPengukuran.getVisibility() != View.VISIBLE &&
                cardTPembacaan.getVisibility() != View.VISIBLE &&
                cardBPiezoMetrik.getVisibility() != View.VISIBLE &&
                cardPerhitunganPsMetrik.getVisibility() != View.VISIBLE) {
            tvEmptyState.setVisibility(View.VISIBLE);
            Log.d(TAG, "📛 Menampilkan empty state untuk online mode");
        }
    }

    // ============ Export Database ============
    private void exportDatabaseToSQL() {
        try {
            // Panggil ini dulu untuk lihat tabel yang ada
            checkDatabaseTables();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "db_rightpiezo_export_" + timeStamp + ".sql";

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File exportFile = new File(downloadDir, fileName);

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            StringBuilder sqlContent = new StringBuilder();

            sqlContent.append("-- Database Export: ").append(new Date().toString()).append("\n");
            sqlContent.append("-- Database: RightPiezoDB\n");
            sqlContent.append("-- App: Right Piezometer\n\n");

            // COBA SEMUA KEMUNGKINAN NAMA TABEL
            String[][] possibleTables = {
                    // Format: {nama_yang_kita_pakai, nama_di_database}
                    {"t_pengukuran_rightpiez", "t_pengukuran_rightpiez"},
                    {"t_pengukuran_rightpiez", "T_pengukuran_rightpiez"},
                    {"t_pengukuran_rightpiez", "pengukuran"},
                    {"t_pengukuran_rightpiez", "t_pengukuran"},

                    {"i_reading_atas", "i_reading_atas"},
                    {"i_reading_atas", "I_reading_atas"},
                    {"i_reading_atas", "reading_atas"},
                    {"i_reading_atas", "i_reading"},

                    {"t_pembacaan", "t_pembacaan"},
                    {"t_pembacaan", "T_pembacaan"},
                    {"t_pembacaan", "pembacaan"},
                    {"t_pembacaan", "t_bacaan"},

                    {"b_piezo_metrik", "b_piezo_metrik"},
                    {"b_piezo_metrik", "B_piezo_metrik"},
                    {"b_piezo_metrik", "piezo_metrik"},
                    {"b_piezo_metrik", "b_metrik"},

                    {"perhitungan_t_psmetrik", "perhitungan_t_psmetrik"},
                    {"perhitungan_t_psmetrik", "Perhitungan_t_psmetrik"},
                    {"perhitungan_t_psmetrik", "perhitungan_psmetrik"},
                    {"perhitungan_t_psmetrik", "t_psmetrik"}
            };

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

    private void exportDatabase(File databasePath, File exportFile) throws Exception {
        StringBuilder sqlContent = new StringBuilder();

        sqlContent.append("-- Database Export: ").append(new Date().toString()).append("\n");
        sqlContent.append("-- Database: ").append(RightPiezoDatabaseHelper.DATABASE_NAME).append("\n\n");

        // Daftar semua tabel Right Piezometer
        String[] allTables = {
                "t_pengukuran_rightpiez",
                "i_reading_atas",
                "t_pembacaan",
                "b_piezo_metrik",
                "perhitungan_t_psmetrik"
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

    private void checkDatabaseTables() {
        try {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table'", null);

            Log.d(TAG, "=== TABEL YANG ADA DI DATABASE ===");
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                Log.d(TAG, "Table: " + tableName);
            }
            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error checking tables: " + e.getMessage());
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

    // ============ UI UTILITY METHODS ============
    private void hideAllCards() {
        cardPengukuran.setVisibility(View.GONE);
        cardIReading.setVisibility(View.GONE);
        cardTPembacaan.setVisibility(View.GONE);
        cardBPiezoMetrik.setVisibility(View.GONE);
        cardPerhitunganPsMetrik.setVisibility(View.GONE);
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