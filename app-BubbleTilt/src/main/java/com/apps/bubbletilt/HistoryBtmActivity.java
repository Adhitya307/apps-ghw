package com.apps.bubbletilt;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
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

public class HistoryBtmActivity extends AppCompatActivity {

    private static final String TAG = "HistoryBtmActivity";
    private Spinner spinnerPengukuran;
    private Button btnTampilkanData, btnExportDB;

    // CardView
    private CardView cardInfoPengukuran, cardBacaan, cardPerhitungan, cardScatter;

    // Info Pengukuran
    private TextView tvTahun, tvPeriode, tvTanggal, tvIDPengukuran;

    // Containers
    private LinearLayout containerBacaan, containerPerhitungan, containerScatter;

    private RequestQueue requestQueue;
    private ArrayList<Integer> pengukuranIds = new ArrayList<>();
    private ArrayList<String> pengukuranLabels = new ArrayList<>();

    // TEST URL - ganti dengan URL yang benar
    private static final String BASE_URL = "http://10.73.69.30/GHW/api-apps/public/api/btm/";

    private DatabaseHelperBtm dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_btm);

        Log.d(TAG, "🔄 HistoryBtmActivity started");

        dbHelper = new DatabaseHelperBtm(this);
        requestQueue = Volley.newRequestQueue(this);

        initViews();

        // Test koneksi dan database pertama kali
        testInitialSetup();

        loadPengukuranList();

        btnTampilkanData.setOnClickListener(v -> {
            int pos = spinnerPengukuran.getSelectedItemPosition();
            if (pos >= 0 && pos < pengukuranIds.size()) {
                int pengukuranId = pengukuranIds.get(pos);
                Log.d(TAG, "📊 Menampilkan data untuk ID: " + pengukuranId);

                // SELALU coba offline dulu, baru online sebagai fallback
                boolean hasLocalData = tampilkanDataOffline(pengukuranId);

                if (!hasLocalData && isOnline()) {
                    Log.d(TAG, "🌐 Data lokal tidak ada, mencoba online...");
                    Toast.makeText(this, "🔄 Mengambil data dari server...", Toast.LENGTH_SHORT).show();
                    tampilkanDataOnline(pengukuranId);
                } else if (!hasLocalData) {
                    Toast.makeText(this, "❌ Data tidak tersedia di perangkat", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Pilih data pengukuran terlebih dahulu!", Toast.LENGTH_SHORT).show();
            }
        });

        btnExportDB.setOnClickListener(v -> exportDatabaseToSQL());
    }

    private void testInitialSetup() {
        // Test database
        dbHelper.printDatabaseInfo();

        // Test URL connection
        testUrlConnection();
    }

    private void testUrlConnection() {
        if (!isOnline()) {
            Log.d(TAG, "📵 Mode offline - skip URL test");
            return;
        }

        String testUrl = BASE_URL + "pengukuran";
        Log.d(TAG, "🔗 Testing URL: " + testUrl);

        JsonObjectRequest testRequest = new JsonObjectRequest(Request.Method.GET, testUrl, null,
                response -> {
                    Log.d(TAG, "✅ SERVER CONNECTION SUCCESS");
                    Toast.makeText(this, "✅ Terhubung ke server", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e(TAG, "❌ SERVER CONNECTION FAILED: " + error.getMessage());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "📡 Error code: " + error.networkResponse.statusCode);
                    }
                    Toast.makeText(this, "❌ Server tidak dapat diakses", Toast.LENGTH_LONG).show();
                });

        requestQueue.add(testRequest);
    }

    private void initViews() {
        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnTampilkanData = findViewById(R.id.btnTampilkanData);
        btnExportDB = findViewById(R.id.btnExportDB);

        cardInfoPengukuran = findViewById(R.id.cardInfoPengukuran);
        cardBacaan = findViewById(R.id.cardBacaan);
        cardPerhitungan = findViewById(R.id.cardPerhitungan);
        cardScatter = findViewById(R.id.cardScatter);

        tvTahun = findViewById(R.id.tvTahun);
        tvPeriode = findViewById(R.id.tvPeriode);
        tvTanggal = findViewById(R.id.tvTanggal);
        tvIDPengukuran = findViewById(R.id.tvIDPengukuran);

        containerBacaan = findViewById(R.id.containerBacaan);
        containerPerhitungan = findViewById(R.id.containerPerhitungan);
        containerScatter = findViewById(R.id.containerScatter);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void loadPengukuranList() {
        // SELALU load dari lokal dulu untuk respon cepat
        loadFromLocal();

        // Jika online, sync dengan server di background
        if (isOnline()) {
            loadFromOnline();
        }
    }

    private void loadFromLocal() {
        pengukuranIds.clear();
        pengukuranLabels.clear();

        List<PengukuranBtmModel> pengukuranList = dbHelper.getAllPengukuran();

        if (pengukuranList.isEmpty()) {
            Log.d(TAG, "📭 Database lokal kosong");
            pengukuranLabels.add("Tidak ada data lokal");
        } else {
            for (PengukuranBtmModel pengukuran : pengukuranList) {
                pengukuranIds.add(pengukuran.getId_pengukuran());
                String label = String.format("ID %d - %s (%s)",
                        pengukuran.getId_pengukuran(),
                        pengukuran.getTanggal(),
                        pengukuran.getPeriode());
                pengukuranLabels.add(label);
            }
            Log.d(TAG, "📂 Data lokal ditemukan: " + pengukuranList.size() + " pengukuran");
        }

        updateSpinner();
    }

    private void loadFromOnline() {
        String url = BASE_URL + "pengukuran";
        Log.d(TAG, "🌐 Loading data online dari: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("data")) {
                            JSONArray data = response.getJSONArray("data");
                            Log.d(TAG, "✅ Data online diterima: " + data.length() + " records");

                            // Simpan data online ke lokal untuk caching
                            saveOnlinePengukuranToLocal(data);

                            // Refresh spinner dengan data terbaru
                            loadFromLocal();
                        } else {
                            Log.e(TAG, "❌ Format response tidak valid: tidak ada field 'data'");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing data online: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "❌ Error loading data online: " + error.getMessage());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "📡 Status code: " + error.networkResponse.statusCode);
                    }
                });
        requestQueue.add(request);
    }

    private void saveOnlinePengukuranToLocal(JSONArray data) {
        try {
            List<PengukuranBtmModel> pengukuranList = new ArrayList<>();

            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.getJSONObject(i);
                PengukuranBtmModel pm = new PengukuranBtmModel();
                pm.setId_pengukuran(obj.getInt("id_pengukuran"));
                pm.setTahun(obj.getInt("tahun"));
                pm.setPeriode(obj.getString("periode"));
                pm.setTanggal(obj.getString("tanggal"));
                pm.setTemp_id(obj.optString("temp_id", ""));
                pm.setCreated_at(obj.optString("created_at", ""));
                pm.setUpdated_at(obj.optString("updated_at", ""));
                pengukuranList.add(pm);
            }

            dbHelper.bulkInsertPengukuran(pengukuranList);
            Log.d(TAG, "💾 Data online disimpan ke lokal: " + pengukuranList.size() + " records");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving online data to local: " + e.getMessage());
        }
    }

    private void updateSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, pengukuranLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(spinnerAdapter);

        if (pengukuranLabels.isEmpty() || pengukuranLabels.get(0).equals("Tidak ada data lokal")) {
            spinnerPengukuran.setEnabled(false);
            btnTampilkanData.setEnabled(false);
        } else {
            spinnerPengukuran.setEnabled(true);
            btnTampilkanData.setEnabled(true);
        }
    }

    // ============ Tampilkan data online ============
    private void tampilkanDataOnline(int pengukuranId) {
        String url = BASE_URL + "detail/" + pengukuranId;
        Log.d(TAG, "🌐 Fetching data dari: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("data")) {
                            JSONObject data = response.getJSONObject("data");
                            Log.d(TAG, "✅ Data online berhasil di-load");

                            // Simpan ke lokal untuk caching
                            saveDetailDataToLocal(pengukuranId, data);

                            // Tampilkan data
                            tampilkanDataJson(data);
                        } else {
                            Log.e(TAG, "❌ Format response tidak valid: tidak ada field 'data'");
                            Toast.makeText(this, "Format data dari server tidak valid", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing data online: " + e.getMessage());
                        Toast.makeText(this, "Error memproses data dari server", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "❌ Error loading data online: " + error.getMessage());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "📡 Status code: " + error.networkResponse.statusCode);
                    }
                    Toast.makeText(this, "Gagal mengambil data dari server", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(request);
    }

    // ============ Tampilkan data offline ============
    private boolean tampilkanDataOffline(int pengukuranId) {
        Log.d(TAG, "📱 Mode offline - menampilkan data lokal untuk ID: " + pengukuranId);

        try {
            // Reset semua card
            cardInfoPengukuran.setVisibility(View.GONE);
            cardBacaan.setVisibility(View.GONE);
            cardPerhitungan.setVisibility(View.GONE);
            cardScatter.setVisibility(View.GONE);

            boolean hasData = false;

            // === Info Pengukuran ===
            PengukuranBtmModel pengukuran = dbHelper.getPengukuranById(pengukuranId);
            if (pengukuran != null) {
                hasData = true;
                cardInfoPengukuran.setVisibility(View.VISIBLE);
                tvTahun.setText("Tahun: " + (pengukuran.getTahun() != 0 ? String.valueOf(pengukuran.getTahun()) : "--"));
                tvPeriode.setText("Periode: " + (pengukuran.getPeriode() != null ? pengukuran.getPeriode() : "--"));
                tvTanggal.setText("Tanggal: " + (pengukuran.getTanggal() != null ? pengukuran.getTanggal() : "--"));
                tvIDPengukuran.setText("ID: " + pengukuran.getId_pengukuran());
                Log.d(TAG, "✅ Data pengukuran ditemukan");
            }

            // === Data Bacaan untuk semua BT ===
            containerBacaan.removeAllViews();
            boolean hasBacaanData = false;

            // GUNAKAN QUERY LANGSUNG UNTUK SEMUA BT
            displayBacaanFromDirectQuery(pengukuranId);

            if (containerBacaan.getChildCount() > 1) { // Ada header + minimal 1 data
                cardBacaan.setVisibility(View.VISIBLE);
                hasData = true;
                Log.d(TAG, "✅ Data bacaan ditampilkan");
            }

            // === Data Perhitungan ===
            containerPerhitungan.removeAllViews();
            displayPerhitunganFromDirectQuery(pengukuranId);

            if (containerPerhitungan.getChildCount() > 1) {
                cardPerhitungan.setVisibility(View.VISIBLE);
                hasData = true;
                Log.d(TAG, "✅ Data perhitungan ditampilkan");
            }

            // === Data Scatter ===
            containerScatter.removeAllViews();
            displayScatterFromDirectQuery(pengukuranId);

            if (containerScatter.getChildCount() > 1) {
                cardScatter.setVisibility(View.VISIBLE);
                hasData = true;
                Log.d(TAG, "✅ Data scatter ditampilkan");
            }

            if (hasData) {
                Toast.makeText(this, "📱 Data ditampilkan dari penyimpanan lokal", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Tidak ada data untuk pengukuran ini", Toast.LENGTH_LONG).show();
            }

            return hasData;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error dalam tampilkanDataOffline: " + e.getMessage(), e);
            Toast.makeText(this, "Error menampilkan data lokal", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // METHOD BARU: Ambil data bacaan langsung dari query
    private void displayBacaanFromDirectQuery(int pengukuranId) {
        String[] btTables = {"t_bacaan_bt_1", "t_bacaan_bt_2", "t_bacaan_bt_3", "t_bacaan_bt_4",
                "t_bacaan_bt_6", "t_bacaan_bt_7", "t_bacaan_bt_8"};

        String[] btNames = {"BT1", "BT2", "BT3", "BT4", "BT6", "BT7", "BT8"};

        for (int i = 0; i < btTables.length; i++) {
            String table = btTables[i];
            String btName = btNames[i];

            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT US_GP, US_Arah, TB_GP, TB_Arah FROM " + table + " WHERE id_pengukuran = ?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                double usGp = cursor.getDouble(cursor.getColumnIndexOrThrow("US_GP"));
                String usArah = cursor.getString(cursor.getColumnIndexOrThrow("US_Arah"));
                double tbGp = cursor.getDouble(cursor.getColumnIndexOrThrow("TB_GP"));
                String tbArah = cursor.getString(cursor.getColumnIndexOrThrow("TB_Arah"));

                Log.d(TAG, "✅ " + btName + " - US_GP:" + usGp + " TB_GP:" + tbGp);

                addBacaanRow(containerBacaan, btName, usGp, usArah, tbGp, tbArah);
            } else {
                Log.d(TAG, "❌ " + btName + " - Tidak ada data");
                // Tampilkan BT dengan data kosong
                addBacaanRow(containerBacaan, btName, 0, "-", 0, "-");
            }
            cursor.close();
        }
    }

    // METHOD BARU: Ambil data perhitungan langsung dari query
    private void displayPerhitunganFromDirectQuery(int pengukuranId) {
        String[] tables = {"p_bt_1", "p_bt_2", "p_bt_3", "p_bt_4", "p_bt_6", "p_bt_7", "p_bt_8"};
        String[] names = {"BT1", "BT2", "BT3", "BT4", "BT6", "BT7", "BT8"};

        for (int i = 0; i < tables.length; i++) {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT DMS, A_sec, B_sec FROM " + tables[i] + " WHERE id_pengukuran = ?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                String dms = cursor.getString(cursor.getColumnIndexOrThrow("DMS"));
                double aSec = cursor.getDouble(cursor.getColumnIndexOrThrow("A_sec"));
                double bSec = cursor.getDouble(cursor.getColumnIndexOrThrow("B_sec"));

                addPerhitunganRow(containerPerhitungan, names[i], dms, aSec, bSec);
            } else {
                addPerhitunganRow(containerPerhitungan, names[i], "-", 0, 0);
            }
            cursor.close();
        }
    }

    // METHOD BARU: Ambil data scatter langsung dari query
    private void displayScatterFromDirectQuery(int pengukuranId) {
        String[] tables = {"p_scatter_bt_1", "p_scatter_bt_2", "p_scatter_bt_3", "p_scatter_bt_4",
                "p_scatter_bt_6", "p_scatter_bt_7", "p_scatter_bt_8"};
        String[] names = {"BT1", "BT2", "BT3", "BT4", "BT6", "BT7", "BT8"};

        for (int i = 0; i < tables.length; i++) {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT Y_US, X_TB, Y_cum, X_cum FROM " + tables[i] + " WHERE id_pengukuran = ?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                double yUs = cursor.getDouble(cursor.getColumnIndexOrThrow("Y_US"));
                double xTb = cursor.getDouble(cursor.getColumnIndexOrThrow("X_TB"));
                double yCum = cursor.getDouble(cursor.getColumnIndexOrThrow("Y_cum"));
                double xCum = cursor.getDouble(cursor.getColumnIndexOrThrow("X_cum"));

                addScatterRow(containerScatter, names[i], yUs, xTb, yCum, xCum);
            } else {
                addScatterRow(containerScatter, names[i], 0, 0, 0, 0);
            }
            cursor.close();
        }
    }
    // Helper methods untuk menampilkan data
    private boolean checkAndDisplayBacaan(String btName, Object bacaan) {
        if (bacaan == null || !isValidBacaanData(bacaan)) {
            return false;
        }

        try {
            double usGp = 0, tbGp = 0;
            String usArah = "-", tbArah = "-";

            if (bacaan instanceof BacaanBt1Model) {
                BacaanBt1Model b = (BacaanBt1Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            } else if (bacaan instanceof BacaanBt2Model) {
                BacaanBt2Model b = (BacaanBt2Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            }
            // Lanjutkan untuk model lainnya...

            addBacaanRow(containerBacaan, btName, usGp, usArah, tbGp, tbArah);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error displaying bacaan " + btName + ": " + e.getMessage());
            return false;
        }
    }

    private boolean checkAndDisplayPerhitungan(String btName, Object perhitungan) {
        if (perhitungan == null || !isValidPerhitunganData(perhitungan)) {
            return false;
        }

        try {
            String dms = "-";
            double aSec = 0, bSec = 0;

            if (perhitungan instanceof PerhitunganBt1Model) {
                PerhitunganBt1Model p = (PerhitunganBt1Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            } else if (perhitungan instanceof PerhitunganBt2Model) {
                PerhitunganBt2Model p = (PerhitunganBt2Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            }
            // Lanjutkan untuk model lainnya...

            addPerhitunganRow(containerPerhitungan, btName, dms, aSec, bSec);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error displaying perhitungan " + btName + ": " + e.getMessage());
            return false;
        }
    }

    private boolean checkAndDisplayScatter(String btName, Object scatter) {
        if (scatter == null || !isValidScatterData(scatter)) {
            return false;
        }

        try {
            double yUs = 0, xTb = 0, yCum = 0, xCum = 0;

            if (scatter instanceof ScatterBt1Model) {
                ScatterBt1Model s = (ScatterBt1Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            } else if (scatter instanceof ScatterBt2Model) {
                ScatterBt2Model s = (ScatterBt2Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            }
            // Lanjutkan untuk model lainnya...

            addScatterRow(containerScatter, btName, yUs, xTb, yCum, xCum);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error displaying scatter " + btName + ": " + e.getMessage());
            return false;
        }
    }

    // Helper methods untuk validasi data - PERBAIKAN
    private boolean isValidBacaanData(Object bacaan) {
        if (bacaan == null) return false;

        try {
            double usGp = 0, tbGp = 0;
            String usArah = "", tbArah = "";

            if (bacaan instanceof BacaanBt1Model) {
                BacaanBt1Model b = (BacaanBt1Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            } else if (bacaan instanceof BacaanBt2Model) {
                BacaanBt2Model b = (BacaanBt2Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            } else if (bacaan instanceof BacaanBt3Model) {
                BacaanBt3Model b = (BacaanBt3Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            } else if (bacaan instanceof BacaanBt4Model) {
                BacaanBt4Model b = (BacaanBt4Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            } else if (bacaan instanceof BacaanBt6Model) {
                BacaanBt6Model b = (BacaanBt6Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            } else if (bacaan instanceof BacaanBt7Model) {
                BacaanBt7Model b = (BacaanBt7Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            } else if (bacaan instanceof BacaanBt8Model) {
                BacaanBt8Model b = (BacaanBt8Model) bacaan;
                usGp = b.getUS_GP();
                usArah = b.getUS_Arah();
                tbGp = b.getTB_GP();
                tbArah = b.getTB_Arah();
            }

            // PERBAIKAN: Jangan terlalu ketat dalam validasi
            // Cukup pastikan ada data yang tidak kosong
            boolean hasValidData = (usGp != 0 || tbGp != 0 ||
                    (usArah != null && !usArah.isEmpty()) ||
                    (tbArah != null && !tbArah.isEmpty()));

            Log.d(TAG, "🔍 Validasi Bacaan - US_GP:" + usGp + " TB_GP:" + tbGp +
                    " US_Arah:" + usArah + " TB_Arah:" + tbArah + " Valid:" + hasValidData);

            return hasValidData;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in isValidBacaanData: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidPerhitunganData(Object perhitungan) {
        if (perhitungan == null) return false;

        try {
            String dms = "";
            double aSec = 0, bSec = 0;

            if (perhitungan instanceof PerhitunganBt1Model) {
                PerhitunganBt1Model p = (PerhitunganBt1Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            } else if (perhitungan instanceof PerhitunganBt2Model) {
                PerhitunganBt2Model p = (PerhitunganBt2Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            } else if (perhitungan instanceof PerhitunganBt3Model) {
                PerhitunganBt3Model p = (PerhitunganBt3Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            } else if (perhitungan instanceof PerhitunganBt4Model) {
                PerhitunganBt4Model p = (PerhitunganBt4Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            } else if (perhitungan instanceof PerhitunganBt6Model) {
                PerhitunganBt6Model p = (PerhitunganBt6Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            } else if (perhitungan instanceof PerhitunganBt7Model) {
                PerhitunganBt7Model p = (PerhitunganBt7Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            } else if (perhitungan instanceof PerhitunganBt8Model) {
                PerhitunganBt8Model p = (PerhitunganBt8Model) perhitungan;
                dms = p.getDMS();
                aSec = p.getA_sec();
                bSec = p.getB_sec();
            }

            // PERBAIKAN: Validasi lebih longgar
            boolean hasValidData = (aSec != 0 || bSec != 0 ||
                    (dms != null && !dms.isEmpty() && !dms.equals("-")));

            Log.d(TAG, "🔍 Validasi Perhitungan - DMS:" + dms + " A_sec:" + aSec +
                    " B_sec:" + bSec + " Valid:" + hasValidData);

            return hasValidData;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in isValidPerhitunganData: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidScatterData(Object scatter) {
        if (scatter == null) return false;

        try {
            double yUs = 0, xTb = 0, yCum = 0, xCum = 0;

            if (scatter instanceof ScatterBt1Model) {
                ScatterBt1Model s = (ScatterBt1Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            } else if (scatter instanceof ScatterBt2Model) {
                ScatterBt2Model s = (ScatterBt2Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            } else if (scatter instanceof ScatterBt3Model) {
                ScatterBt3Model s = (ScatterBt3Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            } else if (scatter instanceof ScatterBt4Model) {
                ScatterBt4Model s = (ScatterBt4Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            } else if (scatter instanceof ScatterBt6Model) {
                ScatterBt6Model s = (ScatterBt6Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            } else if (scatter instanceof ScatterBt7Model) {
                ScatterBt7Model s = (ScatterBt7Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            } else if (scatter instanceof ScatterBt8Model) {
                ScatterBt8Model s = (ScatterBt8Model) scatter;
                yUs = s.getY_US();
                xTb = s.getX_TB();
                yCum = s.getY_cum();
                xCum = s.getX_cum();
            }

            // PERBAIKAN: Validasi lebih longgar
            boolean hasValidData = (yUs != 0 || xTb != 0 || yCum != 0 || xCum != 0);

            Log.d(TAG, "🔍 Validasi Scatter - Y_US:" + yUs + " X_TB:" + xTb +
                    " Y_cum:" + yCum + " X_cum:" + xCum + " Valid:" + hasValidData);

            return hasValidData;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in isValidScatterData: " + e.getMessage());
            return false;
        }
    }
    private void saveDetailDataToLocal(int pengukuranId, JSONObject data) {
        try {
            Log.d(TAG, "💾 Menyimpan data detail ke lokal untuk ID: " + pengukuranId);

            // Implementasi penyimpanan detail data ke database lokal
            // Anda perlu menyesuaikan dengan struktur API response Anda

        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving detail data to local: " + e.getMessage());
        }
    }

    // ============ Online JSON ============
    private void tampilkanDataJson(JSONObject data) throws Exception {
        // Reset semua card
        cardInfoPengukuran.setVisibility(View.GONE);
        cardBacaan.setVisibility(View.GONE);
        cardPerhitungan.setVisibility(View.GONE);
        cardScatter.setVisibility(View.GONE);

        boolean hasData = false;

        // Info Pengukuran
        if (data.has("pengukuran")) {
            cardInfoPengukuran.setVisibility(View.VISIBLE);
            JSONObject pengukuran = data.getJSONObject("pengukuran");
            tvTahun.setText("Tahun: " + pengukuran.optString("tahun", "--"));
            tvPeriode.setText("Periode: " + pengukuran.optString("periode", "--"));
            tvTanggal.setText("Tanggal: " + pengukuran.optString("tanggal", "--"));
            tvIDPengukuran.setText("ID: " + pengukuran.optString("id_pengukuran", "--"));
            hasData = true;
        }

        // Data Bacaan
        if (data.has("bacaan")) {
            cardBacaan.setVisibility(View.VISIBLE);
            containerBacaan.removeAllViews();
            JSONArray bacaanArr = data.getJSONArray("bacaan");
            for (int i = 0; i < bacaanArr.length(); i++) {
                JSONObject bacaan = bacaanArr.getJSONObject(i);
                String btName = bacaan.optString("nama", "BT" + (i + 1));
                double usGp = bacaan.optDouble("US_GP", 0);
                String usArah = bacaan.optString("US_Arah", "-");
                double tbGp = bacaan.optDouble("TB_GP", 0);
                String tbArah = bacaan.optString("TB_Arah", "-");

                addBacaanRow(containerBacaan, btName, usGp, usArah, tbGp, tbArah);
                hasData = true;
            }
        }

        // Data Perhitungan
        if (data.has("perhitungan")) {
            cardPerhitungan.setVisibility(View.VISIBLE);
            containerPerhitungan.removeAllViews();
            JSONArray perhitunganArr = data.getJSONArray("perhitungan");
            for (int i = 0; i < perhitunganArr.length(); i++) {
                JSONObject perhitungan = perhitunganArr.getJSONObject(i);
                String btName = perhitungan.optString("nama", "BT" + (i + 1));
                String dms = perhitungan.optString("DMS", "-");
                double aSec = perhitungan.optDouble("A_sec", 0);
                double bSec = perhitungan.optDouble("B_sec", 0);

                addPerhitunganRow(containerPerhitungan, btName, dms, aSec, bSec);
                hasData = true;
            }
        }

        // Data Scatter
        if (data.has("scatter")) {
            cardScatter.setVisibility(View.VISIBLE);
            containerScatter.removeAllViews();
            JSONArray scatterArr = data.getJSONArray("scatter");
            for (int i = 0; i < scatterArr.length(); i++) {
                JSONObject scatter = scatterArr.getJSONObject(i);
                String btName = scatter.optString("nama", "BT" + (i + 1));
                double yUs = scatter.optDouble("Y_US", 0);
                double xTb = scatter.optDouble("X_TB", 0);
                double yCum = scatter.optDouble("Y_cum", 0);
                double xCum = scatter.optDouble("X_cum", 0);

                addScatterRow(containerScatter, btName, yUs, xTb, yCum, xCum);
                hasData = true;
            }
        }

        if (hasData) {
            Toast.makeText(this, "🌐 Data ditampilkan dari server", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ Tidak ada data yang ditampilkan", Toast.LENGTH_SHORT).show();
        }
    }

    // ============ Export Database ============
    private void exportDatabaseToSQL() {
        try {
            // Gunakan metode export dari DatabaseHelperBtm atau implementasi custom
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "db_btm_export_" + timeStamp + ".sql";

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File exportFile = new File(downloadDir, fileName);

            // Export menggunakan metode yang sudah ada
            exportDatabaseToFile(exportFile);

            Toast.makeText(this, "Database BTM berhasil diexport ke: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportDatabaseToFile(File exportFile) throws Exception {
        StringBuilder sqlContent = new StringBuilder();

        sqlContent.append("-- BTM Database Export: ").append(new Date().toString()).append("\n");
        sqlContent.append("-- Database: ").append(DatabaseHelperBtm.DATABASE_NAME).append("\n\n");

        // Export semua tabel
        exportTableData("t_pengukuran_btm", sqlContent);

        // Export tabel bacaan
        for (int i = 1; i <= 8; i++) {
            if (i != 5) { // Skip BT5 jika tidak ada
                exportTableData("t_bacaan_bt_" + i, sqlContent);
            }
        }

        // Export tabel perhitungan
        for (int i = 1; i <= 8; i++) {
            if (i != 5) { // Skip BT5 jika tidak ada
                exportTableData("p_bt_" + i, sqlContent);
            }
        }

        // Export tabel scatter
        String[] scatterTables = {"p_scatter_bt_1", "p_scatter_bt_2", "p_scatter_bt_3", "p_scatter_bt_4",
                "p_scatter_bt_6", "p_scatter_bt_7", "p_scatter_bt_8"};
        for (String table : scatterTables) {
            exportTableData(table, sqlContent);
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
            // Cek apakah tabel exists
            cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{tableName});

            if (!cursor.moveToFirst()) {
                sqlContent.append("-- Table ").append(tableName).append(" does not exist\n\n");
                return;
            }
            cursor.close();

            // Dapatkan data tabel
            cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + tableName, null);

            sqlContent.append("-- ===========================================\n");
            sqlContent.append("-- Data untuk tabel: ").append(tableName).append("\n");
            sqlContent.append("-- ===========================================\n");

            int rowCount = 0;
            while (cursor.moveToNext()) {
                StringBuilder insertStatement = new StringBuilder();
                insertStatement.append("INSERT OR REPLACE INTO ").append(tableName).append(" VALUES(");

                // Tambahkan nilai
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    if (i > 0) {
                        insertStatement.append(", ");
                    }

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
                            insertStatement.append("NULL"); // Skip BLOB untuk SQL export
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

    // ============ UTILITY METHODS ============

    private void addBacaanRow(LinearLayout container, String btName, double usGp, String usArah, double tbGp, String tbArah) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);

        TextView tvNama = createDataTextView(btName, 1f);
        TextView tvUsGp = createDataTextView(String.format("%.2f", usGp), 1f);
        TextView tvUsArah = createDataTextView(usArah != null ? usArah : "-", 1f);
        TextView tvTbGp = createDataTextView(String.format("%.2f", tbGp), 1f);
        TextView tvTbArah = createDataTextView(tbArah != null ? tbArah : "-", 1f);

        // Add header for first row
        if (container.getChildCount() == 0) {
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setPadding(0, 8, 0, 8);
            headerRow.setBackgroundColor(0xFF0054A6); // Warna biru PLN

            headerRow.addView(createHeaderTextView("BT", 1f));
            headerRow.addView(createHeaderTextView("US GP", 1f));
            headerRow.addView(createHeaderTextView("US Arah", 1f));
            headerRow.addView(createHeaderTextView("TB GP", 1f));
            headerRow.addView(createHeaderTextView("TB Arah", 1f));

            container.addView(headerRow);
        }

        row.addView(tvNama);
        row.addView(tvUsGp);
        row.addView(tvUsArah);
        row.addView(tvTbGp);
        row.addView(tvTbArah);

        container.addView(row);
    }

    private void addPerhitunganRow(LinearLayout container, String btName, String dms, double aSec, double bSec) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);

        TextView tvNama = createDataTextView(btName, 1f);
        TextView tvDms = createDataTextView(dms != null ? dms : "-", 1.5f);
        TextView tvASec = createDataTextView(String.format("%.4f", aSec), 1f);
        TextView tvBSec = createDataTextView(String.format("%.4f", bSec), 1f);

        if (container.getChildCount() == 0) {
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setPadding(0, 8, 0, 8);
            headerRow.setBackgroundColor(0xFF0054A6); // Warna biru PLN

            headerRow.addView(createHeaderTextView("BT", 1f));
            headerRow.addView(createHeaderTextView("DMS", 1.5f));
            headerRow.addView(createHeaderTextView("A Sec", 1f));
            headerRow.addView(createHeaderTextView("B Sec", 1f));

            container.addView(headerRow);
        }

        row.addView(tvNama);
        row.addView(tvDms);
        row.addView(tvASec);
        row.addView(tvBSec);

        container.addView(row);
    }

    private void addScatterRow(LinearLayout container, String btName, double yUs, double xTb, double yCum, double xCum) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);

        TextView tvNama = createDataTextView(btName, 1f);
        TextView tvYUs = createDataTextView(String.format("%.4f", yUs), 1f);
        TextView tvXTb = createDataTextView(String.format("%.4f", xTb), 1f);
        TextView tvYCum = createDataTextView(String.format("%.4f", yCum), 1f);
        TextView tvXCum = createDataTextView(String.format("%.4f", xCum), 1f);

        if (container.getChildCount() == 0) {
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setPadding(0, 8, 0, 8);
            headerRow.setBackgroundColor(0xFF0054A6); // Warna biru PLN

            headerRow.addView(createHeaderTextView("BT", 1f));
            headerRow.addView(createHeaderTextView("Y US", 1f));
            headerRow.addView(createHeaderTextView("X TB", 1f));
            headerRow.addView(createHeaderTextView("Y Cum", 1f));
            headerRow.addView(createHeaderTextView("X Cum", 1f));

            container.addView(headerRow);
        }

        row.addView(tvNama);
        row.addView(tvYUs);
        row.addView(tvXTb);
        row.addView(tvYCum);
        row.addView(tvXCum);

        container.addView(row);
    }

    private TextView createDataTextView(String text, float weight) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        params.gravity = Gravity.CENTER;
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(12);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setPadding(4, 8, 4, 8);
        textView.setGravity(Gravity.CENTER);
        return textView;
    }

    private TextView createHeaderTextView(String text, float weight) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        params.gravity = Gravity.CENTER;
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(12);
        textView.setTextColor(0xFFFFFFFF); // Warna putih
        textView.setPadding(4, 8, 4, 8);
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        return textView;
    }

    // Method baru untuk debugging data - tambahkan di dalam class HistoryBtmActivity
    private void debugDatabaseData(int pengukuranId) {
        Log.d(TAG, "🐛 ====== DEBUG DATABASE untuk ID: " + pengukuranId + " =====");

        // Cek tabel bacaan
        String[] btTables = {"t_bacaan_bt_1", "t_bacaan_bt_2", "t_bacaan_bt_3", "t_bacaan_bt_4",
                "t_bacaan_bt_6", "t_bacaan_bt_7", "t_bacaan_bt_8"};

        for (String table : btTables) {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + table + " WHERE id_pengukuran = ?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                StringBuilder data = new StringBuilder();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String columnName = cursor.getColumnName(i);
                    String value = cursor.getString(i);
                    data.append(columnName).append(": ").append(value).append(" | ");
                }
                Log.d(TAG, "📊 " + table + " - ADA DATA: " + data.toString());
            } else {
                Log.d(TAG, "❌ " + table + " - TIDAK ADA DATA untuk pengukuran ID: " + pengukuranId);
            }
            cursor.close();
        }

        // Cek tabel perhitungan
        String[] perhitunganTables = {"p_bt_1", "p_bt_2", "p_bt_3", "p_bt_4", "p_bt_6", "p_bt_7", "p_bt_8"};
        for (String table : perhitunganTables) {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + table + " WHERE id_pengukuran = ?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                Log.d(TAG, "📈 " + table + " - ADA DATA perhitungan");
            } else {
                Log.d(TAG, "❌ " + table + " - TIDAK ADA DATA perhitungan");
            }
            cursor.close();
        }

        // Cek tabel scatter
        String[] scatterTables = {"p_scatter_bt_1", "p_scatter_bt_2", "p_scatter_bt_3", "p_scatter_bt_4",
                "p_scatter_bt_6", "p_scatter_bt_7", "p_scatter_bt_8"};
        for (String table : scatterTables) {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM " + table + " WHERE id_pengukuran = ?",
                    new String[]{String.valueOf(pengukuranId)});

            if (cursor.moveToFirst()) {
                Log.d(TAG, "📉 " + table + " - ADA DATA scatter");
            } else {
                Log.d(TAG, "❌ " + table + " - TIDAK ADA DATA scatter");
            }
            cursor.close();
        }

        Log.d(TAG, "🐛 ====== END DEBUG DATABASE =====");
    }
}