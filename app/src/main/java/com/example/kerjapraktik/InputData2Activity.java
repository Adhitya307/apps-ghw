package com.example.kerjapraktik;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class InputData2Activity extends AppCompatActivity {

    private Spinner spinnerPengukuran;
    private Button btnPilihPengukuran, btnSubmitThomson, btnSubmitSR, btnSubmitBocoran, btnSubmitTmaWaduk;
    private EditText inputA1R, inputA1L, inputB1, inputB3, inputB5;
    private EditText inputElv624T1, inputElv615T2, inputPipaP1;
    private Spinner inputElv624T1Kode, inputElv615T2Kode, inputPipaP1Kode;
    private EditText inputTmaWaduk;

    // Tombol Hitung Semua Data
    private Button btnHitungSemua;

    private int pengukuranId = -1;
    private final int[] srKodeArray = {1, 40, 66, 68, 70, 79, 81, 83, 85, 92, 94, 96, 98, 100, 102, 104, 106};
    private final Map<Integer, Spinner> srKodeInputs = new HashMap<>();
    private final Map<Integer, EditText> srNilaiInputs = new HashMap<>();
    private final Map<String, Integer> tanggalToIdMap = new HashMap<>();

    // Endpoint utama untuk POST
    private final String SERVER_URL = "http://192.168.1.5/API_Android/public/rembesan/input";

    // Untuk cek-data & get_pengukuran
    private final String CEK_DATA_URL = "http://192.168.1.5/API_Android/public/rembesan/cek-data";
    private final String GET_PENGUKURAN_URL = "http://192.168.1.5/API_Android/public/rembesan/get_pengukuran";

    // URL untuk Hitung Semua Data
    private final String HITUNG_SEMUA_URL = "http://192.168.1.5/API_Android/public/rembesan/Rumus-Rembesan";

    // variabel sinkronisasi
    private boolean showSyncToast = false;
    private AtomicInteger globalCounter = new AtomicInteger(0);
    private int globalTotalData = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data2);

        // bind UI
        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
        btnSubmitThomson = findViewById(R.id.btnSubmitThomson);
        btnSubmitSR = findViewById(R.id.btnSubmitSR);
        btnSubmitBocoran = findViewById(R.id.btnSubmitBocoran);
        btnSubmitTmaWaduk = findViewById(R.id.btnSubmitTmaWaduk);

        // Tombol Hitung Semua Data
        btnHitungSemua = findViewById(R.id.btnHitungSemua);

        inputA1R = findViewById(R.id.inputA1R);
        inputA1L = findViewById(R.id.inputA1L);
        inputB1 = findViewById(R.id.inputB1);
        inputB3 = findViewById(R.id.inputB3);
        inputB5 = findViewById(R.id.inputB5);

        inputElv624T1 = findViewById(R.id.inputElv624T1);
        inputElv615T2 = findViewById(R.id.inputElv615T2);
        inputPipaP1 = findViewById(R.id.inputPipaP1);
        inputElv624T1Kode = findViewById(R.id.inputElv624T1Kode);
        inputElv615T2Kode = findViewById(R.id.inputElv615T2Kode);
        inputPipaP1Kode = findViewById(R.id.inputPipaP1Kode);

        inputTmaWaduk = findViewById(R.id.inputTmaWaduk);

        // find SR fields by convention sr_{kode}_kode and sr_{kode}_nilai
        for (int kode : srKodeArray) {
            int kodeId = getResources().getIdentifier("sr_" + kode + "_kode", "id", getPackageName());
            int nilaiId = getResources().getIdentifier("sr_" + kode + "_nilai", "id", getPackageName());
            Spinner s = null;
            EditText e = null;
            try {
                s = findViewById(kodeId);
            } catch (Exception ignored) {}
            try {
                e = findViewById(nilaiId);
            } catch (Exception ignored) {}
            if (s != null) srKodeInputs.put(kode, s);
            if (e != null) srNilaiInputs.put(kode, e);
        }

        setupSpinners();

        btnPilihPengukuran.setOnClickListener(v -> {
            Object sel = spinnerPengukuran.getSelectedItem();
            if (sel == null) {
                showToast("Pilih tanggal pengukuran dulu.");
                return;
            }
            String selected = sel.toString();
            if (tanggalToIdMap.containsKey(selected)) {
                pengukuranId = tanggalToIdMap.get(selected);
                showToast("ID terpilih: " + pengukuranId);
            } else {
                showToast("Tanggal tidak dikenali, coba sinkron ulang.");
            }
        });

        btnSubmitThomson.setOnClickListener(v -> simpanAtauOffline("thomson", buatDataThomson()));
        btnSubmitSR.setOnClickListener(v -> simpanAtauOffline("sr", buatDataSR()));
        btnSubmitBocoran.setOnClickListener(v -> simpanAtauOffline("bocoran", buatDataBocoran()));
        // TMA harus dikirim sebagai mode = "pengukuran" sesuai backend
        btnSubmitTmaWaduk.setOnClickListener(v -> simpanAtauOffline("pengukuran", buatDataTma()));

        // Hitung semua data
        btnHitungSemua.setOnClickListener(v -> handleHitungSemua());
    }

    /**
     * Handler untuk tombol Hitung Semua Data
     * Akan mengirim permintaan ke server untuk menghitung semua data berdasarkan pengukuran_id
     */
    private void handleHitungSemua() {
        // Pastikan ada koneksi internet
        if (!isInternetAvailable()) {
            showToast("Tidak ada koneksi internet. Tidak dapat menghitung data.");
            return;
        }

        // Dapatkan pengukuran_id yang dipilih
        String selected = spinnerPengukuran.getSelectedItem() != null ? spinnerPengukuran.getSelectedItem().toString() : null;
        int selectedPengukuranId = -1;

        if (selected != null && tanggalToIdMap.containsKey(selected)) {
            selectedPengukuranId = tanggalToIdMap.get(selected);
        } else {
            // fallback: cek prefs atau variabel global
            if (pengukuranId != -1) {
                selectedPengukuranId = pengukuranId;
            } else {
                showToast("Pilih data pengukuran terlebih dahulu!");
                return;
            }
        }

        // BUAT VARIABLE FINAL COPY untuk digunakan dalam lambda
        final int finalPengukuranId = selectedPengukuranId;

        // Tampilkan progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Menghitung data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                // Buat koneksi ke server
                URL url = new URL(HITUNG_SEMUA_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                // Buat data JSON - GUNAKAN VARIABLE FINAL
                JSONObject jsonData = new JSONObject();
                jsonData.put("pengukuran_id", finalPengukuranId);

                // Kirim data
                OutputStream os = conn.getOutputStream();
                os.write(jsonData.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                // Dapatkan respons
                int responseCode = conn.getResponseCode();
                BufferedReader reader;
                if (responseCode == 200) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                // Parse response JSON
                JSONObject response = new JSONObject(sb.toString());
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                // Ambil data detail jika ada
                JSONObject data = response.optJSONObject("data");
                if (data != null) {
                    // Anda bisa mengekstrak detail perhitungan di sini jika diperlukan
                    JSONObject thomson = data.optJSONObject("thomson");
                    JSONObject sr = data.optJSONObject("sr");
                    JSONObject bocoran = data.optJSONObject("bocoran");
                    JSONObject batasmaksimal = data.optJSONObject("batasmaksimal");

                    // Log detail untuk debugging
                    Log.d("HITUNG_SEMUA", "Thomson: " + (thomson != null ? thomson.toString() : "null"));
                    Log.d("HITUNG_SEMUA", "SR: " + (sr != null ? sr.toString() : "null"));
                    Log.d("HITUNG_SEMUA", "Bocoran: " + (bocoran != null ? bocoran.toString() : "null"));
                }

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if ("success".equals(status)) {
                        showToast("Perhitungan berhasil: " + message);

                        // Optional: Tampilkan detail hasil perhitungan
                        if (data != null) {
                            JSONObject batasmaksimal = data.optJSONObject("batasmaksimal");
                            if (batasmaksimal != null && batasmaksimal.optBoolean("success", false)) {
                                double tmaWaduk = batasmaksimal.optDouble("tma_waduk", 0);
                                double batasMaksimal = batasmaksimal.optDouble("batas_maksimal", 0);
                                showToast("TMA Waduk: " + tmaWaduk + ", Batas Maksimal: " + batasMaksimal);
                            }
                        }
                    } else {
                        showToast("Gagal menghitung: " + message);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showToast("Error: " + e.getMessage());
                    Log.e("HITUNG_SEMUA", "Error", e);
                });
            }
        }).start();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.kode_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        for (Spinner s : srKodeInputs.values()) {
            s.setAdapter(adapter);
        }
        if (inputElv624T1Kode != null) inputElv624T1Kode.setAdapter(adapter);
        if (inputElv615T2Kode != null) inputElv615T2Kode.setAdapter(adapter);
        if (inputPipaP1Kode != null) inputPipaP1Kode.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showSyncToast = false;
        globalCounter.set(0);
        globalTotalData = 0;

        if (isInternetAvailable()) {
            syncPengukuranMaster();

            OfflineDataHelper db = new OfflineDataHelper(this);
            try {
                globalTotalData += db.getUnsyncedData("thomson").size();
                globalTotalData += db.getUnsyncedData("sr").size();
                globalTotalData += db.getUnsyncedData("bocoran").size();
                globalTotalData += db.getUnsyncedData("pengukuran").size();
            } catch (Exception ignored) {}

            if (globalTotalData > 0) showSyncToast = true;

            syncOfflineData("thomson");
            syncOfflineData("sr");
            syncOfflineData("bocoran");
            syncOfflineData("pengukuran");
        } else {
            loadTanggalOffline();
        }
    }

    /* ---------- Builders for request data ---------- */

    private Map<String, String> buatDataThomson() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "thomson");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("a1_r", safeText(inputA1R));
        data.put("a1_l", safeText(inputA1L));
        data.put("b1", safeText(inputB1));
        data.put("b3", safeText(inputB3));
        data.put("b5", safeText(inputB5));
        return data;
    }

    private Map<String, String> buatDataSR() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "sr");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        for (int kode : srKodeArray) {
            Spinner s = srKodeInputs.get(kode);
            EditText e = srNilaiInputs.get(kode);
            data.put("sr_" + kode + "_kode", s != null && s.getSelectedItem() != null ? s.getSelectedItem().toString() : "");
            data.put("sr_" + kode + "_nilai", e != null ? e.getText().toString().trim() : "");
        }
        return data;
    }

    private Map<String, String> buatDataBocoran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "bocoran");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("elv_624_t1", safeText(inputElv624T1));
        data.put("elv_624_t1_kode", inputElv624T1Kode != null && inputElv624T1Kode.getSelectedItem() != null ? inputElv624T1Kode.getSelectedItem().toString() : "");
        data.put("elv_615_t2", safeText(inputElv615T2));
        data.put("elv_615_t2_kode", inputElv615T2Kode != null && inputElv615T2Kode.getSelectedItem() != null ? inputElv615T2Kode.getSelectedItem().toString() : "");
        data.put("pipa_p1", safeText(inputPipaP1));
        data.put("pipa_p1_kode", inputPipaP1Kode != null && inputPipaP1Kode.getSelectedItem() != null ? inputPipaP1Kode.getSelectedItem().toString() : "");
        return data;
    }

    /** TMA builder: backend expects mode="pengukuran" for TMA update */
    private Map<String, String> buatDataTma() {
        Map<String, String> data = new HashMap<>();

        // Validasi ringan
        String tma = safeText(inputTmaWaduk);
        if (tma.isEmpty()) {
            showToast("Masukkan nilai TMA Waduk terlebih dahulu!");
            return null;
        }

        data.put("mode", "pengukuran");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("tma_waduk", tma);
        return data;
    }

    /* ---------- Save/send logic ---------- */

    private void simpanAtauOffline(String table, Map<String, String> dataMap) {
        if (dataMap == null) return; // builder bisa mengembalikan null saat validasi gagal

        if (pengukuranId == -1) {
            showToast("Pilih pengukuran terlebih dahulu!");
            return;
        }

        cekDanSimpanData(table, dataMap);
    }

    private void cekDanSimpanData(String table, Map<String, String> dataMap) {
        // Untuk pengukuran (TMA) langsung kirim atau simpan offline
        if ("pengukuran".equals(table)) {
            kirimLangsungAtauOffline(table, dataMap);
            return;
        }

        new Thread(() -> {
            try {
                URL urlCek = new URL(CEK_DATA_URL + "?pengukuran_id=" + pengukuranId);
                HttpURLConnection conn = (HttpURLConnection) urlCek.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

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
                        // Masih ada kolom kosong → boleh insert
                        kirimLangsungAtauOffline(table, dataMap);
                    } else {
                        // Data sudah ada & lengkap → block insert
                        runOnUiThread(() -> showToast("Data " + table + " sudah lengkap untuk pengukuran ini!"));
                    }
                    return;
                }

                // Jika belum ada sama sekali → lanjut kirim
                kirimLangsungAtauOffline(table, dataMap);

            } catch (Exception e) {
                runOnUiThread(() -> showToast("Gagal cek data: " + e.getMessage() + " — menyimpan secara offline/try-send"));
                kirimLangsungAtauOffline(table, dataMap);
            }
        }).start();
    }

    private void kirimLangsungAtauOffline(String table, Map<String, String> dataMap) {
        new Thread(() -> {
            try {
                if (!isInternetAvailable()) {
                    // simpan offline
                    JSONObject json = new JSONObject(dataMap);
                    String tempId = "local_" + System.currentTimeMillis();
                    OfflineDataHelper db = new OfflineDataHelper(this);
                    db.insertData(table, tempId, json.toString());
                    runOnUiThread(() -> showToast("Tidak ada internet. Data disimpan offline."));
                } else {
                    kirimDataKeServer(dataMap, () -> runOnUiThread(() -> showToast("Data berhasil dikirim.")));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showToast("Error saat menyimpan offline: " + e.getMessage()));
            }
        }).start();
    }

    private void kirimDataKeServer(Map<String, String> dataMap, Runnable onSuccess) {
        if (dataMap == null || dataMap.isEmpty()) return;

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(SERVER_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(15_000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                JSONObject json = new JSONObject();
                for (Map.Entry<String, String> e : dataMap.entrySet()) {
                    json.put(e.getKey(), e.getValue());
                }

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                BufferedReader br;
                if (code == HttpURLConnection.HTTP_OK) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                String status = resp.optString("status", "");
                String message = resp.optString("message", "");

                if ("success".equals(status)) {
                    // Jika server mengembalikan pengukuran_id (pada insert pengukuran), simpan ke local master
                    if (resp.has("pengukuran_id")) {
                        int newId = resp.optInt("pengukuran_id", -1);
                        if (newId != -1) {
                            // update local master DB & spinner (opsional)
                            OfflineDataHelper db = new OfflineDataHelper(this);
                            db.insertPengukuranMaster(newId, dataMap.getOrDefault("tanggal", ""));
                            // refresh daftar pengukuran dari server
                            syncPengukuranMaster();
                        }
                    }

                    runOnUiThread(onSuccess);

                } else {
                    final String msg = message.isEmpty() ? ("Kode: " + code) : message;
                    runOnUiThread(() -> showToast("Server error: " + msg));
                }

            } catch (Exception e) {
                String err = e.getMessage() == null ? e.toString() : e.getMessage();
                runOnUiThread(() -> showToast("Gagal kirim: " + err));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /* ---------- Offline sync worker ---------- */

    private void syncOfflineData(String tableName) {
        OfflineDataHelper db = new OfflineDataHelper(this);
        List<Map<String, String>> dataList = db.getUnsyncedData(tableName);

        if (dataList == null || dataList.isEmpty()) return;

        for (Map<String, String> item : dataList) {
            String tempId = item.get("temp_id");
            String jsonStr = item.get("json");

            try {
                JSONObject json = new JSONObject(jsonStr);
                Map<String, String> dataMap = new HashMap<>();
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    dataMap.put(key, json.getString(key));
                }

                kirimDataKeServer(dataMap, () -> {
                    db.deleteByTempId(tableName, tempId);

                    if (showSyncToast && globalCounter.incrementAndGet() == globalTotalData) {
                        runOnUiThread(() -> {
                            showToast("Sinkronisasi sukses");
                            showSyncToast = false;
                        });
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* ---------- Master pengukuran (spinner) ---------- */

    private void syncPengukuranMaster() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(GET_PENGUKURAN_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject resp = new JSONObject(sb.toString());
                if ("success".equals(resp.optString("status"))) {
                    JSONArray arr = resp.getJSONArray("data");
                    OfflineDataHelper db = new OfflineDataHelper(this);
                    db.clearPengukuranMaster();

                    final List<String> tanggalList = new ArrayList<>();
                    tanggalToIdMap.clear();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        int id = obj.getInt("id");
                        String tanggal = obj.getString("tanggal");
                        tanggalToIdMap.put(tanggal, id);
                        tanggalList.add(tanggal);
                        db.insertPengukuranMaster(id, tanggal);
                    }

                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerPengukuran.setAdapter(adapter);
                    });
                } else {
                    runOnUiThread(() -> showToast("Gagal ambil daftar pengukuran dari server."));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showToast("Gagal sync tanggal: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void loadTanggalOffline() {
        OfflineDataHelper db = new OfflineDataHelper(this);
        List<Map<String, String>> rows = db.getPengukuranMaster();
        List<String> tanggalList = new ArrayList<>();
        tanggalToIdMap.clear();

        if (rows != null) {
            for (Map<String, String> r : rows) {
                String tanggal = r.get("tanggal");
                String idStr = r.get("id");
                if (tanggal == null || idStr == null) continue;
                tanggalList.add(tanggal);
                try {
                    tanggalToIdMap.put(tanggal, Integer.parseInt(idStr));
                } catch (NumberFormatException ignored) {}
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(adapter);
    }

    /* ---------- Helpers ---------- */

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
}