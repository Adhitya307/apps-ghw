package com.example.kerjapraktik;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;

public class InputDataActivity extends AppCompatActivity {

    private EditText inputTahun, inputBulan, inputPeriode, inputTanggal, inputTmaWaduk, inputCurahHujan;
    private EditText inputA1R, inputA1L, inputB1, inputB3, inputB5;
    private EditText inputElv624T1, inputElv615T2, inputPipaP1;
    private Button btnSubmitPengukuran, btnSubmitThomson, btnSubmitSR, btnSubmitBocoran;

    private String tempId = null;
    private int pengukuranId = -1;

    private final int[] srKodeArray = {1, 40, 66, 68, 70, 79, 81, 83, 85, 92, 94, 96, 98, 100, 102, 104, 106};

    private boolean isSyncInProgress = false;
    private boolean showSyncToast = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data);

        inputTahun = findViewById(R.id.inputTahun);
        inputBulan = findViewById(R.id.inputBulan);
        inputPeriode = findViewById(R.id.inputPeriode);
        inputTanggal = findViewById(R.id.inputTanggal);
        inputTmaWaduk = findViewById(R.id.inputTmaWaduk);
        inputCurahHujan = findViewById(R.id.inputCurahHujan);
        inputA1R = findViewById(R.id.inputA1R);
        inputA1L = findViewById(R.id.inputA1L);
        inputB1 = findViewById(R.id.inputB1);
        inputB3 = findViewById(R.id.inputB3);
        inputB5 = findViewById(R.id.inputB5);
        inputElv624T1 = findViewById(R.id.inputElv624T1);
        inputElv615T2 = findViewById(R.id.inputElv615T2);
        inputPipaP1 = findViewById(R.id.inputPipaP1);

        btnSubmitPengukuran = findViewById(R.id.btnSubmitPengukuran);
        btnSubmitThomson = findViewById(R.id.btnSubmitThomson);
        btnSubmitSR = findViewById(R.id.btnSubmitSR);
        btnSubmitBocoran = findViewById(R.id.btnSubmitBocoran);

        btnSubmitPengukuran.setOnClickListener(v -> handlePengukuran());
        btnSubmitThomson.setOnClickListener(v -> handleThomson());
        btnSubmitSR.setOnClickListener(v -> handleSR());
        btnSubmitBocoran.setOnClickListener(v -> handleBocoran());

        SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
        pengukuranId = prefs.getInt("pengukuran_id", -1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showSyncToast = false; // supaya tidak muncul toast saat buka awal
        if (isInternetAvailable() && !isSyncInProgress) {
            isSyncInProgress = true;
            syncAllOfflineData(() -> {
                isSyncInProgress = false;
                if (showSyncToast) {
                    runOnUiThread(() -> showToast("Sinkronisasi selesai"));
                }
            });
        }
    }



    private void handlePengukuran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "pengukuran");
        data.put("tahun", inputTahun.getText().toString());
        data.put("bulan", inputBulan.getText().toString());
        data.put("periode", inputPeriode.getText().toString());
        data.put("tanggal", inputTanggal.getText().toString());
        data.put("tma_waduk", inputTmaWaduk.getText().toString());
        data.put("curah_hujan", inputCurahHujan.getText().toString());

        if (isInternetAvailable()) {
            sendToServer(data, "pengukuran", true);
        } else {
            tempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", tempId);
            saveOffline("pengukuran", tempId, data);
        }
    }

    private void handleThomson() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "thomson");
        data.put("a1_r", inputA1R.getText().toString());
        data.put("a1_l", inputA1L.getText().toString());
        data.put("b1", inputB1.getText().toString());
        data.put("b3", inputB3.getText().toString());
        data.put("b5", inputB5.getText().toString());

        if (pengukuranId != -1) data.put("pengukuran_id", String.valueOf(pengukuranId));
        else if (tempId != null) data.put("temp_id", tempId);
        else {
            showToast("Isi data pengukuran terlebih dahulu!");
            return;
        }

        if (isInternetAvailable() && pengukuranId != -1) sendToServer(data, "thomson", false);
        else saveOffline("thomson", tempId, data);
    }

    private void handleSR() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "sr");
        for (int kode : srKodeArray) {
            data.put("sr_" + kode + "_kode", getTextFromId("sr_" + kode + "_kode"));
            data.put("sr_" + kode + "_nilai", getTextFromId("sr_" + kode + "_nilai"));
        }

        if (pengukuranId != -1) data.put("pengukuran_id", String.valueOf(pengukuranId));
        else if (tempId != null) data.put("temp_id", tempId);
        else {
            showToast("Isi data pengukuran terlebih dahulu!");
            return;
        }

        if (isInternetAvailable() && pengukuranId != -1) sendToServer(data, "sr", false);
        else saveOffline("sr", tempId, data);
    }

    private void handleBocoran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "bocoran");
        data.put("elv_624_t1", inputElv624T1.getText().toString());
        data.put("elv_615_t2", inputElv615T2.getText().toString());
        data.put("pipa_p1", inputPipaP1.getText().toString());

        if (pengukuranId != -1) data.put("pengukuran_id", String.valueOf(pengukuranId));
        else if (tempId != null) data.put("temp_id", tempId);
        else {
            showToast("Isi data pengukuran terlebih dahulu!");
            return;
        }

        if (isInternetAvailable() && pengukuranId != -1) sendToServer(data, "bocoran", false);
        else saveOffline("bocoran", tempId, data);
    }

    private void syncAllOfflineData(Runnable onComplete) {
        syncDataSerial("pengukuran", () ->
                syncDataSerial("thomson", () ->
                        syncDataSerial("sr", () ->
                                syncDataSerial("bocoran", onComplete)
                        )
                )
        );
    }


    private void syncDataSerial(String tableName, Runnable next) {
        OfflineDataHelper db = new OfflineDataHelper(this);
        List<Map<String, String>> dataList = db.getAllData(tableName);

        if (dataList.isEmpty()) {
            if (next != null) next.run();
            return;
        }

        syncDataItem(tableName, dataList, 0, db, () -> {
            if (showSyncToast) {
                runOnUiThread(() -> showToast("Sinkron " + tableName + " selesai"));
            }
            if (next != null) next.run();
        });
    }

    private void syncDataItem(String tableName, List<Map<String, String>> dataList, int index, OfflineDataHelper db, Runnable onFinish) {
        if (index >= dataList.size()) {
            onFinish.run();
            return;
        }

        Map<String, String> item = dataList.get(index);
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
            if (!dataMap.containsKey("pengukuran_id")) {
                dataMap.put("temp_id", tempId);
            }

            new Thread(() -> {
                try {
                    URL url = new URL("http://192.168.1.6/kp_android/insert_data.php");
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
                    os.flush(); os.close();

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        db.deleteByTempId(tableName, tempId);
                    }

                } catch (Exception e) {
                    // log or handle
                }

                // lanjut ke item berikutnya
                runOnUiThread(() -> syncDataItem(tableName, dataList, index + 1, db, onFinish));
            }).start();

        } catch (Exception e) {
            runOnUiThread(() -> syncDataItem(tableName, dataList, index + 1, db, onFinish));
        }
    }


    private void sendToServer(Map<String, String> dataMap, String table, boolean isPengukuran) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.1.6/kp_android/insert_data.php");
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
                os.flush(); os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                if (isPengukuran && response.has("pengukuran_id")) {
                    pengukuranId = response.getInt("pengukuran_id");
                    SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
                    prefs.edit().putInt("pengukuran_id", pengukuranId).apply();
                }

                runOnUiThread(() -> showToast(response.optString("message", "Berhasil")));
            } catch (Exception e) {
                runOnUiThread(() -> showToast("Gagal kirim: " + e.getMessage()));
            }
        }).start();
    }

    private void saveOffline(String table, String tempId, Map<String, String> data) {
        try {
            JSONObject json = new JSONObject(data);
            OfflineDataHelper db = new OfflineDataHelper(this);
            db.insertData(table, tempId, json.toString());
            showToast("Tidak ada internet. Data disimpan offline.");
        } catch (Exception e) {
            showToast("Gagal simpan offline: " + e.getMessage());
        }
    }

    private String getTextFromId(String idName) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        EditText et = findViewById(id);
        return et != null ? et.getText().toString() : "";
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo active = cm.getActiveNetworkInfo();
        return active != null && active.isConnected();
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }
}
