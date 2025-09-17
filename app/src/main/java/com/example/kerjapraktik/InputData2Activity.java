
package com.example.kerjapraktik;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InputData2Activity.java
 *
 * Versi lengkap untuk menggantikan file lama.
 * Fitur:
 * - Offline save ke SQLite melalui OfflineDataHelper (insertData, getUnsyncedData, deleteByTempId, insertPengukuranMaster, getPengukuranMaster)
 * - Auto-sync saat onResume()
 * - Sinkronisasi serial per tabel (pengukuran, thomson, sr, bocoran)
 * - Logging rapi: TAG + kategori (INFO, WARN, ERROR, IDLE) agar Android client jelas membedakan
 * - Robust defensive checks agar tidak crash
 * - Penggunaan background thread untuk network / DB
 *
 * Catatan:
 * - Pastikan class OfflineDataHelper ada di project (sesuai file sebelumnya).
 * - Pastikan permission INTERNET ada di AndroidManifest.xml
 *
 * Penulis: ChatGPT (generated for user)
 * Tanggal: 2025-09-17
 */
public class InputData2Activity extends AppCompatActivity {

    private static final String TAG = "InputData2Activity";

    // API endpoints (sesuaikan jika perlu)
    private static final String BASE_URL = "http://192.168.1.28/API_Android/public/rembesan/";
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

        // initial load from server or offline master
        if (isInternetAvailable()) {
            logInfo("onCreate", "Internet available -> sync master pengukuran");
            syncPengukuranMaster();
        } else {
            logInfo("onCreate", "Offline -> load tanggal dari local master");
            loadTanggalOffline();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        logInfo("onResume", "onResume fired; checking offline data to sync...");
        // Reset counters
        syncCounter.set(0);
        syncTotal = 0;
        showSyncToast = false;

        // Count offline items
        try {
            syncTotal += offlineDb.getUnsyncedData("pengukuran").size();
            syncTotal += offlineDb.getUnsyncedData("thomson").size();
            syncTotal += offlineDb.getUnsyncedData("sr").size();
            syncTotal += offlineDb.getUnsyncedData("bocoran").size();
        } catch (Exception e) {
            logWarn("onResume", "Counting offline rows failed: " + e.getMessage());
        }

        if (syncTotal > 0) {
            showSyncToast = true;
            logInfo("onResume", "Found " + syncTotal + " offline rows to sync");
        }

        if (isInternetAvailable()) {
            // First refresh master, then sync offline data serially
            syncPengukuranMaster(() -> {
                // sync in sequence
                syncAllOfflineData(() -> {
                    if (showSyncToast) {
                        showToast("Sinkronisasi offline selesai");
                    }
                });
            });
        } else {
            loadTanggalOffline();
        }
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
                showToast("Pilih tanggal pengukuran dulu.");
                return;
            }
            String selected = sel.toString();
            if (tanggalToIdMap.containsKey(selected)) {
                pengukuranId = tanggalToIdMap.get(selected);
                prefs.edit().putInt("pengukuran_id", pengukuranId).apply();
                showToast("ID terpilih: " + pengukuranId);
                logInfo("btnPilih", "pengukuran selected = " + pengukuranId);
            } else {
                showToast("Tanggal tidak dikenali, coba sinkron ulang.");
                logWarn("btnPilih", "tanggal '" + selected + "' tidak ada di map");
            }
        });

        btnSubmitThomson.setOnClickListener(v -> {
            Map<String,String> m = buildThomsonData();
            if (m != null) simpanAtauOffline("thomson", m);
        });

        btnSubmitSR.setOnClickListener(v -> {
            Map<String,String> m = buildSRData();
            if (m != null) simpanAtauOffline("sr", m);
        });

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

    private Map<String,String> buildThomsonData() {
        if (pengukuranId == -1) {
            showToast("Pilih pengukuran terlebih dahulu.");
            return null;
        }
        Map<String,String> map = new HashMap<>();
        map.put("mode", "thomson");
        map.put("pengukuran_id", String.valueOf(pengukuranId));
        map.put("a1_r", safeText(inputA1R));
        map.put("a1_l", safeText(inputA1L));
        map.put("b1", safeText(inputB1));
        map.put("b3", safeText(inputB3));
        map.put("b5", safeText(inputB5));
        return map;
    }

    private Map<String,String> buildSRData() {
        if (pengukuranId == -1) {
            showToast("Pilih pengukuran terlebih dahulu.");
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
            showToast("Pilih pengukuran terlebih dahulu.");
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
            showToast("Masukkan nilai TMA Waduk terlebih dahulu.");
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
            // This should not happen for these flows, but guard anyway
            showToast("Pengukuran ID tidak tersedia. Pilih pengukuran terlebih dahulu.");
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
            showToast("Tidak ada internet. Data disimpan offline.");
        } catch (Exception e) {
            logError("saveOffline", "Gagal simpan offline: " + e.getMessage());
            showToast("Gagal menyimpan offline: " + e.getMessage());
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
                        final String msg = "Data " + table + " sudah lengkap untuk pengukuran ini.";
                        runOnUiThread(() -> showToast(msg));
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

                    runOnUiThread(() -> showToast("Data berhasil dikirim ke server"));
                } else if ("idle".equalsIgnoreCase(status)) {
                    // server returns idle when no data in request (not fatal)
                    logInfo("kirimDataKeServer", "Server returned IDLE: " + message);
                    runOnUiThread(() -> showToast("Server idle: " + message));
                } else {
                    logError("kirimDataKeServer", "Server returned error: " + message + " (code=" + code + ")");
                    runOnUiThread(() -> showToast("Server error: " + message));
                }

            } catch (Exception e) {
                logError("kirimDataKeServer", "Exception while sending: " + e.getMessage());
                // On network error -> save offline
                try {
                    saveOffline(table, dataMap);
                } catch (Exception ex) {
                    logError("kirimDataKeServer", "Also failed to save offline: " + ex.getMessage());
                    runOnUiThread(() -> showToast("Gagal kirim & gagal simpan offline: " + ex.getMessage()));
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /* ---------- Offline sync ---------- */

    private void syncAllOfflineData(@Nullable Runnable onComplete) {
        // Serial sync order similar to server expectation:
        // pengukuran -> tma updates, then thomson -> sr -> bocoran
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
                    runOnUiThread(() -> showToast("Gagal ambil daftar pengukuran dari server"));
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
                    showToast("Gagal ambil tanggal pengukuran: " + e.getMessage());
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
            showToast("Tidak ada koneksi internet. Tidak dapat menghitung data.");
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
            showToast("Pilih data pengukuran terlebih dahulu!");
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
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(15_000);
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
                JSONObject data = resp.optJSONObject("data");
                runOnUiThread(() -> {
                    pd.dismiss();
                    if (data != null) {
                        String statusServer = data.optString("status", "partial");
                        if ("success".equals(statusServer) || "partial".equals(statusServer)) {
                            showToast("Perhitungan selesai. Cek hasil di web.");
                        } else {
                            showToast("Gagal menghitung: " + data.optString("message", ""));
                        }
                    } else {
                        // In some implementations server may return top-level status
                        String status = resp.optString("status", "");
                        if ("success".equalsIgnoreCase(status)) {
                            showToast("Perhitungan selesai.");
                        } else {
                            showToast("Gagal menghitung: " + resp.optString("message", ""));
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    showToast("Error saat menghitung: " + e.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
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
