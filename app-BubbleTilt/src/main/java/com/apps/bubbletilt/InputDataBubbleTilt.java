package com.apps.bubbletilt;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.apps.bubbletilt.OfflineDataHelperBTM;
import com.google.android.material.textfield.TextInputEditText;
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

public class InputDataBubbleTilt extends AppCompatActivity {

    // Modal pengukuran
    private CardView modalPengukuran;
    private View modalOverlay;
    private ImageButton btnCloseModal;
    private ScrollView mainContent;

    // Input modal
    private EditText modalInputTahun;
    private AutoCompleteTextView modalInputBulan, modalInputPeriode;
    private EditText modalInputTanggal;
    private Button modalBtnSubmitPengukuran;

    // Form utama Bubble Tilt
    private TextInputEditText inputUSGP, inputTBGP;
    private Spinner spinnerUSArah, spinnerTBArah;
    private Button btnSimpanHitung; // ✅ Hanya 1 tombol untuk simpan & hitung
    private Spinner spinnerPengukuran, spinnerBT;
    private Button btnPilihPengukuran;
    private TextView titleBubbleTilt;

    private Calendar calendar;
    private int pengukuranId = -1;
    private int selectedBT = 1;
    private String tempId = null;

    // API URL
    private static final String BASE_URL = "http://192.168.1.10/GHW/api-apps/public/btm/";
    private static final String INSERT_DATA_URL = BASE_URL + "input";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "get-pengukuran-bulan-ini";

    // Data pengukuran
    private final Map<String, Integer> pengukuranMap = new HashMap<>();
    private final List<String> tanggalList = new ArrayList<>();
    private ArrayAdapter<String> pengukuranAdapter;
    private ArrayAdapter<String> btAdapter;

    // AUTO SYNC VARIABLES
    private OfflineDataHelperBTM offlineDb;
    private SharedPreferences syncPrefs;
    private boolean isSyncInProgress = false;
    private Handler networkCheckHandler = new Handler();
    private Runnable networkCheckRunnable;
    private boolean lastOnlineStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data_bubble_tilt);

        // Inisialisasi database offline + prefs
        offlineDb = new OfflineDataHelperBTM(this);
        syncPrefs = getSharedPreferences("btm_sync_prefs", MODE_PRIVATE);
        calendar = Calendar.getInstance();

        initModalComponents();
        initFormComponents();
        initSpinnerComponents();
        setupModalDropdowns();
        setupModalCalendar();
        setupArahSpinners();

        // Load data pengukuran (online/offline)
        loadPengukuranData();

        // Tampilkan modal di awal
        showModal();
    }

    // Setup spinner untuk US Arah dan TB Arah
    private void setupArahSpinners() {
        // Spinner untuk US Arah (U atau S)
        String[] usArahOptions = {"Pilih Arah US", "U", "S"};
        ArrayAdapter<String> usArahAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, usArahOptions);
        usArahAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUSArah.setAdapter(usArahAdapter);

        // Spinner untuk TB Arah (B atau T)
        String[] tbArahOptions = {"Pilih Arah TB", "B", "T"};
        ArrayAdapter<String> tbArahAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tbArahOptions);
        tbArahAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTBArah.setAdapter(tbArahAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInternetAndShowToast();

        if (isInternetAvailable()) {
            if (offlineDb.hasUnsyncedDataBTM()) {
                syncAllOfflineData(() -> {
                    if (!isAlreadySynced()) {
                        showToast("✅ Sinkronisasi data offline selesai");
                        markAsSynced();
                    }
                });
            } else {
                loadPengukuranData();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopNetworkMonitoring();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNetworkMonitoring();
        if (offlineDb != null) {
            offlineDb.close();
        }
    }

    // AUTO SYNC METHODS
    private void startNetworkMonitoring() {
        networkCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkInternetAndShowToast();
                networkCheckHandler.postDelayed(this, 5000);
            }
        };
        networkCheckHandler.postDelayed(networkCheckRunnable, 5000);
    }

    private void stopNetworkMonitoring() {
        if (networkCheckHandler != null && networkCheckRunnable != null) {
            networkCheckHandler.removeCallbacks(networkCheckRunnable);
        }
    }

    private void checkInternetAndShowToast() {
        boolean isOnline = isInternetAvailable();
        if (isOnline != lastOnlineStatus) {
            if (isOnline) {
                showToast("✅ Online - Koneksi tersedia");
                startAutoSyncWhenOnline();
            } else {
                showToast("📱 Offline - Data disimpan lokal");
            }
            lastOnlineStatus = isOnline;
        }
    }

    private void startAutoSyncWhenOnline() {
        if (isSyncInProgress || !isInternetAvailable()) return;

        int offlineCount = offlineDb.getOfflineDataCountBTM();
        if (offlineCount > 0) {
            Log.d("BTM_AutoSync", "Found " + offlineCount + " offline data, starting auto-sync");
            triggerAutoSync();
        }
    }

    private void triggerAutoSync() {
        if (isSyncInProgress) return;

        Log.d("BTM_AutoSync", "Triggering auto-sync for offline data");
        isSyncInProgress = true;
        showToast("🔄 Auto-sync data offline...");

        syncAllOfflineDataAuto(() -> {
            isSyncInProgress = false;
            Log.d("BTM_AutoSync", "Auto-sync completed");
            runOnUiThread(this::loadPengukuranData);
        });
    }

    private void syncAllOfflineDataAuto(Runnable onComplete) {
        int offlineCount = offlineDb.getOfflineDataCountBTM();
        if (offlineCount == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerialAuto("pengukuran", () ->
                syncDataSerialAuto("data", () -> {
                    showToast("✅ " + offlineCount + " data terkirim");
                    if (onComplete != null) onComplete.run();
                })
        );
    }

    private void syncDataSerialAuto(String tableType, Runnable next) {
        List<Map<String,String>> list = offlineDb.getUnsyncedDataBTM(tableType);
        if (list == null || list.isEmpty()) {
            if (next != null) next.run();
            return;
        }
        syncDataItemAuto(tableType, list, 0, next);
    }

    private void syncDataItemAuto(String tableType, List<Map<String,String>> dataList, int index, Runnable onFinish) {
        if (index >= dataList.size()) {
            if (onFinish != null) onFinish.run();
            return;
        }

        Map<String,String> item = dataList.get(index);
        String tempId = item.get("temp_id");
        String jsonStr = item.get("json");

        if (jsonStr == null || jsonStr.isEmpty()) {
            offlineDb.deleteByTempIdBTM(tableType, tempId);
            syncDataItemAuto(tableType, dataList, index + 1, onFinish);
            return;
        }

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(jsonStr);
                Map<String,String> dataMap = new HashMap<>();
                Iterator<String> it = json.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    dataMap.put(k, json.optString(k, ""));
                }

                HttpURLConnection conn = null;
                try {
                    URL url = new URL(INSERT_DATA_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");

                    OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        offlineDb.deleteByTempIdBTM(tableType, tempId);
                        Log.d("BTM_AutoSync", "Synced " + tableType + " tempId=" + tempId);
                    }
                } catch (Exception e) {
                    Log.e("BTM_AutoSync", "Failed to sync tempId=" + tempId + ": " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            } catch (Exception e) {
                Log.e("BTM_AutoSync", "JSON parse failed for tempId=" + tempId + ": " + e.getMessage());
                offlineDb.deleteByTempIdBTM(tableType, tempId);
            }

            runOnUiThread(() -> syncDataItemAuto(tableType, dataList, index + 1, onFinish));
        }).start();
    }

    private boolean isAlreadySynced() {
        SharedPreferences prefs = getSharedPreferences("btm_app_prefs", MODE_PRIVATE);
        String lastSyncDate = prefs.getString("last_sync_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return today.equals(lastSyncDate);
    }

    private void markAsSynced() {
        SharedPreferences prefs = getSharedPreferences("btm_app_prefs", MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString("last_sync_date", today).apply();
    }

    // INIT COMPONENTS
    private void initModalComponents() {
        modalPengukuran = findViewById(R.id.modalPengukuran);
        modalOverlay = findViewById(R.id.modalOverlay);
        btnCloseModal = findViewById(R.id.btnCloseModal);
        mainContent = findViewById(R.id.mainContent);

        modalInputTahun = findViewById(R.id.modalInputTahun);
        modalInputBulan = findViewById(R.id.modalInputBulan);
        modalInputPeriode = findViewById(R.id.modalInputPeriode);
        modalInputTanggal = findViewById(R.id.modalInputTanggal);
        modalBtnSubmitPengukuran = findViewById(R.id.modalBtnSubmitPengukuran);

        if (btnCloseModal != null) btnCloseModal.setOnClickListener(v -> hideModal());
        if (modalOverlay != null) modalOverlay.setOnClickListener(v -> hideModal());
        if (modalBtnSubmitPengukuran != null) modalBtnSubmitPengukuran.setOnClickListener(v -> handleModalPengukuran());
    }

    private void initFormComponents() {
        inputUSGP = findViewById(R.id.inputUSGP);
        spinnerUSArah = findViewById(R.id.spinnerUSArah);
        inputTBGP = findViewById(R.id.inputTBGP);
        spinnerTBArah = findViewById(R.id.spinnerTBArah);

        // ✅ Hanya 1 tombol untuk Simpan & Hitung
        btnSimpanHitung = findViewById(R.id.btnSimpanHitung);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        spinnerBT = findViewById(R.id.spinnerBT);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
        titleBubbleTilt = findViewById(R.id.titleBubbleTilt);

        // Set click listener untuk tombol Simpan & Hitung
        btnSimpanHitung.setOnClickListener(v -> handleSimpanHitung());
    }

    private void initSpinnerComponents() {
        // Adapter untuk spinner pengukuran
        pengukuranAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        pengukuranAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(pengukuranAdapter);

        // Adapter untuk spinner BT (Bubble Tilt 1-8)
        String[] btOptions = {"BT1", "BT2", "BT3", "BT4", "BT5", "BT6", "BT7", "BT8"};
        btAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, btOptions);
        btAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBT.setAdapter(btAdapter);

        btnPilihPengukuran.setOnClickListener(v -> {
            Object selected = spinnerPengukuran.getSelectedItem();
            if (selected != null && pengukuranMap.containsKey(selected.toString())) {
                pengukuranId = pengukuranMap.get(selected.toString());
                showToast("✅ Pengukuran dipilih: " + selected);
            } else {
                showToast("❌ Pilih tanggal pengukuran terlebih dahulu");
            }
        });

        spinnerPengukuran.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = position >= 0 && position < tanggalList.size() ? tanggalList.get(position) : null;
                if (selected != null && pengukuranMap.containsKey(selected)) {
                    pengukuranId = pengukuranMap.get(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerBT.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBT = position + 1;
                clearForm();
                updateTitle();
                loadExistingData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Method untuk clear form
    private void clearForm() {
        runOnUiThread(() -> {
            inputUSGP.setText("");
            inputTBGP.setText("");
            spinnerUSArah.setSelection(0);
            spinnerTBArah.setSelection(0);
        });
    }

    private void updateTitle() {
        if (titleBubbleTilt != null) {
            titleBubbleTilt.setText("Bubble Tilt Measurements - BT" + selectedBT);
        }
    }

    private void loadExistingData() {
        if (pengukuranId == -1) return;

        if (isInternetAvailable()) {
            loadDataFromServer();
        } else {
            loadDataFromOffline();
        }

        // ✅ TAMBAHAN: Cek data di database untuk debugging
        cekDataDiDatabase();
    }

    private void loadDataFromServer() {
        new Thread(() -> {
            try {
                String url = BASE_URL + "get-data?pengukuran_id=" + pengukuranId + "&bt=" + selectedBT;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject response = new JSONObject(sb.toString());
                    if (response.getString("status").equals("success")) {
                        JSONObject data = response.getJSONObject("data");
                        runOnUiThread(() -> populateForm(data));
                    }
                }
            } catch (Exception e) {
                Log.e("LOAD_DATA", "Error loading data: " + e.getMessage());
            }
        }).start();
    }

    private void loadDataFromOffline() {
        Map<String, String> data = offlineDb.getBTMData(pengukuranId, selectedBT);
        if (data != null) {
            try {
                JSONObject jsonData = new JSONObject();
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    jsonData.put(entry.getKey(), entry.getValue());
                }
                populateForm(jsonData);
            } catch (Exception e) {
                Log.e("LOAD_OFFLINE", "Error parsing offline data: " + e.getMessage());
            }
        }
    }

    private void populateForm(JSONObject data) {
        try {
            if (data.has("US_GP")) inputUSGP.setText(data.getString("US_GP"));
            if (data.has("TB_GP")) inputTBGP.setText(data.getString("TB_GP"));

            if (data.has("US_Arah")) {
                String usArah = data.getString("US_Arah");
                if (usArah.equals("U") || usArah.equals("S")) {
                    for (int i = 0; i < spinnerUSArah.getCount(); i++) {
                        if (spinnerUSArah.getItemAtPosition(i).toString().equals(usArah)) {
                            spinnerUSArah.setSelection(i);
                            break;
                        }
                    }
                }
            }

            if (data.has("TB_Arah")) {
                String tbArah = data.getString("TB_Arah");
                if (tbArah.equals("B") || tbArah.equals("T")) {
                    for (int i = 0; i < spinnerTBArah.getCount(); i++) {
                        if (spinnerTBArah.getItemAtPosition(i).toString().equals(tbArah)) {
                            spinnerTBArah.setSelection(i);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("POPULATE_FORM", "Error populating form: " + e.getMessage());
        }
    }

    // ==================== TOMBOL SIMPAN & HITUNG ====================

    // Handle Simpan & Hitung dalam 1 tombol
    private void handleSimpanHitung() {
        if (pengukuranId == -1) {
            showToast("❌ Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        // Validasi field
        if (!validateBubbleTiltFields()) {
            showToast("❌ Harap isi semua field Bubble Tilt");
            return;
        }

        // Simpan data terlebih dahulu
        simpanDataBubbleTilt();
    }

    // Simpan data Bubble Tilt
    private void simpanDataBubbleTilt() {
        String usArah = spinnerUSArah.getSelectedItem().toString();
        String tbArah = spinnerTBArah.getSelectedItem().toString();

        if (usArah.equals("Pilih Arah US") || tbArah.equals("Pilih Arah TB")) {
            showToast("❌ Harap pilih arah yang valid");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("mode", "bubbletilt");
        data.put("bt_number", String.valueOf(selectedBT));
        data.put("us_gp", inputUSGP.getText().toString().trim());
        data.put("us_arah", usArah);
        data.put("tb_gp", inputTBGP.getText().toString().trim());
        data.put("tb_arah", tbArah);
        data.put("pengukuran_id", String.valueOf(pengukuranId));

        Log.d("BTM_API", "Menyimpan data BT" + selectedBT + ": " + data.toString());

        if (isInternetAvailable()) {
            // Jika online, simpan ke server lalu hitung
            simpanKeServerDanHitung(data);
        } else {
            // Jika offline, simpan ke lokal
            String localTempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", localTempId);
            saveOffline("data", localTempId, data);
            showToast("📱 Data BT" + selectedBT + " disimpan offline");
            // Untuk offline, tidak bisa hitung karena butuh koneksi server
            showToast("⚠️ Hitung hanya bisa dilakukan saat online");
        }
    }

    // Simpan ke server dan langsung hitung
// Simpan ke server dan langsung hitung
    private void simpanKeServerDanHitung(Map<String, String> data) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(INSERT_DATA_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject jsonData = new JSONObject();
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    jsonData.put(entry.getKey(), entry.getValue());
                }

                String jsonString = jsonData.toString();
                Log.d("BTM_API", "JSON yang dikirim: " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("BTM_API", "Response Code: " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("BTM_API", "Response Body: " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                runOnUiThread(() -> {
                    // ✅ PERBAIKAN: Tangani semua status response
                    if (status.equalsIgnoreCase("success") || status.equalsIgnoreCase("info")) {
                        // Baik data baru berhasil disimpan atau data sudah ada, lanjutkan hitung
                        showToast("✅ " + message);
                        // Tunggu sebentar sebelum hitung untuk memastikan data tersimpan
                        new Handler().postDelayed(() -> {
                            hitungBubbleTilt();
                        }, 1000);
                    } else {
                        showToast("❌ Gagal simpan: " + message);
                    }
                });

            } catch (Exception e) {
                Log.e("SIMPAN_HITUNG", "Error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal simpan: " + e.getMessage() + ". Data disimpan offline.");
                    // Simpan offline jika gagal
                    String localTempId = "local_" + System.currentTimeMillis();
                    data.put("temp_id", localTempId);
                    saveOffline("data", localTempId, data);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // Hitung Bubble Tilt
    private void hitungBubbleTilt() {
        try {
            // ✅ PERBAIKAN: Validasi ulang pengukuran_id
            if (pengukuranId == -1) {
                showToast("❌ Pengukuran ID tidak valid");
                return;
            }

            String url = BASE_URL + "hitung/bubbletilt";

            JSONObject postData = new JSONObject();
            postData.put("pengukuran_id", pengukuranId);
            postData.put("bt_number", selectedBT);

            Log.d("BTM_HITUNG", "Mengirim request hitung untuk BT" + selectedBT + ", pengukuran_id: " + pengukuranId);
            Log.d("BTM_HITUNG", "JSON Hitung: " + postData.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    postData,
                    response -> {
                        try {
                            Log.d("BTM_HITUNG", "Response Hitung: " + response.toString());

                            String status = response.optString("status", "error");
                            String message = response.optString("message", "Tidak ada pesan dari server");

                            if (status.equalsIgnoreCase("success")) {
                                JSONObject data = response.optJSONObject("data");

                                // Format hasil perhitungan
                                StringBuilder hasilBuilder = new StringBuilder();
                                hasilBuilder.append("📊 HASIL PERHITUNGAN BT").append(selectedBT).append("\n\n");

                                if (data != null) {
                                    // Data Bacaan
                                    if (data.has("data_bacaan")) {
                                        JSONObject dataBacaan = data.getJSONObject("data_bacaan");
                                        hasilBuilder.append("📝 DATA BACAAN:\n");
                                        hasilBuilder.append("US_GP: ").append(dataBacaan.optDouble("US_GP", 0)).append("\n");
                                        hasilBuilder.append("US_Arah: ").append(dataBacaan.optString("US_Arah", "")).append("\n");
                                        hasilBuilder.append("TB_GP: ").append(dataBacaan.optDouble("TB_GP", 0)).append("\n");
                                        hasilBuilder.append("TB_Arah: ").append(dataBacaan.optString("TB_Arah", "")).append("\n\n");
                                    }

                                    // Hasil Akhir
                                    if (data.has("hasil_akhir")) {
                                        JSONObject hasilAkhir = data.getJSONObject("hasil_akhir");
                                        hasilBuilder.append("📈 HASIL AKHIR:\n");
                                        hasilBuilder.append("ΔH: ").append(String.format(Locale.getDefault(), "%.4f", hasilAkhir.optDouble("delta_h", 0))).append("\n");
                                        hasilBuilder.append("Kemiringan: ").append(String.format(Locale.getDefault(), "%.4f", hasilAkhir.optDouble("kemiringan", 0))).append("°\n");
                                        hasilBuilder.append("Arah: ").append(hasilAkhir.optString("arah_kemiringan", "")).append("\n");
                                        hasilBuilder.append("Keterangan: ").append(hasilAkhir.optString("keterangan", "")).append("\n\n");
                                    }

                                    // Perhitungan Utama (jika ada)
                                    if (data.has("perhitungan_utama")) {
                                        JSONObject perhitunganUtama = data.getJSONObject("perhitungan_utama");
                                        hasilBuilder.append("🧮 PERHITUNGAN UTAMA:\n");
                                        if (perhitunganUtama.has("A_sec")) {
                                            hasilBuilder.append("A_sec: ").append(String.format(Locale.getDefault(), "%.4f", perhitunganUtama.optDouble("A_sec", 0))).append("\n");
                                        }
                                        if (perhitunganUtama.has("B_sec")) {
                                            hasilBuilder.append("B_sec: ").append(String.format(Locale.getDefault(), "%.4f", perhitunganUtama.optDouble("B_sec", 0))).append("\n");
                                        }
                                        if (perhitunganUtama.has("sin_C_deg")) {
                                            hasilBuilder.append("sin_C_deg: ").append(String.format(Locale.getDefault(), "%.4f", perhitunganUtama.optDouble("sin_C_deg", 0))).append("\n");
                                        }
                                    }
                                } else {
                                    hasilBuilder.append("Tidak ada data hasil perhitungan");
                                }

                                // Tampilkan dialog hasil
                                showResultDialog("✅ Simpan & Hitung Berhasil", hasilBuilder.toString());

                                // Clear form setelah berhasil
                                clearForm();

                            } else {
                                showToast("⚠️ " + message);
                                // Tampilkan response lengkap untuk debugging
                                Log.e("BTM_HITUNG", "Hitung gagal: " + response.toString());
                            }

                        } catch (Exception e) {
                            Log.e("BTM_HITUNG", "Error parsing response: " + e.getMessage());
                            showToast("❌ Gagal memproses hasil: " + e.getMessage());
                        }
                    },
                    error -> {
                        String msg = "❌ Gagal terhubung ke server untuk hitung";
                        if (error != null) {
                            if (error.networkResponse != null) {
                                msg += " (HTTP " + error.networkResponse.statusCode + ")";
                                try {
                                    String errorBody = new String(error.networkResponse.data, "UTF-8");
                                    Log.e("BTM_HITUNG", "Error response body: " + errorBody);
                                } catch (Exception e) {
                                    Log.e("BTM_HITUNG", "Error reading error response");
                                }
                            } else if (error.getMessage() != null) {
                                msg += ": " + error.getMessage();
                            }
                        }
                        Log.e("BTM_HITUNG", "Volley error: " + msg);
                        showToast(msg);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    return headers;
                }

                @Override
                public byte[] getBody() {
                    try {
                        JSONObject postData = new JSONObject();
                        postData.put("pengukuran_id", pengukuranId);
                        postData.put("bt_number", selectedBT);
                        return postData.toString().getBytes("UTF-8");
                    } catch (Exception e) {
                        Log.e("BTM_HITUNG", "Error creating request body: " + e.getMessage());
                        return null;
                    }
                }
            };

            // Tambahkan timeout
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    15000,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            queue.add(request);

        } catch (Exception e) {
            Log.e("BTM_HITUNG", "Error: " + e.getMessage());
            showToast("❌ Error: " + e.getMessage());
        }
    }

    // Method untuk debugging - cek data di database
    private void cekDataDiDatabase() {
        if (pengukuranId == -1) return;

        new Thread(() -> {
            try {
                String url = BASE_URL + "get-data?pengukuran_id=" + pengukuranId + "&bt=" + selectedBT;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject response = new JSONObject(sb.toString());
                    Log.d("BTM_DEBUG", "Data di database: " + response.toString());

                    if (response.getString("status").equals("success")) {
                        JSONObject data = response.getJSONObject("data");
                        runOnUiThread(() -> {
                            showToast("ℹ️ Data BT" + selectedBT + " sudah ada di database");
                            // Isi form dengan data yang ada
                            populateForm(data);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("BTM_DEBUG", "Error cek data: " + e.getMessage());
            }
        }).start();
    }

    // Validasi field Bubble Tilt
    private boolean validateBubbleTiltFields() {
        String usArah = spinnerUSArah.getSelectedItem().toString();
        String tbArah = spinnerTBArah.getSelectedItem().toString();

        return !inputUSGP.getText().toString().trim().isEmpty() &&
                !inputTBGP.getText().toString().trim().isEmpty() &&
                !usArah.equals("Pilih Arah US") &&
                !tbArah.equals("Pilih Arah TB");
    }

    // Method untuk menampilkan dialog hasil perhitungan
    private void showResultDialog(String title, String message) {
        runOnUiThread(() -> {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();
        });
    }

    // ==================== METHOD LAIN YANG TETAP SAMA ====================

    private void setupModalDropdowns() {
        try {
            String[] bulanArray = getResources().getStringArray(R.array.bulan_options);
            ArrayAdapter<String> bulanAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bulanArray);
            if (modalInputBulan != null) {
                modalInputBulan.setAdapter(bulanAdapter);
                modalInputBulan.setOnClickListener(v -> modalInputBulan.showDropDown());
            }

            String[] periodeArray = getResources().getStringArray(R.array.periode_options);
            ArrayAdapter<String> periodeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, periodeArray);
            if (modalInputPeriode != null) {
                modalInputPeriode.setAdapter(periodeAdapter);
                modalInputPeriode.setOnClickListener(v -> modalInputPeriode.showDropDown());
            }
        } catch (Exception e) {
            Log.e("SETUP_MODAL", "Gagal setup dropdown: " + e.getMessage());
        }
    }

    private void setupModalCalendar() {
        if (modalInputTanggal != null) {
            modalInputTanggal.setOnClickListener(v -> showModalDatePickerDialog());
            try {
                TextInputLayout tanggalLayout = (TextInputLayout) modalInputTanggal.getParent().getParent();
                if (tanggalLayout != null) {
                    tanggalLayout.setEndIconDrawable(R.drawable.ic_calendar);
                    tanggalLayout.setEndIconOnClickListener(v -> showModalDatePickerDialog());
                }
            } catch (Exception ignored) {}
        }
    }

    private void showModalDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    if (modalInputTanggal != null) modalInputTanggal.setText(dateFormat.format(calendar.getTime()));

                    if (modalInputTahun != null) modalInputTahun.setText(String.valueOf(selectedYear));

                    try {
                        String[] bulanNama = getResources().getStringArray(R.array.bulan_options);
                        if (modalInputBulan != null) {
                            if (selectedMonth >= 0 && selectedMonth < bulanNama.length) {
                                modalInputBulan.setText(bulanNama[selectedMonth]);
                            } else {
                                modalInputBulan.setText(String.format(Locale.getDefault(), "%02d", (selectedMonth + 1)));
                            }
                        }
                    } catch (Exception ignored) {}

                    String triwulan;
                    if (selectedMonth <= 2) {
                        triwulan = "TW-1";
                    } else if (selectedMonth <= 5) {
                        triwulan = "TW-2";
                    } else if (selectedMonth <= 8) {
                        triwulan = "TW-3";
                    } else {
                        triwulan = "TW-4";
                    }

                    if (modalInputPeriode != null) {
                        try {
                            modalInputPeriode.setText(triwulan, false);
                        } catch (Exception e) {
                            modalInputPeriode.setText(triwulan);
                        }
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showModal() {
        if (modalPengukuran == null || modalOverlay == null || mainContent == null) return;
        modalPengukuran.setVisibility(View.VISIBLE);
        modalOverlay.setVisibility(View.VISIBLE);
        mainContent.setAlpha(0.5f);
        mainContent.setEnabled(false);
    }

    private void hideModal() {
        if (modalPengukuran == null || modalOverlay == null || mainContent == null) return;
        modalPengukuran.setVisibility(View.GONE);
        modalOverlay.setVisibility(View.GONE);
        mainContent.setAlpha(1.0f);
        mainContent.setEnabled(true);
        mainContent.setVisibility(View.VISIBLE);
    }

    // Load data pengukuran dengan offline support
    private void loadPengukuranData() {
        if (!isInternetAvailable()) {
            showToast("📱 Tidak ada internet, load data dari lokal");
            loadTanggalOffline();
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(GET_PENGUKURAN_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("BTM_API", "Response get-pengukuran: " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");

                if ("success".equals(status)) {
                    JSONArray dataArray = response.optJSONArray("data");
                    pengukuranMap.clear();
                    tanggalList.clear();

                    if (dataArray != null && dataArray.length() > 0) {
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);
                            String tanggal = item.optString("tanggal", "");
                            int id = item.optInt("id_pengukuran", -1);

                            if (!tanggal.isEmpty() && id != -1) {
                                tanggalList.add(tanggal);
                                pengukuranMap.put(tanggal, id);
                            }
                        }
                        runOnUiThread(() -> {
                            pengukuranAdapter.notifyDataSetChanged();
                            if (!tanggalList.isEmpty()) {
                                spinnerPengukuran.setSelection(0);
                                pengukuranId = pengukuranMap.get(tanggalList.get(0));
                                showToast("📅 Load " + tanggalList.size() + " data pengukuran");
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            showToast("ℹ️ Tidak ada data pengukuran tersedia");
                            loadTanggalOffline();
                        });
                    }
                } else {
                    String message = response.optString("message", "Gagal load data");
                    runOnUiThread(() -> {
                        showToast("❌ Error: " + message);
                        loadTanggalOffline();
                    });
                }

            } catch (Exception e) {
                Log.e("LOAD_PENGUKURAN", "Error: ", e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal load data pengukuran: " + e.getMessage());
                    loadTanggalOffline();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // Load data dari lokal ketika offline
    private void loadTanggalOffline() {
        try {
            List<Map<String,String>> rows = offlineDb.getPengukuranMasterBTM();
            List<String> list = new ArrayList<>();
            pengukuranMap.clear();

            if (rows != null && !rows.isEmpty()) {
                for (Map<String,String> r : rows) {
                    String tanggal = r.get("tanggal");
                    String idStr = r.get("id_pengukuran");
                    if (tanggal != null) {
                        list.add(tanggal);
                        try {
                            if (idStr != null && !idStr.startsWith("local_")) {
                                pengukuranMap.put(tanggal, Integer.parseInt(idStr));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } else {
                list.add("Belum ada pengukuran (offline)");
            }

            runOnUiThread(() -> {
                tanggalList.clear();
                tanggalList.addAll(list);
                pengukuranAdapter.notifyDataSetChanged();
                if (!list.isEmpty()) {
                    spinnerPengukuran.setSelection(0);
                }
            });
        } catch (Exception e) {
            Log.e("BTM_Offline", "Error load offline master: " + e.getMessage());
        }
    }

    // Handle modal dengan offline support
    private void handleModalPengukuran() {
        if (modalInputTahun == null || modalInputBulan == null || modalInputPeriode == null || modalInputTanggal == null) {
            showToast("Form modal belum siap");
            return;
        }

        String tahun = modalInputTahun.getText().toString().trim();
        String bulan = modalInputBulan.getText().toString().trim();
        String periode = modalInputPeriode.getText().toString().trim();
        String tanggal = modalInputTanggal.getText().toString().trim();

        if (tahun.isEmpty() || bulan.isEmpty() || periode.isEmpty() || tanggal.isEmpty()) {
            showToast("Harap isi semua field yang wajib");
            return;
        }

        String bulanAngka = convertBulanToNumber(bulan);
        Map<String, String> data = new HashMap<>();
        data.put("mode", "pengukuran");
        data.put("tahun", tahun);
        data.put("bulan", bulanAngka);
        data.put("periode", periode);
        data.put("tanggal", tanggal);

        Log.d("BTM_API", "Mengirim data pengukuran: " + data.toString());

        if (isInternetAvailable()) {
            sendToServer(data, "pengukuran", true);
        } else {
            tempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", tempId);
            saveOffline("pengukuran", tempId, data);
            hideModal();
        }
    }

    private String convertBulanToNumber(String bulanName) {
        Map<String, String> bulanMap = new HashMap<>();
        bulanMap.put("JANUARI", "01"); bulanMap.put("JAN", "01");
        bulanMap.put("FEBRUARI", "02"); bulanMap.put("FEB", "02");
        bulanMap.put("MARET", "03"); bulanMap.put("MAR", "03");
        bulanMap.put("APRIL", "04"); bulanMap.put("APR", "04");
        bulanMap.put("MEI", "05");
        bulanMap.put("JUNI", "06"); bulanMap.put("JUN", "06");
        bulanMap.put("JULI", "07"); bulanMap.put("JUL", "07");
        bulanMap.put("AGUSTUS", "08"); bulanMap.put("AGS", "08"); bulanMap.put("AGT", "08");
        bulanMap.put("SEPTEMBER", "09"); bulanMap.put("SEP", "09");
        bulanMap.put("OKTOBER", "10"); bulanMap.put("OKT", "10");
        bulanMap.put("NOVEMBER", "11"); bulanMap.put("NOV", "11");
        bulanMap.put("DESEMBER", "12"); bulanMap.put("DES", "12");

        String upperBulan = bulanName.toUpperCase();
        return bulanMap.getOrDefault(upperBulan, "01");
    }

    // Save offline method
    private void saveOffline(String tableType, String tempId, Map<String, String> data) {
        try {
            JSONObject json = new JSONObject(data);
            boolean success = offlineDb.insertDataBTM(tableType, tempId, json.toString());
            if (success) {
                showToast("📱 Data disimpan offline (" + tableType + ")");
            } else {
                showToast("❌ Gagal simpan offline");
            }
        } catch (Exception e) {
            Log.e("BTM_Offline", "Gagal simpan offline: " + e.getMessage());
            showToast("❌ Gagal simpan offline: " + e.getMessage());
        }
    }

    // Send to server dengan offline fallback
    private void sendToServer(Map<String, String> dataMap, String tableType, boolean isPengukuran) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(INSERT_DATA_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject jsonData = new JSONObject();
                for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                    jsonData.put(entry.getKey(), entry.getValue());
                }

                String jsonString = jsonData.toString();
                Log.d("BTM_API", "JSON yang dikirim: " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("BTM_API", "Response Code: " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("BTM_API", "Response Body: " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                if (isPengukuran && response.has("pengukuran_id")) {
                    pengukuranId = response.optInt("pengukuran_id", -1);
                    tempId = null;
                    Log.d("BTM_API", "Pengukuran ID diterima: " + pengukuranId);
                    runOnUiThread(this::loadPengukuranData);
                }

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            showToast("✅ " + message);
                            if (isPengukuran) {
                                hideModal();
                            }
                            break;
                        case "info":
                            showToast("ℹ️ " + message);
                            if (isPengukuran && response.has("pengukuran_id")) {
                                pengukuranId = response.optInt("pengukuran_id", -1);
                                hideModal();
                                runOnUiThread(this::loadPengukuranData);
                            }
                            break;
                        case "warning":
                            showToast("⚠️ " + message);
                            break;
                        case "error":
                        default:
                            showToast("❌ " + message);
                            if (!dataMap.containsKey("temp_id")) {
                                String localTempId = "local_" + System.currentTimeMillis();
                                dataMap.put("temp_id", localTempId);
                                saveOffline(tableType, localTempId, dataMap);
                            }
                            break;
                    }
                });

            } catch (Exception e) {
                Log.e("SEND_TO_SERVER", "Error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal kirim: " + e.getMessage() + ". Data disimpan offline.");
                    if (!dataMap.containsKey("temp_id")) {
                        String localTempId = "local_" + System.currentTimeMillis();
                        dataMap.put("temp_id", localTempId);
                        saveOffline(tableType, localTempId, dataMap);
                    }
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // Sync all offline data method
    private void syncAllOfflineData(Runnable onComplete) {
        boolean adaData = offlineDb.hasUnsyncedDataBTM();
        if (!adaData) {
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerial("pengukuran", () ->
                syncDataSerial("data", onComplete)
        );
    }

    private void syncDataSerial(String tableType, Runnable next) {
        List<Map<String, String>> dataList = offlineDb.getUnsyncedDataBTM(tableType);
        if (dataList.isEmpty()) {
            if (next != null) next.run();
            return;
        }
        syncDataItem(tableType, dataList, 0, next);
    }

    private void syncDataItem(String tableType, List<Map<String, String>> dataList, int index, Runnable onFinish) {
        if (index >= dataList.size()) {
            if (onFinish != null) onFinish.run();
            return;
        }

        Map<String, String> item = dataList.get(index);
        String tempId = item.get("temp_id");
        String jsonStr = item.get("json");

        try {
            JSONObject jsonData = new JSONObject(jsonStr);

            new Thread(() -> {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(INSERT_DATA_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setConnectTimeout(9000);
                    conn.setReadTimeout(9000);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonData.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        offlineDb.deleteByTempIdBTM(tableType, tempId);
                        Log.d("BTM_Sync", "Data " + tableType + " tempId=" + tempId + " berhasil disinkronisasi");
                    }
                } catch (Exception e) {
                    Log.e("BTM_Sync", "Error sync " + tableType + " tempId=" + tempId, e);
                } finally {
                    if (conn != null) conn.disconnect();
                }

                runOnUiThread(() -> syncDataItem(tableType, dataList, index + 1, onFinish));
            }).start();

        } catch (Exception e) {
            Log.e("BTM_Sync", "JSON parse error untuk data " + tableType + " tempId=" + tempId, e);
            runOnUiThread(() -> syncDataItem(tableType, dataList, index + 1, onFinish));
        }
    }

    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d("BTM_TOAST", "Pesan: " + message);
        } catch (Exception e) {
            Log.e("TOAST_ERROR", "Gagal menampilkan toast: " + e.getMessage());
        }
    }

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo active = cm != null ? cm.getActiveNetworkInfo() : null;
            return active != null && active.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}