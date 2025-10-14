package com.example.kerjapraktik;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import android.app.AlertDialog;

public class InputData2Activity extends AppCompatActivity {

    private static final String TAG = "InputData2Activity";

    // API endpoints
    private static final String BASE_URL = "http://192.168.1.11/API_Android/public/rembesan/";
    private static final String SERVER_INPUT_URL = BASE_URL + "input";
    private static final String CEK_DATA_URL = BASE_URL + "cek-data";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "get_pengukuran";
    private static final String HITUNG_SEMUA_URL = BASE_URL + "Rumus-Rembesan";

    // UI
    private Spinner spinnerPengukuran;
    private Button btnPilihPengukuran, btnSubmitThomson, btnSubmitSR, btnSubmitBocoran, btnSubmitTmaWaduk, btnHitungSemua;
    private EditText inputA1R, inputA1L, inputB1, inputB3, inputB5;
    private EditText inputElv624T1, inputElv615T2, inputPipaP1, inputTmaWaduk;
    private Spinner inputElv624T1Kode, inputElv615T2Kode, inputPipaP1Kode;
    private Map<Integer, Spinner> srKodeSpinners = new HashMap<>();
    private Map<Integer, EditText> srNilaiFields = new HashMap<>();

    // Data
    private final int[] srKodeArray = {1,40,66,68,70,79,81,83,85,92,94,96,98,100,102,104,106};
    private final Map<String,Integer> tanggalToIdMap = new LinkedHashMap<>();
    private int pengukuranId = -1;
    private String tempIdForCurrentPengukuran = null;

    // Offline DB helper
    private OfflineDataHelper offlineDb;

    // Sync control
    private final AtomicInteger syncCounter = new AtomicInteger(0);
    private int syncTotal = 0;
    private boolean showSyncToast = false;

    // Preferences
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data2);

        offlineDb = new OfflineDataHelper(this);
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        bindViews();
        setupSpinners();
        setupClickHandlers();

        // Sembunyikan field yang tidak diperlukan di HP 2 (Gallery)
        hideUnnecessaryFieldsHP2();

        // initial load from server or offline master
        if (isInternetAvailable()) {
            logInfo("onCreate", "Internet available -> sync master pengukuran");
            syncPengukuranMaster();
        } else {
            logInfo("onCreate", "Offline -> load tanggal dari local master");
            loadTanggalOffline();
        }
    }

    // METHOD UNTUK HP 2 (GALLERY): Sembunyikan field yang tidak diperlukan
    private void hideUnnecessaryFieldsHP2() {
        try {
            Log.i("InputData2Activity", "hideUnnecessaryFieldsHP2 - Memulai penyembunyian field untuk HP 2 (Gallery)");

            // === SEMBUNYIKAN B3 dan B5 (hanya untuk Stilling Basin/HP 1) ===
            TextInputLayout b3Layout = findViewById(R.id.b3_layout);
            TextInputLayout b5Layout = findViewById(R.id.b5_layout);

            if (b3Layout != null) {
                b3Layout.setVisibility(View.GONE);
                Log.i("InputData2Activity", "hideUnnecessaryFieldsHP2 - B3 layout disembunyikan");
            } else {
                Log.w("InputData2Activity", "hideUnnecessaryFieldsHP2 - B3 layout tidak ditemukan");
            }

            if (b5Layout != null) {
                b5Layout.setVisibility(View.GONE);
                Log.i("InputData2Activity", "hideUnnecessaryFieldsHP2 - B5 layout disembunyikan");
            } else {
                Log.w("InputData2Activity", "hideUnnecessaryFieldsHP2 - B5 layout tidak ditemukan");
            }

            // === UPDATE JUDUL DAN TOMBOL THOMSON UNTUK HP 2 (GALLERY) ===
            TextView thomsonTitle = findViewById(R.id.thomson_title);
            if (thomsonTitle != null) {
                thomsonTitle.setText("Thomson Weir - GALLERY (A1 R, A1 L, B1)");
                Log.i("InputData2Activity", "hideUnnecessaryFieldsHP2 - Judul Thomson diupdate untuk Gallery");
            }

            if (btnSubmitThomson != null) {
                btnSubmitThomson.setText("Simpan Thomson - Gallery");
                Log.i("InputData2Activity", "hideUnnecessaryFieldsHP2 - Tombol Thomson diupdate untuk Gallery");
            }

            Log.i("InputData2Activity", "hideUnnecessaryFieldsHP2 - Penyembunyian field untuk HP 2 (Gallery) selesai");

        } catch (Exception e) {
            Log.e("InputData2Activity", "hideUnnecessaryFieldsHP2 - Error: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        logInfo("onResume", "onResume fired; checking offline data to sync...");

        syncCounter.set(0);
        syncTotal = 0;
        showSyncToast = false;

        try {
            syncTotal += offlineDb.getUnsyncedData("pengukuran").size();
            syncTotal += offlineDb.getUnsyncedData("thomson").size();
            syncTotal += offlineDb.getUnsyncedData("sr").size();
            syncTotal += offlineDb.getUnsyncedData("bocoran").size();
        } catch (Exception e) {
            logWarn("onResume", "Counting offline rows failed: " + e.getMessage());
        }

        if (syncTotal > 0) {
            logInfo("onResume", "Found " + syncTotal + " offline rows to sync");
        }

        if (isInternetAvailable()) {
            syncPengukuranMaster(() -> {
                syncAllOfflineData(() -> {
                    if (syncTotal > 0 && !isAlreadySynced()) {
                        showElegantToast("Sinkronisasi offline selesai", "success");
                        markAsSynced();
                    }
                });
            });
        } else {
            loadTanggalOffline();
        }
    }

    /** Cek apakah sudah pernah sinkron hari ini */
    private boolean isAlreadySynced() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String lastSyncDate = prefs.getString("last_sync_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return today.equals(lastSyncDate);
    }

    /** Tandai sudah sinkron hari ini */
    private void markAsSynced() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString("last_sync_date", today).apply();
    }

    private void bindViews() {
        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
        btnSubmitThomson = findViewById(R.id.btnSubmitThomson);
        btnSubmitSR = findViewById(R.id.btnSubmitSR);
        btnSubmitBocoran = findViewById(R.id.btnSubmitBocoran);
        btnSubmitTmaWaduk = findViewById(R.id.btnSubmitTmaWaduk);
        btnHitungSemua = findViewById(R.id.btnHitungSemua);

        inputA1R = findViewById(R.id.inputA1R);
        inputA1L = findViewById(R.id.inputA1L);
        inputB1 = findViewById(R.id.inputB1);
        inputB3 = findViewById(R.id.inputB3);
        inputB5 = findViewById(R.id.inputB5);

        inputElv624T1 = findViewById(R.id.inputElv624T1);
        inputElv615T2 = findViewById(R.id.inputElv615T2);
        inputPipaP1 = findViewById(R.id.inputPipaP1);
        inputTmaWaduk = findViewById(R.id.inputTmaWaduk);

        inputElv624T1Kode = findViewById(R.id.inputElv624T1Kode);
        inputElv615T2Kode = findViewById(R.id.inputElv615T2Kode);
        inputPipaP1Kode = findViewById(R.id.inputPipaP1Kode);

        for (int kode : srKodeArray) {
            int kodeRes = getResources().getIdentifier("sr_" + kode + "_kode", "id", getPackageName());
            int nilaiRes = getResources().getIdentifier("sr_" + kode + "_nilai", "id", getPackageName());
            try {
                Spinner sp = findViewById(kodeRes);
                if (sp != null) srKodeSpinners.put(kode, sp);
            } catch (Exception e) { /* ignore */ }
            try {
                EditText et = findViewById(nilaiRes);
                if (et != null) srNilaiFields.put(kode, et);
            } catch (Exception e) { /* ignore */ }
        }
    }

    private void setupSpinners() {
        try {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this, R.array.kode_options, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            for (Spinner s : srKodeSpinners.values()) {
                s.setAdapter(adapter);
            }
            if (inputElv624T1Kode != null) inputElv624T1Kode.setAdapter(adapter);
            if (inputElv615T2Kode != null) inputElv615T2Kode.setAdapter(adapter);
            if (inputPipaP1Kode != null) inputPipaP1Kode.setAdapter(adapter);
        } catch (Exception e) {
            logWarn("setupSpinners", "Gagal setup spinner: " + e.getMessage());
        }
    }

    private void setupClickHandlers() {
        btnPilihPengukuran.setOnClickListener(v -> {
            Object sel = spinnerPengukuran.getSelectedItem();
            if (sel == null) {
                showElegantToast("Pilih tanggal pengukuran dulu.", "warning");
                return;
            }

            String selected = sel.toString();
            if (tanggalToIdMap.containsKey(selected)) {
                pengukuranId = tanggalToIdMap.get(selected);
                prefs.edit().putInt("pengukuran_id", pengukuranId).apply();

                showElegantToast("Tanggal terpilih: " + selected, "success");
                logInfo("btnPilih", "tanggal pengukuran terpilih = " + selected);

            } else {
                showElegantToast("Tanggal tidak dikenali, coba sinkron ulang.", "error");
                logWarn("btnPilih", "tanggal '" + selected + "' tidak ada di map");
            }
        });

        btnSubmitThomson.setOnClickListener(v -> {
            Map<String,String> m = buildThomsonDataHP2();
            if (m != null) simpanAtauOffline("thomson", m);
        });

        // Tombol SR disembunyikan di HP 2, tapi tetap ada handler untuk safety
        if (btnSubmitSR != null) {
            btnSubmitSR.setOnClickListener(v -> {
                showElegantToast("Fitur SR tidak tersedia di perangkat ini", "info");
            });
        }

        btnSubmitBocoran.setOnClickListener(v -> {
            Map<String,String> m = buildBocoranData();
            if (m != null) simpanAtauOffline("bocoran", m);
        });

        btnSubmitTmaWaduk.setOnClickListener(v -> {
            Map<String,String> m = buildTmaData();
            if (m != null) simpanAtauOffline("pengukuran", m);
        });

        btnHitungSemua.setOnClickListener(v -> handleHitungSemua());
    }

    /* ---------- Builders ---------- */

    // METHOD UNTUK HP 2 (GALLERY): Build Thomson data khusus untuk Gallery (hanya A1 R, A1 L, B1)
    private Map<String,String> buildThomsonDataHP2() {
        if (pengukuranId == -1) {
            showElegantToast("Pilih pengukuran terlebih dahulu.", "warning");
            return null;
        }
        Map<String,String> map = new HashMap<>();
        map.put("mode", "thomson");
        map.put("pengukuran_id", String.valueOf(pengukuranId));

        // KIRIM A1 R, A1 L, B1 yang diinput di HP 2 (Gallery)
        map.put("a1_r", safeText(inputA1R));
        map.put("a1_l", safeText(inputA1L));
        map.put("b1", safeText(inputB1));

        // B3 dan B5 dikosongkan (karena diinput di HP 1 - Stilling Basin)
        map.put("b3", "");
        map.put("b5", "");

        return map;
    }

    private Map<String,String> buildSRData() {
        // Method ini tetap ada tapi tidak digunakan di HP 2
        if (pengukuranId == -1) {
            showElegantToast("Pilih pengukuran terlebih dahulu.", "warning");
            return null;
        }
        Map<String,String> map = new HashMap<>();
        map.put("mode", "sr");
        map.put("pengukuran_id", String.valueOf(pengukuranId));
        for (int kode : srKodeArray) {
            Spinner sp = srKodeSpinners.get(kode);
            EditText et = srNilaiFields.get(kode);
            map.put("sr_" + kode + "_kode", sp != null && sp.getSelectedItem() != null ? sp.getSelectedItem().toString() : "");
            map.put("sr_" + kode + "_nilai", et != null ? et.getText().toString().trim() : "");
        }
        return map;
    }

    private Map<String,String> buildBocoranData() {
        if (pengukuranId == -1) {
            showElegantToast("Pilih pengukuran terlebih dahulu.", "warning");
            return null;
        }
        Map<String,String> map = new HashMap<>();
        map.put("mode", "bocoran");
        map.put("pengukuran_id", String.valueOf(pengukuranId));
        map.put("elv_624_t1", safeText(inputElv624T1));
        map.put("elv_624_t1_kode", inputElv624T1Kode != null && inputElv624T1Kode.getSelectedItem() != null ? inputElv624T1Kode.getSelectedItem().toString() : "");
        map.put("elv_615_t2", safeText(inputElv615T2));
        map.put("elv_615_t2_kode", inputElv615T2Kode != null && inputElv615T2Kode.getSelectedItem() != null ? inputElv615T2Kode.getSelectedItem().toString() : "");
        map.put("pipa_p1", safeText(inputPipaP1));
        map.put("pipa_p1_kode", inputPipaP1Kode != null && inputPipaP1Kode.getSelectedItem() != null ? inputPipaP1Kode.getSelectedItem().toString() : "");
        return map;
    }

    private Map<String,String> buildTmaData() {
        String tma = safeText(inputTmaWaduk);
        if (tma.isEmpty()) {
            showElegantToast("Masukkan nilai TMA Waduk terlebih dahulu.", "warning");
            return null;
        }
        Map<String,String> map = new HashMap<>();
        map.put("mode", "pengukuran");
        map.put("pengukuran_id", String.valueOf(pengukuranId));
        map.put("tma_waduk", tma);
        return map;
    }

    /* ---------- Save / Offline logic ---------- */

    private void simpanAtauOffline(String table, Map<String,String> dataMap) {
        // Ensure pengukuran_id present (except when user creates pengukuran via modal)
        if (!dataMap.containsKey("pengukuran_id") || dataMap.get("pengukuran_id") == null || dataMap.get("pengukuran_id").isEmpty()) {
            showElegantToast("Pengukuran ID tidak tersedia. Pilih pengukuran terlebih dahulu.", "error");
            return;
        }

        // If online -> check server if need to insert; else save offline
        if (isInternetAvailable()) {
            cekDanSimpanData(table, dataMap);
        } else {
            // Save offline
            saveOffline(table, dataMap);
        }
    }

    private void saveOffline(String table, Map<String,String> dataMap) {
        try {
            JSONObject json = new JSONObject(dataMap);
            String tempId = "local_" + System.currentTimeMillis();
            offlineDb.insertData(table, tempId, json.toString());
            logInfo("saveOffline", "Disimpan offline ke tabel " + table + " tempId=" + tempId);
            showElegantToast("📱 Tidak ada internet. Data disimpan offline.", "warning");
        } catch (Exception e) {
            logError("saveOffline", "Gagal simpan offline: " + e.getMessage());
            showElegantToast("❌ Gagal menyimpan offline: " + e.getMessage(), "error");
        }
    }

    private void cekDanSimpanData(String table, Map<String,String> dataMap) {
        // For pengukuran (TMA) we can directly send
        if ("pengukuran".equals(table)) {
            kirimDataKeServer(table, dataMap, true);
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String urlStr = CEK_DATA_URL + "?pengukuran_id=" + dataMap.get("pengukuran_id");
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                JSONObject data = resp.has("data") ? resp.getJSONObject("data") : resp;

                boolean dataSudahAda = false;
                boolean dataLengkap = false;

                switch (table) {
                    case "thomson":
                        dataSudahAda = data.optBoolean("thomson_ada", false);
                        dataLengkap = data.optBoolean("thomson_lengkap", false);
                        break;
                    case "sr":
                        dataSudahAda = data.optBoolean("sr_ada", false);
                        break;
                    case "bocoran":
                        dataSudahAda = data.optBoolean("bocoran_ada", false);
                        break;
                }

                if (dataSudahAda) {
                    if ("thomson".equals(table) && !dataLengkap) {
                        // allowed to insert partial thomson
                        logInfo("cekDanSimpanData", "Thomson exists but incomplete -> will send");
                        kirimDataKeServer(table, dataMap, false);
                    } else {
                        final String msg = "ℹ️ Data " + table + " sudah lengkap untuk pengukuran ini.";
                        runOnUiThread(() -> showElegantToast(msg, "info"));
                        logInfo("cekDanSimpanData", msg);
                    }
                } else {
                    kirimDataKeServer(table, dataMap, false);
                }

            } catch (Exception e) {
                logWarn("cekDanSimpanData", "Gagal cek data: " + e.getMessage() + " -> Simpan offline sebagai fallback");
                // fallback: save offline
                saveOffline(table, dataMap);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void kirimDataKeServer(String table, Map<String,String> dataMap, boolean isPengukuran) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(SERVER_INPUT_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(15_000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");

                JSONObject json = new JSONObject();
                for (Map.Entry<String,String> e : dataMap.entrySet()) {
                    json.put(e.getKey(), e.getValue());
                }

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                String status = resp.optString("status", "");
                String message = resp.optString("message", "");

                if ("success".equalsIgnoreCase(status)) {
                    logInfo("kirimDataKeServer", "Server accepted data for table=" + table + " message=" + message);

                    // handle pengukuran insert return id
                    if (isPengukuran && resp.has("pengukuran_id")) {
                        int newId = resp.optInt("pengukuran_id", -1);
                        if (newId != -1) {
                            // store into local pengukuran master table
                            try {
                                OfflineDataHelper db = new OfflineDataHelper(this);
                                db.insertPengukuranMaster(newId, dataMap.getOrDefault("tanggal", ""));
                                logInfo("kirimDataKeServer", "Inserted pengukuran_master id=" + newId);
                                // refresh spinner list
                                syncPengukuranMaster(null);
                            } catch (Exception e) {
                                logWarn("kirimDataKeServer", "Gagal insert pengukuran_master: " + e.getMessage());
                            }
                        }
                    }

                    // ⬇️⬇️⬇️ PERBAIKAN: Toast yang lebih spesifik ⬇️⬇️⬇️
                    final String successMessage;
                    if ("thomson".equals(table)) {
                        successMessage = "✅ Data Thomson Weir berhasil disimpan";
                    } else if ("bocoran".equals(table)) {
                        successMessage = "✅ Data Bocoran berhasil disimpan";
                    } else if ("pengukuran".equals(table)) {
                        successMessage = "✅ Data TMA Waduk berhasil disimpan";
                    } else {
                        successMessage = "✅ Data berhasil disimpan";
                    }

                    runOnUiThread(() -> showElegantToast(successMessage, "success"));
                    // ⬆️⬆️⬆️ AKHIR PERBAIKAN ⬆️⬆️⬆️

                } else if ("idle".equalsIgnoreCase(status)) {
                    // server returns idle when no data in request (not fatal)
                    logInfo("kirimDataKeServer", "Server returned IDLE: " + message);
                    runOnUiThread(() -> showElegantToast("ℹ️ Server idle: " + message, "info"));
                } else {
                    logError("kirimDataKeServer", "Server returned error: " + message + " (code=" + code + ")");
                    runOnUiThread(() -> showElegantToast("❌ Server error: " + message, "error"));
                }

            } catch (Exception e) {
                logError("kirimDataKeServer", "Exception while sending: " + e.getMessage());
                // On network error -> save offline dengan pesan spesifik
                try {
                    // ⬇️⬇️⬇️ PERBAIKAN: Pesan offline yang lebih spesifik ⬇️⬇️⬇️
                    final String offlineMessage;
                    if ("thomson".equals(table)) {
                        offlineMessage = "📱 Data Thomson Weir (Gallery) disimpan offline";
                    } else if ("bocoran".equals(table)) {
                        offlineMessage = "📱 Data Bocoran disimpan offline";
                    } else if ("pengukuran".equals(table)) {
                        offlineMessage = "📱 Data TMA Waduk disimpan offline";
                    } else {
                        offlineMessage = "📱 Data disimpan offline";
                    }

                    // Simpan offline dengan pesan khusus
                    JSONObject json = new JSONObject(dataMap);
                    String tempId = "local_" + System.currentTimeMillis();
                    offlineDb.insertData(table, tempId, json.toString());
                    logInfo("kirimDataKeServer", "Disimpan offline ke tabel " + table + " tempId=" + tempId);
                    runOnUiThread(() -> showElegantToast(offlineMessage, "warning"));
                    // ⬆️⬆️⬆️ AKHIR PERBAIKAN ⬆️⬆️⬆️

                } catch (Exception ex) {
                    logError("kirimDataKeServer", "Also failed to save offline: " + ex.getMessage());
                    runOnUiThread(() -> showElegantToast("❌ Gagal kirim & gagal simpan offline: " + ex.getMessage(), "error"));
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /* ---------- Offline sync ---------- */

    private void syncAllOfflineData(@Nullable Runnable onComplete) {
        logInfo("syncAllOfflineData", "Starting offline sync sequence...");
        syncDataSerial("pengukuran", () ->
                syncDataSerial("thomson", () ->
                        syncDataSerial("sr", () ->
                                syncDataSerial("bocoran", () -> {
                                    logInfo("syncAllOfflineData", "All offline tables processed");
                                    if (onComplete != null) onComplete.run();
                                }))));
    }

    private void syncDataSerial(String tableName, Runnable next) {
        List<Map<String,String>> list;
        try {
            list = offlineDb.getUnsyncedData(tableName);
        } catch (Exception e) {
            logError("syncDataSerial", "Failed to read unsynced data for " + tableName + ": " + e.getMessage());
            if (next != null) next.run();
            return;
        }

        if (list == null || list.isEmpty()) {
            logInfo("syncDataSerial", "No unsynced rows for " + tableName);
            if (next != null) next.run();
            return;
        }

        syncDataItem(tableName, list, 0, next);
    }

    private void syncDataItem(String tableName, List<Map<String,String>> dataList, int index, Runnable onFinish) {
        if (index >= dataList.size()) {
            if (onFinish != null) onFinish.run();
            return;
        }

        Map<String,String> item = dataList.get(index);
        String tempId = item.get("temp_id");
        String jsonStr = item.get("json");

        if (jsonStr == null || jsonStr.isEmpty()) {
            logWarn("syncDataItem", "Empty json for tempId=" + tempId + " table=" + tableName + " -> deleting row");
            offlineDb.deleteByTempId(tableName, tempId);
            // continue with next
            syncDataItem(tableName, dataList, index + 1, onFinish);
            return;
        }

        try {
            JSONObject json = new JSONObject(jsonStr);
            Map<String,String> dataMap = new HashMap<>();
            Iterator<String> it = json.keys();
            while (it.hasNext()) {
                String k = it.next();
                dataMap.put(k, json.optString(k, ""));
            }

            // send
            HttpURLConnection conn = null;
            try {
                URL url = new URL(SERVER_INPUT_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(15_000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                String status = resp.optString("status", "");
                if ("success".equalsIgnoreCase(status)) {
                    offlineDb.deleteByTempId(tableName, tempId);
                    logInfo("syncDataItem", "Synced table=" + tableName + " tempId=" + tempId);
                    if (showSyncToast) {
                        int done = syncCounter.incrementAndGet();
                        logInfo("syncDataItem", "Progress: " + done + " / " + syncTotal);
                    }
                } else if ("idle".equalsIgnoreCase(status)) {
                    // treat idle as success (nothing to do)
                    offlineDb.deleteByTempId(tableName, tempId);
                    logInfo("syncDataItem", "Server returned idle for tempId=" + tempId + " -> row deleted");
                } else {
                    logWarn("syncDataItem", "Server returned non-success for tempId=" + tempId + ": " + resp.optString("message"));
                }
            } catch (Exception e) {
                logError("syncDataItem", "Failed send for tempId=" + tempId + " err=" + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        } catch (Exception e) {
            logError("syncDataItem", "JSON parse failed for tempId=" + tempId + " -> deleting row. err=" + e.getMessage());
            // corrupt row -> delete
            offlineDb.deleteByTempId(tableName, tempId);
        }

        // process next on UI thread to respect ordering and avoid deep recursion on worker thread
        runOnUiThread(() -> syncDataItem(tableName, dataList, index + 1, onFinish));
    }

    /* ---------- Pengukuran master (spinner) ---------- */

    private void syncPengukuranMaster() {
        syncPengukuranMaster(null);
    }

    private void syncPengukuranMaster(@Nullable Runnable onDone) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            List<String> tanggalList = new ArrayList<>();
            try {
                URL url = new URL(GET_PENGUKURAN_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                String status = resp.optString("status", "");
                if (!"success".equalsIgnoreCase(status) && !resp.has("data")) {
                    logWarn("syncPengukuranMaster", "Server returned non-success when fetching pengukuran: " + resp.optString("message"));
                    runOnUiThread(() -> showElegantToast("❌ Gagal ambil daftar pengukuran dari server", "error"));
                    if (onDone != null) onDone.run();
                    return;
                }

                JSONArray arr = resp.optJSONArray("data");
                if (arr == null) arr = new JSONArray();

                // Clear and store to local master
                OfflineDataHelper db = new OfflineDataHelper(this);
                db.clearPengukuranMaster();
                tanggalToIdMap.clear();

                Calendar cal = Calendar.getInstance();
                int bulanIni = cal.get(Calendar.MONTH) + 1;
                int tahunIni = cal.get(Calendar.YEAR);

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    int id = obj.optInt("id", -1);
                    String tanggal = obj.optString("tanggal", "");
                    if (id == -1 || tanggal.isEmpty()) continue;
                    String[] parts = tanggal.split("-");
                    if (parts.length >= 2) {
                        int tahunData = Integer.parseInt(parts[0]);
                        int bulanData = Integer.parseInt(parts[1]);
                        if (tahunData == tahunIni && bulanData == bulanIni) {
                            tanggalToIdMap.put(tanggal, id);
                            tanggalList.add(tanggal);
                            db.insertPengukuranMaster(id, tanggal);
                        }
                    }
                }

                if (tanggalList.isEmpty()) {
                    tanggalList.add("Belum ada pengukuran bulan ini");
                    pengukuranId = -1;
                }

                final List<String> finalTanggalList = tanggalList;
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, finalTanggalList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerPengukuran.setAdapter(adapter);
                    spinnerPengukuran.setSelection(0);
                });

            } catch (Exception e) {
                logError("syncPengukuranMaster", "Gagal sync pengukuran: " + e.getMessage());
                runOnUiThread(() -> {
                    showElegantToast("❌ Gagal ambil tanggal pengukuran: " + e.getMessage(), "error");
                    loadTanggalOffline();
                });
            } finally {
                if (conn != null) conn.disconnect();
                if (onDone != null) runOnUiThread(onDone);
            }
        }).start();
    }

    private void loadTanggalOffline() {
        try {
            OfflineDataHelper db = new OfflineDataHelper(this);
            List<Map<String,String>> rows = db.getPengukuranMaster();
            List<String> list = new ArrayList<>();
            tanggalToIdMap.clear();
            if (rows != null && !rows.isEmpty()) {
                for (Map<String,String> r : rows) {
                    String tanggal = r.get("tanggal");
                    String idStr = r.get("id");
                    if (tanggal != null && idStr != null) {
                        list.add(tanggal);
                        try {
                            tanggalToIdMap.put(tanggal, Integer.parseInt(idStr));
                        } catch (Exception ignored) {}
                    }
                }
            } else {
                list.add("Belum ada pengukuran (offline)");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPengukuran.setAdapter(adapter);
            spinnerPengukuran.setSelection(0);
        } catch (Exception e) {
            logError("loadTanggalOffline", "Error load offline master: " + e.getMessage());
        }
    }

    /* ---------- Hitung semua ---------- */

    private void handleHitungSemua() {
        if (!isInternetAvailable()) {
            showElegantToast("Tidak ada koneksi internet. Tidak dapat menghitung data.", "error");
            return;
        }

        String sel = spinnerPengukuran.getSelectedItem() != null ? spinnerPengukuran.getSelectedItem().toString() : null;
        int id = -1;
        if (sel != null && tanggalToIdMap.containsKey(sel)) {
            id = tanggalToIdMap.get(sel);
        } else {
            id = prefs.getInt("pengukuran_id", -1);
        }

        if (id == -1) {
            showElegantToast("Pilih data pengukuran terlebih dahulu!", "warning");
            return;
        }

        final int finalId = id;
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Menghitung data...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(HITUNG_SEMUA_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(300_000);
                conn.setReadTimeout(300_000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                JSONObject json = new JSONObject();
                json.put("pengukuran_id", finalId);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                runOnUiThread(() -> {
                    pd.dismiss();
                    try {
                        String status = resp.optString("status", "");
                        JSONObject messages = resp.optJSONObject("messages");
                        JSONObject data = resp.optJSONObject("data");
                        String tanggal = resp.optString("tanggal", "-");

                        // 🔹 Ringkasan pesan perhitungan
                        StringBuilder msgBuilder = new StringBuilder();
                        if (messages != null) {
                            Iterator<String> keys = messages.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                String value = messages.optString(key, "");
                                msgBuilder.append("• ").append(key).append(": ").append(value).append("\n");
                            }
                        }

                        // 🔹 Hasil Look Burt
                        String lookBurtInfo = "";
                        String statusKeterangan = "aman"; // default
                        if (data != null) {
                            String rembBendungan = data.optString("rembesan_bendungan", "-");
                            String rembPerM = data.optString("rembesan_per_m", "-");
                            String ket = data.optString("keterangan", "-");
                            lookBurtInfo = "\n💧 Analisa Look Burt:\n"
                                    + "  - Rembesan Bendungan: " + rembBendungan + "\n"
                                    + "  - Rembesan per M: " + rembPerM + "\n"
                                    + "  - Keterangan: " + ket;

                            // Tentukan status berdasarkan keterangan
                            if (ket.toLowerCase().contains("bahaya")) {
                                statusKeterangan = "danger";
                            } else if (ket.toLowerCase().contains("peringatan") || ket.toLowerCase().contains("waspada")) {
                                statusKeterangan = "warning";
                            } else {
                                statusKeterangan = "success";
                            }
                        }

                        // 🔹 Tentukan notifikasi akhir
                        if ("success".equalsIgnoreCase(status)) {
                            showCalculationResultDialog(" Perhitungan Berhasil",
                                    "Semua perhitungan berhasil untuk tanggal " + tanggal + lookBurtInfo,
                                    statusKeterangan, tanggal);
                        } else if ("partial_error".equalsIgnoreCase(status)) {
                            showCalculationResultDialog("⚠️ Perhitungan Sebagian Berhasil",
                                    "Beberapa perhitungan gagal:\n\n" + msgBuilder.toString() + lookBurtInfo,
                                    "warning", tanggal);
                        } else if ("error".equalsIgnoreCase(status)) {
                            showElegantToast("❌ Gagal menghitung: " + resp.optString("message", "Terjadi kesalahan"), "error");
                        } else {
                            showElegantToast("ℹ️ Respon tidak dikenal dari server", "info");
                        }

                    } catch (Exception e) {
                        showElegantToast("Error parsing hasil: " + e.getMessage(), "error");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    showElegantToast("Error saat menghitung: " + e.getMessage(), "error");
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void showCalculationResultDialog(String title, String message, String status, String tanggal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate custom layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_calculation_result, null);
        builder.setView(dialogView);

        // Initialize views
        TextView titleText = dialogView.findViewById(R.id.dialog_title);
        TextView messageText = dialogView.findViewById(R.id.dialog_message);
        TextView tanggalText = dialogView.findViewById(R.id.dialog_tanggal);
        ImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        Button okButton = dialogView.findViewById(R.id.dialog_button_ok);
        LinearLayout headerLayout = dialogView.findViewById(R.id.dialog_header);

        // Set data berdasarkan status
        int colorRes = getColorForStatus(status);
        int iconRes = getIconForStatus(status);

        titleText.setText(title);
        messageText.setText(message);
        tanggalText.setText("📅 Tanggal: " + tanggal);
        iconView.setImageResource(iconRes);

        // Gradient background untuk header
        headerLayout.setBackgroundColor(ContextCompat.getColor(this, colorRes));

        // Tambahkan elevation/shadow
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            headerLayout.setElevation(8f);
            okButton.setElevation(4f);
        }

        // Animasi icon
        iconView.setAlpha(0f);
        iconView.animate().alpha(1f).setDuration(500).start();

        // Style button dengan ripple effect
        okButton.setBackgroundColor(ContextCompat.getColor(this, colorRes));
        okButton.setTextColor(Color.WHITE);

        // Format message dengan styling
        String formattedMessage = formatMessageWithIcons(message);
        messageText.setText(formattedMessage);

        // Animasi dialog masuk
        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.8f);
        dialogView.setScaleY(0.8f);

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // Tampilkan dialog dulu baru animasi
        dialog.show();

        // Animasi dialog masuk
        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();

        okButton.setOnClickListener(v -> {
            // Animasi tombol ketika ditekan
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }).start();

            // Animasi dialog keluar
            dialogView.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(200)
                    .withEndAction(dialog::dismiss)
                    .start();
        });
    }

    // METHOD: Format pesan dengan icon dan styling
    private String formatMessageWithIcons(String message) {
        // Ganti keyword dengan icon
        String formatted = message
                .replace("Analisa Look Burt", "🔍 Analisa Look Burt")
                .replace("Rembesan Bendungan", "💧 Rembesan Bendungan")
                .replace("Rembesan per M", "📏 Rembesan per M")
                .replace("Keterangan:", "📋 Keterangan:")
                .replace("Berhasil", "✅ Berhasil")
                .replace("Gagal", "❌ Gagal")
                .replace("Aman", "🟢 Aman")
                .replace("Peringatan", "🟡 Peringatan")
                .replace("Bahaya", "🔴 Bahaya");

        return formatted;
    }

    // Method untuk toast elegan dengan improvement
    private void showElegantToast(String message, String type) {
        runOnUiThread(() -> {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast_custom,
                    (android.view.ViewGroup) findViewById(R.id.custom_toast_container));

            TextView text = layout.findViewById(R.id.custom_toast_text);
            ImageView icon = layout.findViewById(R.id.custom_toast_icon);
            CardView card = layout.findViewById(R.id.custom_toast_card);

            // Format pesan toast
            String formattedMessage = formatMessageWithIcons(message);
            text.setText(formattedMessage);

            // Set warna dan icon berdasarkan type
            int colorRes = getColorForStatus(type);
            int iconRes = getIconForStatus(type);

            card.setCardBackgroundColor(ContextCompat.getColor(this, colorRes));
            icon.setImageResource(iconRes);

            // Animasi toast
            card.setAlpha(0f);
            card.setScaleX(0.8f);
            card.setScaleY(0.8f);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);

            toast.show();

            // Animasi toast masuk
            card.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start();
        });
    }

    // Helper method untuk mendapatkan warna berdasarkan status
    private int getColorForStatus(String status) {
        switch (status.toLowerCase()) {
            case "success":
            case "aman":
                return R.color.pln_success; // Hijau PLN
            case "warning":
            case "peringatan":
                return R.color.pln_warning; // Kuning/Oranye
            case "error":
            case "danger":
            case "bahaya":
                return R.color.pln_danger; // Merah
            case "info":
            default:
                return R.color.pln_info; // Biru PLN
        }
    }

    // Helper method untuk mendapatkan icon berdasarkan status
    private int getIconForStatus(String status) {
        switch (status.toLowerCase()) {
            case "success":
            case "aman":
                return R.drawable.ic_success;
            case "warning":
            case "peringatan":
                return R.drawable.ic_warning;
            case "error":
            case "danger":
            case "bahaya":
                return R.drawable.ic_danger;
            case "info":
            default:
                return R.drawable.ic_info;
        }
    }

    /* ---------- Helpers & Logging ---------- */

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo net = cm.getActiveNetworkInfo();
            return net != null && net.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private String safeText(EditText et) {
        return et == null ? "" : et.getText().toString().trim();
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    // Logging helpers to distinguish categories:
    private void logInfo(String where, String msg) {
        Log.i(TAG, "[INFO][" + where + "] " + msg);
    }
    private void logWarn(String where, String msg) {
        Log.w(TAG, "[WARN][" + where + "] " + msg);
    }
    private void logError(String where, String msg) {
        Log.e(TAG, "[ERROR][" + where + "] " + msg);
    }
}