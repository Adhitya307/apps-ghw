package com.example.kerjapraktik;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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
    private Button btnPilihPengukuran, btnSubmitThomson, btnSubmitSR, btnSubmitBocoran;
    private EditText inputA1R, inputA1L, inputB1, inputB3, inputB5;
    private EditText inputElv624T1, inputElv615T2, inputPipaP1;
    private Spinner inputElv624T1Kode, inputElv615T2Kode, inputPipaP1Kode;

    private int pengukuranId = -1;
    private final int[] srKodeArray = {1, 40, 66, 68, 70, 79, 81, 83, 85, 92, 94, 96, 98, 100, 102, 104, 106};
    private final Map<Integer, Spinner> srKodeInputs = new HashMap<>();
    private final Map<Integer, EditText> srNilaiInputs = new HashMap<>();
    private final Map<String, Integer> tanggalToIdMap = new HashMap<>();

    private final String SERVER_URL = "http://192.168.72.30/kp_android/";

    // 🔥 variabel global sinkronisasi
    private boolean showSyncToast = false;
    private AtomicInteger globalCounter = new AtomicInteger(0);
    private int globalTotalData = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data2);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
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

        btnSubmitThomson = findViewById(R.id.btnSubmitThomson);
        btnSubmitSR = findViewById(R.id.btnSubmitSR);
        btnSubmitBocoran = findViewById(R.id.btnSubmitBocoran);

        // Initialize SR Spinners and EditTexts
        for (int kode : srKodeArray) {
            srKodeInputs.put(kode, findViewById(getResources().getIdentifier("sr_" + kode + "_kode", "id", getPackageName())));
            srNilaiInputs.put(kode, findViewById(getResources().getIdentifier("sr_" + kode + "_nilai", "id", getPackageName())));
        }

        // Setup spinners with S,M,L,E options
        setupSpinners();

        btnPilihPengukuran.setOnClickListener(v -> {
            String selected = (String) spinnerPengukuran.getSelectedItem();
            if (selected != null && tanggalToIdMap.containsKey(selected)) {
                pengukuranId = tanggalToIdMap.get(selected);
                showToast("ID terpilih: " + pengukuranId);
            }
        });

        btnSubmitThomson.setOnClickListener(v -> simpanAtauOffline("thomson", buatDataThomson()));
        btnSubmitSR.setOnClickListener(v -> simpanAtauOffline("sr", buatDataSR()));
        btnSubmitBocoran.setOnClickListener(v -> simpanAtauOffline("bocoran", buatDataBocoran()));
    }

    private void setupSpinners() {
        // Create array adapter for S,M,L,E options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.kode_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set adapter for all SR Kode spinners
        for (int kode : srKodeArray) {
            srKodeInputs.get(kode).setAdapter(adapter);
        }

        // Set adapter for Bocoran Kode spinners
        inputElv624T1Kode.setAdapter(adapter);
        inputElv615T2Kode.setAdapter(adapter);
        inputPipaP1Kode.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showSyncToast = false;
        globalCounter.set(0);
        globalTotalData = 0;

        if (isInternetAvailable()) {
            syncPengukuranMaster();

            // hitung semua data offline
            OfflineDataHelper db = new OfflineDataHelper(this);
            globalTotalData += db.getUnsyncedData("thomson").size();
            globalTotalData += db.getUnsyncedData("sr").size();
            globalTotalData += db.getUnsyncedData("bocoran").size();

            if (globalTotalData > 0) {
                showSyncToast = true;
            }

            // mulai sinkronisasi
            syncOfflineData("thomson");
            syncOfflineData("sr");
            syncOfflineData("bocoran");
        } else {
            loadTanggalOffline();
        }
    }

    private Map<String, String> buatDataThomson() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "thomson");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("a1_r", inputA1R.getText().toString());
        data.put("a1_l", inputA1L.getText().toString());
        data.put("b1", inputB1.getText().toString());
        data.put("b3", inputB3.getText().toString());
        data.put("b5", inputB5.getText().toString());
        return data;
    }

    private Map<String, String> buatDataSR() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "sr");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        for (int kode : srKodeArray) {
            data.put("sr_" + kode + "_kode", srKodeInputs.get(kode).getSelectedItem().toString());
            data.put("sr_" + kode + "_nilai", srNilaiInputs.get(kode).getText().toString());
        }
        return data;
    }

    private Map<String, String> buatDataBocoran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "bocoran");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("elv_624_t1", inputElv624T1.getText().toString());
        data.put("elv_624_t1_kode", inputElv624T1Kode.getSelectedItem().toString());
        data.put("elv_615_t2", inputElv615T2.getText().toString());
        data.put("elv_615_t2_kode", inputElv615T2Kode.getSelectedItem().toString());
        data.put("pipa_p1", inputPipaP1.getText().toString());
        data.put("pipa_p1_kode", inputPipaP1Kode.getSelectedItem().toString());
        return data;
    }

    private void simpanAtauOffline(String table, Map<String, String> dataMap) {
        if (pengukuranId == -1) {
            showToast("Pilih pengukuran terlebih dahulu!");
            return;
        }

        JSONObject json = new JSONObject(dataMap);
        if (!isInternetAvailable()) {
            String tempId = "local_" + System.currentTimeMillis();
            OfflineDataHelper db = new OfflineDataHelper(this);
            db.insertData(table, tempId, json.toString());
            showToast("Tidak ada internet. Data disimpan offline.");
        } else {
            kirimDataKeServer(dataMap, () -> showToast("Data berhasil dikirim."));
        }
    }

    private void kirimDataKeServer(Map<String, String> dataMap, Runnable onSuccess) {
        if (dataMap == null || dataMap.isEmpty()) return;

        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "insert_data.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                    if (postData.length() != 0) postData.append('&');
                    postData.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                }

                OutputStream os = conn.getOutputStream();
                os.write(postData.toString().getBytes());
                os.flush();
                os.close();

                if (conn.getResponseCode() == 200) {
                    runOnUiThread(onSuccess);
                }

            } catch (Exception e) {
                runOnUiThread(() -> showToast("Gagal kirim: " + e.getMessage()));
            }
        }).start();
    }

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

                    // ✅ hanya tampilkan sekali ketika semua data sudah sinkron
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

    private void syncPengukuranMaster() {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "get_pengukuran_ids.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray array = new JSONArray(sb.toString());
                OfflineDataHelper db = new OfflineDataHelper(this);
                db.clearPengukuranMaster();

                List<String> tanggalList = new ArrayList<>();
                tanggalToIdMap.clear();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
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

            } catch (Exception e) {
                runOnUiThread(() -> showToast("Gagal sync tanggal: " + e.getMessage()));
            }
        }).start();
    }

    private void loadTanggalOffline() {
        OfflineDataHelper db = new OfflineDataHelper(this);
        List<Map<String, String>> list = db.getPengukuranMaster();
        List<String> tanggalList = new ArrayList<>();
        tanggalToIdMap.clear();

        for (Map<String, String> row : list) {
            String tanggal = row.get("tanggal");
            int id = Integer.parseInt(row.get("id"));
            tanggalList.add(tanggal);
            tanggalToIdMap.put(tanggal, id);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(adapter);
    }

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo net = cm.getActiveNetworkInfo();
            return net != null && net.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }
}