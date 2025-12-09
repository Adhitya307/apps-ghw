package com.apps.bubbletilt;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
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
    private Button btnSimpanHitung;
    private Spinner spinnerPengukuran, spinnerBT;
    private Button btnPilihPengukuran;
    private TextView titleBubbleTilt;

    private Calendar calendar;
    private int pengukuranId = -1;
    private int selectedBT = 1;
    private String tempId = null;

    // API URL
    private static final String BASE_URL = "http://192.168.1.11/GHW/api-apps/public/btm/";
    private static final String INSERT_DATA_URL = BASE_URL + "input";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "get-pengukuran-bulan-ini";
    private static final String HITUNG_URL = BASE_URL + "hitung/bubbletilt";
    private static final String GET_SCATTER_DATA_URL = BASE_URL + "get-scatter-data/";

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

    // Untuk sinkronisasi dengan popup berurutan
    private List<BTMPopupData> pendingPopups = new ArrayList<>();
    private boolean isShowingPopup = false;

    // Format angka
    private DecimalFormat df6 = new DecimalFormat("#0.000000");
    private DecimalFormat df3 = new DecimalFormat("#0.000");

    // Interface untuk callback
    interface HitungCallback {
        void onComplete(boolean success, JSONObject data);
    }

    interface ScatterDataCallback {
        void onDataReceived(JSONObject scatterData);
    }

    // Data class untuk popup antrian
    private static class BTMPopupData {
        String tanggal;
        int btNumber;
        JSONObject dataBacaan;
        JSONObject hasilPerhitungan;
        JSONObject scatterData;

        BTMPopupData(String tanggal, int btNumber, JSONObject dataBacaan,
                     JSONObject hasilPerhitungan, JSONObject scatterData) {
            this.tanggal = tanggal;
            this.btNumber = btNumber;
            this.dataBacaan = dataBacaan;
            this.hasilPerhitungan = hasilPerhitungan;
            this.scatterData = scatterData;
        }
    }

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

        // Set default values untuk modal
        setDefaultModalValues();

        // Modal muncul otomatis di awal
        showModal();

        // START MONITORING JARINGAN
        startNetworkMonitoring();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInternetAndShowToast();

        if (isInternetAvailable()) {
            if (offlineDb.hasUnsyncedDataBTM()) {
                syncAllOfflineDataWithPopup(() -> {
                    if (!isAlreadySynced()) {
                        showToast("✅ Sinkronisasi data offline selesai");
                        markAsSynced();
                    }
                });
            } else {
                prosesPerhitunganTertunda();
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

    // ==================== POPUP HASIL PERHITUNGAN BUBBLE TILT (CUSTOM LAYOUT) ====================

    private void showBubbleTiltResultPopup(String tanggal, int btNumber,
                                           JSONObject dataBacaan, JSONObject hasilPerhitungan,
                                           JSONObject scatterData, Runnable onPopupClosed) {
        try {
            // Inflate custom layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_bubbletilt_result, null);

            // Setup dialog dengan custom theme
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);

            // Setup window properties
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT
                );

                // Set gravity to center
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = Gravity.CENTER;
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(params);
            }

            // Bind views yang masih digunakan
            TextView tvBTInfo = dialogView.findViewById(R.id.tvBTInfo);
            TextView tvUSData = dialogView.findViewById(R.id.tvUSData);
            TextView tvUSArah = dialogView.findViewById(R.id.tvUSArah);
            TextView tvTBData = dialogView.findViewById(R.id.tvTBData);
            TextView tvTBArah = dialogView.findViewById(R.id.tvTBArah);

            TextView tvYUS = dialogView.findViewById(R.id.tvYUS);
            TextView tvXTB = dialogView.findViewById(R.id.tvXTB);
            TextView tvYCUM = dialogView.findViewById(R.id.tvYCUM);
            TextView tvXCUM = dialogView.findViewById(R.id.tvXCUM);

            ImageView btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
            MaterialButton btnOK = dialogView.findViewById(R.id.btnOK);

            // Set data header
            tvBTInfo.setText(String.format("BT%d - Tanggal: %s", btNumber, tanggal));

            // Set data bacaan
            if (dataBacaan != null) {
                try {
                    double us_gp = dataBacaan.optDouble("US_GP", 0);
                    String us_arah = dataBacaan.optString("US_Arah", "");
                    double tb_gp = dataBacaan.optDouble("TB_GP", 0);
                    String tb_arah = dataBacaan.optString("TB_Arah", "");

                    tvUSData.setText(df6.format(us_gp));
                    tvUSArah.setText(us_arah);
                    tvTBData.setText(df6.format(tb_gp));
                    tvTBArah.setText(tb_arah);
                } catch (Exception e) {
                    Log.e("POPUP_DATA_BACAAN", "Error parsing data bacaan: " + e.getMessage());
                }
            }

            // Set data scatter
            if (scatterData != null) {
                try {
                    double y_us = scatterData.optDouble("y_u0", 0);
                    double x_tb = scatterData.optDouble("x_tb", 0);
                    double y_cum = scatterData.optDouble("y_cum", 0);
                    double x_cum = scatterData.optDouble("x_cum", 0);

                    tvYUS.setText(df6.format(y_us));
                    tvXTB.setText(df6.format(x_tb));
                    tvYCUM.setText(df6.format(y_cum));
                    tvXCUM.setText(df6.format(x_cum));
                } catch (Exception e) {
                    Log.e("POPUP_SCATTER", "Error parsing scatter data: " + e.getMessage());
                }
            }

            // HAPUS BAGIAN HASIL PERHITUNGAN (A_sec, B_sec, sin_C_deg, DMS)
            // Karena sudah dihapus dari layout, tidak perlu di-set lagi

            // Setup click listeners
            btnCloseDialog.setOnClickListener(v -> {
                dialog.dismiss();
                handlePopupClose(onPopupClosed);
            });

            btnOK.setOnClickListener(v -> {
                dialog.dismiss();
                handlePopupClose(onPopupClosed);
            });

            dialog.setOnDismissListener(d -> {
                handlePopupClose(onPopupClosed);
            });

            // Show dialog
            dialog.show();
            isShowingPopup = true;

        } catch (Exception e) {
            Log.e("BUBBLETILT_DIALOG", "Error showing dialog: " + e.getMessage());
            showToast("❌ Gagal menampilkan hasil");
            handlePopupClose(onPopupClosed);
        }
    }

    private void handlePopupClose(Runnable onPopupClosed) {
        isShowingPopup = false;
        if (onPopupClosed != null) {
            onPopupClosed.run();
        }
    }

    // ==================== SISTEM ANTRIAN POPUP ====================

    private void addToPopupQueue(BTMPopupData popupData) {
        pendingPopups.add(popupData);

        // Jika tidak sedang menampilkan popup, langsung tampilkan
        if (!isShowingPopup) {
            showNextPendingPopup();
        }
    }

    private void showNextPendingPopup() {
        if (pendingPopups.isEmpty() || isShowingPopup) {
            return;
        }

        // Ambil popup pertama dari antrian
        BTMPopupData popupData = pendingPopups.remove(0);

        // Tampilkan popup dengan callback untuk lanjut ke berikutnya
        showBubbleTiltResultPopup(
                popupData.tanggal,
                popupData.btNumber,
                popupData.dataBacaan,
                popupData.hasilPerhitungan,
                popupData.scatterData,
                this::showNextPendingPopup
        );

        // Jika masih ada popup lain, tampilkan toast info
        if (!pendingPopups.isEmpty()) {
            showToast("📊 Masih ada " + pendingPopups.size() + " hasil perhitungan yang akan ditampilkan");
        }
    }

    // ==================== HITUNG BUBBLE TILT ONLINE LANGSUNG ====================

    private void hitungBubbleTiltOtomatis() {
        try {
            if (pengukuranId == -1) {
                showToast("❌ Pengukuran ID tidak valid");
                return;
            }

            String url = HITUNG_URL;

            JSONObject postData = new JSONObject();
            postData.put("pengukuran_id", pengukuranId);
            postData.put("bt_number", selectedBT);

            Log.d("BTM_HITUNG", "Mengirim request hitung untuk BT" + selectedBT + ", pengukuran_id: " + pengukuranId);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    postData,
                    response -> {
                        try {
                            Log.d("BTM_HITUNG", "Response Hitung: " + response.toString());

                            boolean success = response.optBoolean("success", false);
                            String message = response.optString("message", "");

                            if (success) {
                                // Ambil data dengan benar
                                JSONObject data = response.optJSONObject("data");
                                if (data == null) {
                                    data = response;
                                }

                                // Ambil scatter data
                                JSONObject scatterData = extractScatterData(data);

                                // Ambil hasil perhitungan
                                JSONObject hasilPerhitungan = extractHasilPerhitungan(data);

                                // Ambil tanggal
                                String tanggal = spinnerPengukuran.getSelectedItem() != null ?
                                        spinnerPengukuran.getSelectedItem().toString() :
                                        "Tanggal tidak tersedia";

                                // Buat data bacaan dari input form
                                JSONObject dataBacaan = new JSONObject();
                                dataBacaan.put("US_GP", inputUSGP.getText().toString().trim());
                                dataBacaan.put("US_Arah", spinnerUSArah.getSelectedItem().toString());
                                dataBacaan.put("TB_GP", inputTBGP.getText().toString().trim());
                                dataBacaan.put("TB_Arah", spinnerTBArah.getSelectedItem().toString());

                                // Buat popup data
                                BTMPopupData popupData = new BTMPopupData(
                                        tanggal, selectedBT, dataBacaan, hasilPerhitungan, scatterData
                                );
                                addToPopupQueue(popupData);

                                showToast("🧮 Perhitungan berhasil!");
                                clearForm();

                            } else {
                                showToast("⚠️ " + message);
                            }
                        } catch (Exception e) {
                            Log.e("BTM_HITUNG", "Error parsing response: " + e.getMessage());
                            showToast("❌ Gagal memproses hasil");
                        }
                    },
                    error -> {
                        Log.e("BTM_HITUNG", "Volley error: " + error.getMessage());
                        showToast("❌ Gagal terhubung ke server");
                    }
            );

            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    15000,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            Volley.newRequestQueue(getApplicationContext()).add(request);

        } catch (Exception e) {
            Log.e("BTM_HITUNG", "Error: " + e.getMessage());
            showToast("❌ Error: " + e.getMessage());
        }
    }

    // Method helper untuk ekstrak scatter data
    private JSONObject extractScatterData(JSONObject data) {
        JSONObject scatterData = new JSONObject();

        try {
            if (data != null && data.has("scatter")) {
                JSONObject scatter = data.getJSONObject("scatter");

                double y_us = scatter.getDouble("Y_US");
                double x_tb = scatter.getDouble("X_TB");
                double y_cum = scatter.getDouble("Y_cum");
                double x_cum = scatter.getDouble("X_cum");

                scatterData.put("y_u0", y_us);
                scatterData.put("x_tb", x_tb);
                scatterData.put("y_cum", y_cum);
                scatterData.put("x_cum", x_cum);

                Log.d("EXTRACT_SCATTER", "Nilai dari SERVER:");
                Log.d("EXTRACT_SCATTER", "y_u0: " + y_us);
                Log.d("EXTRACT_SCATTER", "x_tb: " + x_tb);
                Log.d("EXTRACT_SCATTER", "y_cum: " + y_cum);
                Log.d("EXTRACT_SCATTER", "x_cum: " + x_cum);

            } else {
                Log.e("EXTRACT_SCATTER", "Data scatter tidak ditemukan di response!");
                scatterData.put("y_u0", 0.0);
                scatterData.put("x_tb", 0.0);
                scatterData.put("y_cum", 0.0);
                scatterData.put("x_cum", 0.0);
            }

        } catch (JSONException e) {
            Log.e("EXTRACT_SCATTER", "Error parsing JSON: " + e.getMessage());
            try {
                scatterData.put("y_u0", 0.0);
                scatterData.put("x_tb", 0.0);
                scatterData.put("y_cum", 0.0);
                scatterData.put("x_cum", 0.0);
            } catch (JSONException ex) {
                // Ignore jika masih error
            }
        }

        return scatterData;
    }

    // Method helper untuk ekstrak hasil perhitungan
    private JSONObject extractHasilPerhitungan(JSONObject data) {
        JSONObject hasil = new JSONObject();

        try {
            if (data != null && data.has("perhitungan")) {
                JSONObject perhitungan = data.getJSONObject("perhitungan");
                hasil = perhitungan;
            } else {
                // Ambil langsung dari data
                hasil.put("A_sec", data.optDouble("A_sec", 0));
                hasil.put("B_sec", data.optDouble("B_sec", 0));
                hasil.put("sin_C_deg", data.optDouble("sin_C_deg", 0));
                hasil.put("DMS", data.optString("DMS", "0° 0' 0\""));
            }
        } catch (Exception e) {
            Log.e("EXTRACT_HASIL", "Error extracting hasil: " + e.getMessage());
            try {
                hasil.put("A_sec", 0.0);
                hasil.put("B_sec", 0.0);
                hasil.put("sin_C_deg", 0.0);
                hasil.put("DMS", "0° 0' 0\"");
            } catch (Exception ex) {
                // Ignore
            }
        }

        return hasil;
    }

    // ==================== HITUNG BUBBLE TILT UNTUK SINKRONISASI ====================

    private void hitungBTSingle(int btNumber, int pengukuranId, boolean addToQueue, HitungCallback callback) {
        new Thread(() -> {
            boolean success = false;
            JSONObject resultData = null;
            JSONObject dataBacaanFromDB = null;

            try {
                // Ambil data bacaan dari database
                Map<String, String> bubbleData = offlineDb.getBTMData(pengukuranId, btNumber);

                if (bubbleData != null && !bubbleData.isEmpty()) {
                    dataBacaanFromDB = new JSONObject();
                    dataBacaanFromDB.put("US_GP", bubbleData.getOrDefault("us_gp", "0"));
                    dataBacaanFromDB.put("US_Arah", bubbleData.getOrDefault("us_arah", ""));
                    dataBacaanFromDB.put("TB_GP", bubbleData.getOrDefault("tb_gp", "0"));
                    dataBacaanFromDB.put("TB_Arah", bubbleData.getOrDefault("tb_arah", ""));

                    Log.d("HITUNG_BTM_DATA", "Data dari database - US: " +
                            bubbleData.get("us_gp") + bubbleData.get("us_arah") +
                            ", TB: " + bubbleData.get("tb_gp") + bubbleData.get("tb_arah"));
                }

                // Hitung dengan data yang ada
                String url = HITUNG_URL;

                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject postData = new JSONObject();
                postData.put("pengukuran_id", pengukuranId);
                postData.put("bt_number", btNumber);

                OutputStream os = conn.getOutputStream();
                os.write(postData.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    String responseBody = sb.toString();
                    JSONObject response = new JSONObject(responseBody);

                    boolean successResponse = response.optBoolean("success", false);
                    success = successResponse;

                    if (success && response.has("data")) {
                        resultData = response.getJSONObject("data");

                        // Tambah ke antrian popup
                        if (addToQueue && resultData != null) {
                            JSONObject scatterData = extractScatterData(resultData);
                            JSONObject hasilPerhitungan = extractHasilPerhitungan(resultData);

                            // Buat final copy untuk lambda
                            final JSONObject finalScatterData = scatterData;
                            final JSONObject finalHasilPerhitungan = hasilPerhitungan;
                            final JSONObject finalDataBacaan = dataBacaanFromDB;

                            runOnUiThread(() -> {
                                try {
                                    String tanggal = spinnerPengukuran.getSelectedItem() != null ?
                                            spinnerPengukuran.getSelectedItem().toString() :
                                            "Tanggal tidak tersedia";

                                    // Gunakan data bacaan dari database jika ada
                                    JSONObject dataBacaan = finalDataBacaan != null ?
                                            finalDataBacaan : new JSONObject();

                                    // Tambah ke antrian popup
                                    BTMPopupData popupData = new BTMPopupData(
                                            tanggal, btNumber, dataBacaan,
                                            finalHasilPerhitungan, finalScatterData
                                    );
                                    addToPopupQueue(popupData);

                                    Log.d("HITUNG_BTM_POPUP", "✅ Popup ditambahkan untuk BT" + btNumber);

                                } catch (Exception e) {
                                    Log.e("HITUNG_BTM_POPUP", "Error creating popup data: " + e.getMessage());
                                }
                            });
                        }
                    }

                    Log.d("HITUNG_BTM", "✅ Berhasil hitung BT" + btNumber + " untuk pengukuran " + pengukuranId);
                } else {
                    Log.e("HITUNG_BTM", "❌ Gagal hitung BT" + btNumber + ", response code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("HITUNG_BTM", "❌ Error hitung BT" + btNumber + " untuk pengukuran " + pengukuranId + ": " + e.getMessage());
            } finally {
                callback.onComplete(success, resultData);
            }
        }).start();
    }

    // ==================== FITUR SINKRONISASI OTOMATIS ====================

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
        boolean hasPendingCalc = hasPendingCalculations();

        if (offlineCount > 0 || hasPendingCalc) {
            Log.d("BTM_AutoSync", "Found " + offlineCount + " offline data and " +
                    (hasPendingCalc ? "pending calculations" : "no pending calculations"));
            triggerAutoSync();
        }
    }

    private boolean hasPendingCalculations() {
        SharedPreferences prefs = getSharedPreferences("btm_pending_calc", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        return !allEntries.isEmpty();
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
            prosesPerhitunganTertunda();
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerialAuto("pengukuran", () ->
                syncDataSerialAuto("data", () -> {
                    showToast("✅ " + offlineCount + " data terkirim");
                    prosesPerhitunganTertunda();
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

                        // Tandai perlu perhitungan jika ini data bubbletilt
                        if (tableType.equals("data") && json.has("mode")) {
                            String mode = json.optString("mode", "");
                            if (mode.equals("bubbletilt")) {
                                int pengukuranId = json.optInt("pengukuran_id", -1);
                                int btNumber = json.optInt("bt_number", 1);
                                if (pengukuranId != -1) {
                                    tandaiPerluHitungBTM(tempId, pengukuranId, btNumber);
                                }
                            }
                        }
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

    // ==================== SYNC DENGAN POPUP (VERSI SIMPLIFIED) ====================

    private void syncAllOfflineDataWithPopup(Runnable onComplete) {
        boolean adaData = offlineDb.hasUnsyncedDataBTM();
        if (!adaData) {
            prosesPerhitunganTertunda();
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerialWithPopup("pengukuran", () -> {
            syncDataSerialWithPopup("data", onComplete);
        });
    }

    private void syncDataSerialWithPopup(String tableType, Runnable next) {
        List<Map<String, String>> dataList = offlineDb.getUnsyncedDataBTM(tableType);
        if (dataList.isEmpty()) {
            if (next != null) next.run();
            return;
        }
        syncDataItemWithPopup(tableType, dataList, 0, next);
    }

    private void syncDataItemWithPopup(String tableType, List<Map<String, String>> dataList, int index, Runnable onFinish) {
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

                        // Jika ini data bubbletilt yang berhasil disinkron, proses perhitungan
                        if (tableType.equals("data") && jsonData.has("mode")) {
                            String mode = jsonData.optString("mode", "");
                            if (mode.equals("bubbletilt")) {
                                int pengukuranId = jsonData.optInt("pengukuran_id", -1);
                                int btNumber = jsonData.optInt("bt_number", 1);

                                if (pengukuranId != -1) {
                                    // Langsung hitung dan tampilkan popup
                                    hitungBTSingleForSync(btNumber, pengukuranId);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("BTM_Sync", "Error sync " + tableType + " tempId=" + tempId, e);
                } finally {
                    if (conn != null) conn.disconnect();
                }

                runOnUiThread(() -> syncDataItemWithPopup(tableType, dataList, index + 1, onFinish));
            }).start();

        } catch (Exception e) {
            Log.e("BTM_Sync", "JSON parse error untuk data " + tableType + " tempId=" + tempId, e);
            runOnUiThread(() -> syncDataItemWithPopup(tableType, dataList, index + 1, onFinish));
        }
    }

    private void hitungBTSingleForSync(int btNumber, int pengukuranId) {
        hitungBTSingle(btNumber, pengukuranId, true, (success, data) -> {
            if (success) {
                Log.d("BTM_Sync", "Successfully calculated bubbletilt for BT" + btNumber + ", pengukuran " + pengukuranId);
            }
        });
    }

    // ==================== FITUR PERHITUNGAN BUBBLE TILT ====================

    private void tandaiPerluHitungBTM(String tempId, int pengukuranId, int btNumber) {
        SharedPreferences prefs = getSharedPreferences("btm_pending_calc", MODE_PRIVATE);
        String key = "pending_" + tempId;

        Map<String, String> pendingData = new HashMap<>();
        pendingData.put("pengukuran_id", String.valueOf(pengukuranId));
        pendingData.put("bt_number", String.valueOf(btNumber));
        pendingData.put("temp_id", tempId);

        try {
            JSONObject json = new JSONObject(pendingData);
            prefs.edit().putString(key, json.toString()).apply();
            Log.d("PENDING_CALC_BTM", "Data ditandai perlu hitung: " + json.toString());
        } catch (Exception e) {
            Log.e("PENDING_CALC_BTM", "Gagal menyimpan pending calculation: " + e.getMessage());
        }
    }

    private void prosesPerhitunganTertunda() {
        if (!isInternetAvailable()) return;

        SharedPreferences prefs = getSharedPreferences("btm_pending_calc", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        if (allEntries.isEmpty()) return;

        Log.d("PENDING_CALC_BTM", "Processing " + allEntries.size() + " pending calculations");
        showToast("🔄 Memproses " + allEntries.size() + " perhitungan tertunda...");

        // Proses satu per satu
        List<Map.Entry<String, ?>> entries = new ArrayList<>(allEntries.entrySet());
        prosesPendingEntries(entries, 0);
    }

    private void prosesPendingEntries(List<Map.Entry<String, ?>> entries, int index) {
        if (index >= entries.size()) {
            runOnUiThread(() -> {
                // Tunggu sebentar sebelum menampilkan toast selesai
                new Handler().postDelayed(() -> {
                    if (pendingPopups.isEmpty()) {
                        showToast("✅ Semua perhitungan tertunda selesai");
                    }
                }, 1000);
            });
            return;
        }

        Map.Entry<String, ?> entry = entries.get(index);
        if (entry.getKey().startsWith("pending_")) {
            try {
                String jsonStr = (String) entry.getValue();
                JSONObject json = new JSONObject(jsonStr);

                int pendingPengukuranId = json.getInt("pengukuran_id");
                int pendingBtNumber = json.getInt("bt_number");
                String pendingTempId = json.getString("temp_id");

                Log.d("PENDING_CALC_BTM", "Processing pending calculation for BT" + pendingBtNumber + ", pengukuran " + pendingPengukuranId);

                hitungBTSingle(pendingBtNumber, pendingPengukuranId, true, (success, data) -> {
                    if (success) {
                        // Hapus dari pending setelah sukses
                        SharedPreferences prefs = getSharedPreferences("btm_pending_calc", MODE_PRIVATE);
                        prefs.edit().remove(entry.getKey()).apply();
                    }
                    prosesPendingEntries(entries, index + 1);
                });

            } catch (Exception e) {
                Log.e("PENDING_CALC_BTM", "Error processing pending calculation: " + e.getMessage());
                prosesPendingEntries(entries, index + 1);
            }
        } else {
            prosesPendingEntries(entries, index + 1);
        }
    }

    // ==================== METHOD INIT COMPONENTS ====================

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

        btnSimpanHitung = findViewById(R.id.btnSimpanHitung);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        spinnerBT = findViewById(R.id.spinnerBT);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
        titleBubbleTilt = findViewById(R.id.titleBubbleTilt);

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

    // ==================== METHOD UTILITAS ====================

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
            tandaiPerluHitungBTM(localTempId, pengukuranId, selectedBT);
            // Clear form setelah simpan offline
            clearForm();
        }
    }

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
                    // Tangani semua status response
                    if (status.equalsIgnoreCase("success") || status.equalsIgnoreCase("info")) {
                        showToast("✅ " + message);
                        // Tunggu sebentar sebelum hitung untuk memastikan data tersimpan
                        new Handler().postDelayed(() -> {
                            hitungBubbleTiltOtomatis();
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
                    tandaiPerluHitungBTM(localTempId, pengukuranId, selectedBT);
                    clearForm();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private boolean validateBubbleTiltFields() {
        String usArah = spinnerUSArah.getSelectedItem().toString();
        String tbArah = spinnerTBArah.getSelectedItem().toString();

        return !inputUSGP.getText().toString().trim().isEmpty() &&
                !inputTBGP.getText().toString().trim().isEmpty() &&
                !usArah.equals("Pilih Arah US") &&
                !tbArah.equals("Pilih Arah TB");
    }

    // ==================== METHOD MODAL ====================

    private void setDefaultModalValues() {
        // Set current date as default
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(Calendar.getInstance().getTime());
        modalInputTanggal.setText(currentDate);

        // Set current year as default
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        modalInputTahun.setText(String.valueOf(currentYear));

        // Set current month as default
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        String[] bulanArray = getResources().getStringArray(R.array.bulan_options);
        if (currentMonth >= 0 && currentMonth < bulanArray.length) {
            modalInputBulan.setText(bulanArray[currentMonth], false);
        }

        // Set current periode as default
        String currentPeriode;
        if (currentMonth <= 2) {
            currentPeriode = "TW-1";
        } else if (currentMonth <= 5) {
            currentPeriode = "TW-2";
        } else if (currentMonth <= 8) {
            currentPeriode = "TW-3";
        } else {
            currentPeriode = "TW-4";
        }
        modalInputPeriode.setText(currentPeriode, false);
    }

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

        // Load data pengukuran setelah modal ditutup
        loadPengukuranData();
    }

    // ==================== METHOD DATABASE & SYNC ====================

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

    private void saveOffline(String tableType, String tempId, Map<String, String> data) {
        try {
            JSONObject json = new JSONObject(data);
            boolean success = offlineDb.insertDataBTM(tableType, tempId, json.toString());
            if (success) {
                Log.d("BTM_Offline", "Data disimpan offline (" + tableType + ")");
            } else {
                showToast("❌ Gagal simpan offline");
            }
        } catch (Exception e) {
            Log.e("BTM_Offline", "Gagal simpan offline: " + e.getMessage());
            showToast("❌ Gagal simpan offline: " + e.getMessage());
        }
    }

    // ==================== METHOD PENGUKURAN ====================

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

    // ==================== METHOD UTILITY ====================

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