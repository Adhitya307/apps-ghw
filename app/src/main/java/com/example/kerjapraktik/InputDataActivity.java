package com.example.kerjapraktik;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class InputDataActivity extends AppCompatActivity {

    private EditText inputTahun, inputBulan, inputTanggal, inputTmaWaduk;
    private EditText inputA1R, inputA1L, inputB1, inputB3, inputB5;
    private EditText inputElv624T1, inputElv615T2, inputPipaP1;
    private AutoCompleteTextView inputPeriode;
    private Button btnSubmitPengukuran, btnSubmitThomson, btnSubmitSR, btnSubmitBocoran;
    private Calendar calendar;

    private String tempId = null;
    private int pengukuranId = -1;
    private final int[] srKodeArray = {1, 40, 66, 68, 70, 79, 81, 83, 85, 92, 94, 96, 98, 100, 102, 104, 106};
    private boolean isSyncInProgress = false;
    private Map<Integer, Spinner> srKodeSpinners = new HashMap<>();
    private Spinner elv624T1Kode, elv615T2Kode, pipaP1Kode;

    private OfflineDataHelper offlineDb;
    private SharedPreferences syncPrefs;

    // URL yang sudah diperbaiki
    private static final String BASE_URL = "http://10.0.2.2/API_Android/public/rembesan/";
    private static final String INSERT_DATA_URL = BASE_URL + "input";
    private static final String CEK_DATA_URL = BASE_URL + "cek-data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data);

        offlineDb = new OfflineDataHelper(this);
        syncPrefs = getSharedPreferences("sync_prefs", MODE_PRIVATE);
        calendar = Calendar.getInstance();

        // Initialize input fields
        inputTahun = findViewById(R.id.inputTahun);
        inputBulan = findViewById(R.id.inputBulan);
        inputPeriode = findViewById(R.id.inputPeriode);
        inputTanggal = findViewById(R.id.inputTanggal);
        inputTmaWaduk = findViewById(R.id.inputTmaWaduk);
        inputA1R = findViewById(R.id.inputA1R);
        inputA1L = findViewById(R.id.inputA1L);
        inputB1 = findViewById(R.id.inputB1);
        inputB3 = findViewById(R.id.inputB3);
        inputB5 = findViewById(R.id.inputB5);
        inputElv624T1 = findViewById(R.id.inputElv624T1);
        inputElv615T2 = findViewById(R.id.inputElv615T2);
        inputPipaP1 = findViewById(R.id.inputPipaP1);

        // Initialize buttons
        btnSubmitPengukuran = findViewById(R.id.btnSubmitPengukuran);
        btnSubmitThomson = findViewById(R.id.btnSubmitThomson);
        btnSubmitSR = findViewById(R.id.btnSubmitSR);
        btnSubmitBocoran = findViewById(R.id.btnSubmitBocoran);

        // Initialize Spinners for bocoran kode
        elv624T1Kode = findViewById(R.id.elv_624_t1_kode);
        elv615T2Kode = findViewById(R.id.elv_615_t2_kode);
        pipaP1Kode = findViewById(R.id.pipa_p1_kode);

        // Initialize Spinners for SR kode
        for (int kode : srKodeArray) {
            int resId = getResources().getIdentifier("sr_" + kode + "_kode", "id", getPackageName());
            Spinner spinner = findViewById(resId);
            if (spinner != null) {
                srKodeSpinners.put(kode, spinner);
            }
        }

        // Set click listeners
        btnSubmitPengukuran.setOnClickListener(v -> handlePengukuran());
        btnSubmitThomson.setOnClickListener(v -> handleThomson());
        btnSubmitSR.setOnClickListener(v -> handleSR());
        btnSubmitBocoran.setOnClickListener(v -> handleBocoran());

        // Setup dropdown Periode
        String[] periodeArray = getResources().getStringArray(R.array.periode_options);
        ArrayAdapter<String> periodeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                periodeArray
        );
        inputPeriode.setAdapter(periodeAdapter);
        inputPeriode.setOnClickListener(v -> inputPeriode.showDropDown());

        // Setup calendar functionality
        setupCalendar();

        // Load pengukuran_id dari SharedPreferences
        SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
        pengukuranId = prefs.getInt("pengukuran_id", -1);
    }

    private void setupCalendar() {
        inputTanggal.setOnClickListener(v -> showDatePickerDialog());
        TextInputLayout tanggalLayout = (TextInputLayout) inputTanggal.getParent().getParent();
        tanggalLayout.setEndIconOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    inputTanggal.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isInternetAvailable() && !isSyncInProgress) {
            List<Map<String, String>> pengukuran = offlineDb.getAllData("pengukuran");
            List<Map<String, String>> thomson = offlineDb.getAllData("thomson");
            List<Map<String, String>> sr = offlineDb.getAllData("sr");
            List<Map<String, String>> bocoran = offlineDb.getAllData("bocoran");

            boolean adaDataOffline = !pengukuran.isEmpty() || !thomson.isEmpty() || !sr.isEmpty() || !bocoran.isEmpty();

            if (adaDataOffline) {
                isSyncInProgress = true;
                syncAllOfflineData(() -> {
                    isSyncInProgress = false;
                    boolean sudahTampil = syncPrefs.getBoolean("toast_shown", false);
                    if (!sudahTampil) {
                        runOnUiThread(() -> Toast.makeText(this, "Sinkronisasi berhasil", Toast.LENGTH_LONG).show());
                        syncPrefs.edit().putBoolean("toast_shown", true).apply();
                    }
                });
            }
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

        if (isInternetAvailable()) cekDanSimpanData("pengukuran", data, true);
        else {
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

        if (isInternetAvailable() && pengukuranId != -1) cekDanSimpanData("thomson", data, false);
        else saveOffline("thomson", tempId, data);
    }

    private void handleSR() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "sr");

        for (int kode : srKodeArray) {
            Spinner spinner = srKodeSpinners.get(kode);
            if (spinner != null) {
                data.put("sr_" + kode + "_kode", spinner.getSelectedItem().toString());
                data.put("sr_" + kode + "_nilai", getTextFromId("sr_" + kode + "_nilai"));
            }
        }

        if (pengukuranId != -1) data.put("pengukuran_id", String.valueOf(pengukuranId));
        else if (tempId != null) data.put("temp_id", tempId);
        else {
            showToast("Isi data pengukuran terlebih dahulu!");
            return;
        }

        if (isInternetAvailable() && pengukuranId != -1) cekDanSimpanData("sr", data, false);
        else saveOffline("sr", tempId, data);
    }

    private void handleBocoran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "bocoran");
        data.put("elv_624_t1", inputElv624T1.getText().toString());
        data.put("elv_624_t1_kode", elv624T1Kode.getSelectedItem().toString());
        data.put("elv_615_t2", inputElv615T2.getText().toString());
        data.put("elv_615_t2_kode", elv615T2Kode.getSelectedItem().toString());
        data.put("pipa_p1", inputPipaP1.getText().toString());
        data.put("pipa_p1_kode", pipaP1Kode.getSelectedItem().toString());

        if (pengukuranId != -1) data.put("pengukuran_id", String.valueOf(pengukuranId));
        else if (tempId != null) data.put("temp_id", tempId);
        else {
            showToast("Isi data pengukuran terlebih dahulu!");
            return;
        }

        if (isInternetAvailable() && pengukuranId != -1) cekDanSimpanData("bocoran", data, false);
        else saveOffline("bocoran", tempId, data);
    }

    private void cekDanSimpanData(String table, Map<String, String> dataMap, boolean isPengukuran) {
        if (isPengukuran) {
            // Untuk pengukuran, langsung simpan tanpa cek
            sendToServer(dataMap, table, isPengukuran);
            return;
        }

        new Thread(() -> {
            try {
                // Cek status data terlebih dahulu
                URL urlCek = new URL(CEK_DATA_URL + "?pengukuran_id=" + pengukuranId);
                HttpURLConnection connCek = (HttpURLConnection) urlCek.openConnection();
                connCek.setRequestMethod("GET");
                connCek.setRequestProperty("Accept", "application/json");

                BufferedReader readerCek = new BufferedReader(new InputStreamReader(connCek.getInputStream()));
                StringBuilder sbCek = new StringBuilder();
                String lineCek;
                while ((lineCek = readerCek.readLine()) != null) {
                    sbCek.append(lineCek);
                }
                readerCek.close();

                JSONObject responseCek = new JSONObject(sbCek.toString());
                JSONObject data = responseCek.has("data") ? responseCek.getJSONObject("data") : responseCek;

                boolean dataSudahAda = false;
                switch (table) {
                    case "thomson":
                        dataSudahAda = data.getBoolean("thomson_ada");
                        break;
                    case "sr":
                        dataSudahAda = data.getBoolean("sr_ada");
                        break;
                    case "bocoran":
                        dataSudahAda = data.getBoolean("bocoran_ada");
                        break;
                }

                if (dataSudahAda) {
                    runOnUiThread(() -> showToast("Data " + table + " sudah ada untuk pengukuran ini!"));
                    return;
                }

                // Jika data belum ada, lanjutkan penyimpanan
                sendToServer(dataMap, table, isPengukuran);

            } catch (Exception e) {
                runOnUiThread(() -> showToast("Gagal cek data, mencoba simpan langsung..."));

                // Fallback: simpan langsung jika gagal cek
                sendToServer(dataMap, table, isPengukuran);
            }
        }).start();
    }

    private void syncAllOfflineData(Runnable onComplete) {
        boolean adaData = !offlineDb.getAllData("pengukuran").isEmpty()
                || !offlineDb.getAllData("thomson").isEmpty()
                || !offlineDb.getAllData("sr").isEmpty()
                || !offlineDb.getAllData("bocoran").isEmpty();

        if (!adaData) {
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerial("pengukuran", () ->
                syncDataSerial("thomson", () ->
                        syncDataSerial("sr", () ->
                                syncDataSerial("bocoran", onComplete)
                        )
                )
        );
    }

    private void syncDataSerial(String tableName, Runnable next) {
        List<Map<String, String>> dataList = offlineDb.getAllData(tableName);
        if (dataList.isEmpty()) {
            if (next != null) next.run();
            return;
        }
        syncDataItem(tableName, dataList, 0, next);
    }

    private void syncDataItem(String tableName, List<Map<String, String>> dataList, int index, Runnable onFinish) {
        if (index >= dataList.size()) {
            onFinish.run();
            return;
        }

        Map<String, String> item = dataList.get(index);
        String tempId = item.get("temp_id");
        String jsonStr = item.get("json");

        try {
            JSONObject jsonData = new JSONObject(jsonStr);

            new Thread(() -> {
                try {
                    URL url = new URL(INSERT_DATA_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonData.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader reader;
                    if (responseCode == 200) {
                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    }

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    if (responseCode == 200) {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if ("success".equals(jsonResponse.optString("status"))) {
                            offlineDb.deleteByTempId(tableName, tempId);
                        }
                    }

                } catch (Exception e) {
                    Log.e("SYNC", "Error sync " + tableName + " tempId=" + tempId, e);
                }

                runOnUiThread(() -> syncDataItem(tableName, dataList, index + 1, onFinish));
            }).start();

        } catch (Exception e) {
            runOnUiThread(() -> syncDataItem(tableName, dataList, index + 1, onFinish));
        }
    }

    private void sendToServer(Map<String, String> dataMap, String table, boolean isPengukuran) {
        new Thread(() -> {
            try {
                URL url = new URL(INSERT_DATA_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                // Konversi Map menjadi JSON
                JSONObject jsonData = new JSONObject();
                for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                    jsonData.put(entry.getKey(), entry.getValue());
                }

                OutputStream os = conn.getOutputStream();
                os.write(jsonData.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

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

                JSONObject response = new JSONObject(sb.toString());
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                if (isPengukuran && response.has("pengukuran_id")) {
                    pengukuranId = response.getInt("pengukuran_id");
                    SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
                    prefs.edit().putInt("pengukuran_id", pengukuranId).apply();
                }

                runOnUiThread(() -> {
                    if ("success".equals(status)) {
                        showToast(message);
                    } else {
                        showToast("Error: " + message);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> showToast("Gagal kirim: " + e.getMessage()));
            }
        }).start();
    }

    private void saveOffline(String table, String tempId, Map<String, String> data) {
        try {
            JSONObject json = new JSONObject(data);
            offlineDb.insertData(table, tempId, json.toString());
            showToast("Tidak ada internet. Data disimpan offline.");
            syncPrefs.edit().putBoolean("toast_shown", false).apply();
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