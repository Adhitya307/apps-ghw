package com.example.app.exstenso;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class InputDataExstenso extends AppCompatActivity {

    // Komponen modal pengukuran
    private CardView modalPengukuran;
    private View modalOverlay;
    private ImageButton btnCloseModal;
    private ScrollView mainContent;

    // Input field dalam modal
    private EditText modalInputTahun;
    private AutoCompleteTextView modalInputBulan, modalInputPeriode;
    private EditText modalInputTanggal;
    private Button modalBtnSubmitPengukuran;

    // Komponen form utama Exstenso
    private TextInputEditText inputDMA, inputPembacaan10, inputPembacaan20, inputPembacaan30;
    private Button btnSimpanDMA, btnSimpanPembacaan;
    private Spinner spinnerPengukuran, spinnerExType;
    private Button btnPilihPengukuran;
    private TextView titleExstenso;

    private Calendar calendar;
    private int pengukuranId = -1;
    private String selectedExType = "pembacaan_ex1";

    private static final String BASE_URL = "http://192.168.1.11/GHW/api-apps/public/exstenso/";
    private static final String INSERT_DATA_URL = BASE_URL + "inputdata";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "getpengukuran";
    private static final String GET_DATA_URL = BASE_URL + "getdata";

    private final Map<String, Integer> pengukuranMap = new HashMap<>();
    private final List<String> tanggalList = new ArrayList<>();
    private ArrayAdapter<String> pengukuranAdapter;
    private ArrayAdapter<String> exTypeAdapter;

    private String currentTahun = "";
    private String currentTanggal = "";
    private String currentPeriode = "";

    private OfflineDataHelperExstenso offlineDb;
    private SharedPreferences syncPrefs;
    private boolean isSyncInProgress = false;
    private Handler networkCheckHandler = new Handler();
    private Runnable networkCheckRunnable;
    private boolean lastOnlineStatus = false;

    // Untuk sinkronisasi dengan popup berurutan
    private List<PopupData> pendingPopups = new ArrayList<>();
    private boolean isShowingPopup = false;

    // Konstanta untuk nilai ambang batas dan pembacaan awal
    private static final Map<String, Double[]> EX_THRESHOLDS = new HashMap<>();
    private static final Map<String, double[]> EX_INITIAL_READINGS = new HashMap<>();
    private static final Map<String, String> EX_NAMES = new HashMap<>();

    static {
        // Data untuk EX1
        Double[] ex1Thresholds = {80.10, 104.00, 110.90};
        EX_THRESHOLDS.put("pembacaan_ex1", ex1Thresholds);
        EX_INITIAL_READINGS.put("pembacaan_ex1", new double[]{35.0, 40.95, 29.80});
        EX_NAMES.put("pembacaan_ex1", "EX1");

        // Data untuk EX2
        Double[] ex2Thresholds = {46.0, 80.0, 81.0};
        EX_THRESHOLDS.put("pembacaan_ex2", ex2Thresholds);
        EX_INITIAL_READINGS.put("pembacaan_ex2", new double[]{22.60, 23.70, 30.75});
        EX_NAMES.put("pembacaan_ex2", "EX2");

        // Data untuk EX3
        Double[] ex3Thresholds = {80.10, 104.0, 110.90};
        EX_THRESHOLDS.put("pembacaan_ex3", ex3Thresholds);
        EX_INITIAL_READINGS.put("pembacaan_ex3", new double[]{37.75, 39.15, 41.40});
        EX_NAMES.put("pembacaan_ex3", "EX3");

        // Data untuk EX4
        Double[] ex4Thresholds = {46.0, 80.0, 81.0};
        EX_THRESHOLDS.put("pembacaan_ex4", ex4Thresholds);
        EX_INITIAL_READINGS.put("pembacaan_ex4", new double[]{33.80, 29.30, 48.95});
        EX_NAMES.put("pembacaan_ex4", "EX4");
    }

    // Interface untuk callback
    interface HitungCallback {
        void onComplete(boolean success, JSONObject data);
    }

    // Data class untuk popup antrian
    private static class PopupData {
        String tanggal;
        String exType;
        JSONObject data;

        PopupData(String tanggal, String exType, JSONObject data) {
            this.tanggal = tanggal;
            this.exType = exType;
            this.data = data;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data_exstenso);

        offlineDb = new OfflineDataHelperExstenso(this);
        syncPrefs = getSharedPreferences("exstenso_sync_prefs", MODE_PRIVATE);
        calendar = Calendar.getInstance();

        initModalComponents();
        initFormComponents();
        initSpinnerComponents();
        setupModalDropdowns();
        setupModalCalendar();

        loadPengukuranData();
        showModal();
        startNetworkMonitoring();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInternetAndShowToast();

        if (isInternetAvailable()) {
            if (offlineDb.hasUnsyncedDataExstenso()) {
                syncAllOfflineData(() -> {
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

    // ==================== MODIFIKASI showDeformationResultPopup ====================

    private void showDeformationResultPopup(String tanggal, String exType, JSONObject data, Runnable onPopupClosed) {
        try {
            // Inflate custom layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_deformation_result, null);

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

            // Bind all views
            TextView tvExtInfo = dialogView.findViewById(R.id.tvExtInfo);
            TextView tvThresholdAman = dialogView.findViewById(R.id.tvThresholdAman);
            TextView tvThresholdPeringatan = dialogView.findViewById(R.id.tvThresholdPeringatan);
            TextView tvThresholdBahaya = dialogView.findViewById(R.id.tvThresholdBahaya);

            // 10 Meter views
            TextView tvDeformasi10 = dialogView.findViewById(R.id.tvDeformasi10);
            TextView tvPembAwal10 = dialogView.findViewById(R.id.tvPembAwal10);
            TextView tvBacaanAkhir10 = dialogView.findViewById(R.id.tvBacaanAkhir10);
            TextView tvStatus10 = dialogView.findViewById(R.id.tvStatus10);

            // 20 Meter views
            TextView tvDeformasi20 = dialogView.findViewById(R.id.tvDeformasi20);
            TextView tvPembAwal20 = dialogView.findViewById(R.id.tvPembAwal20);
            TextView tvBacaanAkhir20 = dialogView.findViewById(R.id.tvBacaanAkhir20);
            TextView tvStatus20 = dialogView.findViewById(R.id.tvStatus20);

            // 30 Meter views
            TextView tvDeformasi30 = dialogView.findViewById(R.id.tvDeformasi30);
            TextView tvPembAwal30 = dialogView.findViewById(R.id.tvPembAwal30);
            TextView tvBacaanAkhir30 = dialogView.findViewById(R.id.tvBacaanAkhir30);
            TextView tvStatus30 = dialogView.findViewById(R.id.tvStatus30);

            // Buttons
            ImageView btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
            MaterialButton btnOK = dialogView.findViewById(R.id.btnOK);


            // Set data
            String exName = EX_NAMES.get(exType);
            tvExtInfo.setText(String.format("%s - Tanggal: %s", exName, tanggal));

            // Get thresholds
            Double[] thresholds = EX_THRESHOLDS.get(exType);
            if (thresholds != null) {
                tvThresholdAman.setText(String.format("≤ %.2f", thresholds[0]));
                tvThresholdPeringatan.setText(String.format("%.2f - %.2f", thresholds[0] + 0.01, thresholds[1]));
                tvThresholdBahaya.setText(String.format("≥ %.2f", thresholds[1] + 0.01));
            }

            // Format angka dengan 2 desimal
            DecimalFormat df = new DecimalFormat("#0.00");

            // Process and display 10 meter data
            processAndDisplayData(
                    data, exType, 10,
                    tvDeformasi10, tvPembAwal10, tvBacaanAkhir10, tvStatus10, df
            );

            // Process and display 20 meter data
            processAndDisplayData(
                    data, exType, 20,
                    tvDeformasi20, tvPembAwal20, tvBacaanAkhir20, tvStatus20, df
            );

            // Process and display 30 meter data
            processAndDisplayData(
                    data, exType, 30,
                    tvDeformasi30, tvPembAwal30, tvBacaanAkhir30, tvStatus30, df
            );

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
            Log.e("DEFORMATION_DIALOG", "Error showing dialog: " + e.getMessage());
            showToast("❌ Gagal menampilkan hasil");
            handlePopupClose(onPopupClosed);
        }
    }

    private void processAndDisplayData(JSONObject data, String exType, int meter,
                                       TextView tvDeformasi, TextView tvPembAwal,
                                       TextView tvBacaanAkhir, TextView tvStatus,
                                       DecimalFormat df) {
        try {
            // Get keys based on data structure
            String deformasiKey = "deformasi_" + meter;
            String pembacaanAwalKey = "pembacaan_awal" + meter;

            // Try alternative keys if main keys not found
            if (!data.has(deformasiKey)) {
                deformasiKey = "deformasi" + meter;
            }
            if (!data.has(pembacaanAwalKey)) {
                pembacaanAwalKey = "pemb_awal" + meter;
            }

            // Get values
            double deformasi = data.optDouble(deformasiKey, 0);
            double[] initialReadings = EX_INITIAL_READINGS.get(exType);
            double pembacaanAwal;

            if (initialReadings != null && meter == 10) {
                pembacaanAwal = data.optDouble(pembacaanAwalKey, initialReadings[0]);
            } else if (initialReadings != null && meter == 20) {
                pembacaanAwal = data.optDouble(pembacaanAwalKey, initialReadings[1]);
            } else if (initialReadings != null && meter == 30) {
                pembacaanAwal = data.optDouble(pembacaanAwalKey, initialReadings[2]);
            } else {
                pembacaanAwal = data.optDouble(pembacaanAwalKey, 0);
            }

            double bacaanAkhir = pembacaanAwal + deformasi;

            // Set text
            tvDeformasi.setText(df.format(deformasi));
            tvPembAwal.setText(df.format(pembacaanAwal));
            tvBacaanAkhir.setText(df.format(bacaanAkhir));

            // Determine status
            Double[] thresholds = EX_THRESHOLDS.get(exType);
            if (thresholds != null) {
                String status = getStatus(bacaanAkhir, thresholds[1], thresholds[2]);
                tvStatus.setText(status);
                setStatusBackground(tvStatus, status);
            }

        } catch (Exception e) {
            Log.e("PROCESS_DATA", "Error processing data for " + meter + "m: " + e.getMessage());
        }
    }

    private void setStatusBackground(TextView tvStatus, String status) {
        switch (status) {
            case "AMAN":
                tvStatus.setBackgroundResource(R.drawable.status_aman_bg);
                break;
            case "PERINGATAN":
                tvStatus.setBackgroundResource(R.drawable.status_warning_bg);
                break;
            case "BAHAYA":
                tvStatus.setBackgroundResource(R.drawable.status_danger_bg);
                break;
        }
    }

    private void handlePopupClose(Runnable onPopupClosed) {
        isShowingPopup = false;
        if (onPopupClosed != null) {
            onPopupClosed.run();
        }
    }

    private View createSeparator() {
        View separator = new View(this);
        separator.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        separator.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(0, 10, 0, 10);
        separator.setLayoutParams(params);
        return separator;
    }

    private LinearLayout createResultCard(String title, double deformasi, double pembacaanAwal,
                                          double aman, double peringatan, double bahaya,
                                          double bacaanAkhir) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(15, 15, 15, 15);
        card.setBackgroundColor(Color.WHITE);

        // Tambahkan border
        try {
            card.setBackground(getResources().getDrawable(R.drawable.border));
        } catch (Exception e) {
            card.setElevation(4f);
        }

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 10);
        card.addView(tvTitle);

        // Deformasi
        TextView tvDeformasi = new TextView(this);
        tvDeformasi.setText("        DEFORMASI = " + String.format("%.2f", deformasi));
        tvDeformasi.setTextSize(14);
        tvDeformasi.setPadding(20, 0, 0, 5);
        card.addView(tvDeformasi);

        // Pembacaan Awal
        TextView tvAwal = new TextView(this);
        tvAwal.setText("        PEMBACAAN AWAL = " + String.format("%.2f", pembacaanAwal));
        tvAwal.setTextSize(14);
        tvAwal.setPadding(20, 0, 0, 5);
        card.addView(tvAwal);

        // Ambang Batas
        TextView tvAmbang = new TextView(this);
        tvAmbang.setText("        AMAN = " + String.format("%.2f", aman));
        tvAmbang.setTextSize(14);
        tvAmbang.setPadding(20, 0, 0, 5);
        card.addView(tvAmbang);

        TextView tvPeringatan = new TextView(this);
        tvPeringatan.setText("        PERINGATAN = " + String.format("%.2f", peringatan));
        tvPeringatan.setTextSize(14);
        tvPeringatan.setPadding(20, 0, 0, 5);
        card.addView(tvPeringatan);

        TextView tvBahaya = new TextView(this);
        tvBahaya.setText("        BAHAYA = " + String.format("%.2f", bahaya));
        tvBahaya.setTextSize(14);
        tvBahaya.setPadding(20, 0, 0, 5);
        card.addView(tvBahaya);

        // Bacaan Akhir
        TextView tvBacaan = new TextView(this);
        tvBacaan.setText("        BACAAN = PEMBACAAN AWAL + DEFORMASI");
        tvBacaan.setTextSize(14);
        tvBacaan.setPadding(20, 0, 0, 5);
        card.addView(tvBacaan);

        TextView tvPerhitungan = new TextView(this);
        tvPerhitungan.setText("                = " + String.format("%.2f", pembacaanAwal) + " + " +
                String.format("%.2f", deformasi) + " = " +
                String.format("%.2f", bacaanAkhir));
        tvPerhitungan.setTextSize(14);
        tvPerhitungan.setPadding(30, 0, 0, 5);
        card.addView(tvPerhitungan);

        // Status - menggunakan logika lama
        String status = getStatus(bacaanAkhir, peringatan, bahaya);
        TextView tvStatus = new TextView(this);
        tvStatus.setText("        STATUS = " + status);
        tvStatus.setTextSize(14);
        tvStatus.setPadding(20, 0, 0, 0);
        tvStatus.setTextColor(getStatusColor(status));
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvStatus);

        return card;
    }

    private String getStatus(double bacaanAkhir, double peringatan, double bahaya) {
        if (bacaanAkhir <= peringatan) {
            return "AMAN";
        } else if (bacaanAkhir <= bahaya) {
            return "PERINGATAN";
        } else {
            return "BAHAYA";
        }
    }

    private int getStatusColor(String status) {
        if (status.contains("BAHAYA")) {
            return Color.RED;
        } else if (status.contains("PERINGATAN")) {
            return Color.parseColor("#FFA500"); // Orange
        } else {
            return Color.GREEN;
        }
    }

    // ==================== METODE UNTUK SINKRONISASI DENGAN POPUP BERURUTAN ====================

    private void showNextPendingPopup() {
        if (pendingPopups.isEmpty() || isShowingPopup) {
            return;
        }

        // Ambil popup pertama dari antrian
        PopupData popupData = pendingPopups.remove(0);

        // Tampilkan popup dengan callback untuk lanjut ke berikutnya
        showDeformationResultPopup(popupData.tanggal, popupData.exType, popupData.data, () -> {
            // Setelah popup ditutup, tampilkan popup berikutnya
            showNextPendingPopup();
        });

        // Jika masih ada popup lain, tampilkan toast info
        if (!pendingPopups.isEmpty()) {
            showToast("📊 Masih ada " + pendingPopups.size() + " hasil perhitungan yang akan ditampilkan");
        }
    }

    private void addToPopupQueue(String tanggal, String exType, JSONObject data) {
        pendingPopups.add(new PopupData(tanggal, exType, data));

        // Jika tidak sedang menampilkan popup, langsung tampilkan
        if (!isShowingPopup) {
            showNextPendingPopup();
        }
    }

    // ==================== hitungDeformasiOtomatis (UNTUK ONLINE LANGSUNG) ====================

    private void hitungDeformasiOtomatis() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String hitungEndpoint = getHitungEndpoint(selectedExType);
                String url = BASE_URL + hitungEndpoint;
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject jsonData = new JSONObject();
                jsonData.put("id_pengukuran", pengukuranId);

                String jsonString = jsonData.toString();
                Log.d("EXSTENSO_API", "Hitung deformasi " + selectedExType + ": " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("EXSTENSO_API", "Response Code (Hitung): " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("EXSTENSO_API", "Response Body (Hitung): " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                runOnUiThread(() -> {
                    if (status.equals("success")) {
                        showToast("🧮 Deformasi berhasil dihitung!");

                        // AMBIL DATA LANGSUNG DARI RESPONSE HITUNG DEFORMASI
                        try {
                            JSONObject data = response.getJSONObject("data");
                            String tanggal = spinnerPengukuran.getSelectedItem().toString();

                            // Tampilkan popup langsung (tanpa antrian untuk input langsung)
                            showDeformationResultPopup(tanggal, selectedExType, data, null);

                            // Clear field setelah berhasil
                            clearPembacaanSection();

                        } catch (Exception e) {
                            Log.e("HITUNG_DEFORMASI", "Error parsing data: " + e.getMessage());
                            showToast("✅ Data tersimpan, tapi gagal parsing hasil");
                        }
                    } else {
                        showToast("❌ Gagal hitung deformasi: " + message);
                    }
                });

            } catch (Exception e) {
                Log.e("HITUNG_DEFORMASI", "Error hitung deformasi: " + e.getMessage());
                runOnUiThread(() -> {
                    showToast("❌ Error hitung deformasi: " + e.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ==================== hitungDeformasiSingle (UNTUK SINKRONISASI) ====================

    private void hitungDeformasiSingle(String exType, int pengukuranId, boolean addToQueue, HitungCallback callback) {
        new Thread(() -> {
            boolean success = false;
            JSONObject resultData = null;
            HttpURLConnection conn = null;
            try {
                String hitungEndpoint = getHitungEndpoint(exType);
                String url = BASE_URL + hitungEndpoint;

                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject jsonData = new JSONObject();
                jsonData.put("id_pengukuran", pengukuranId);

                String jsonString = jsonData.toString();
                Log.d("HITUNG_DEFORMASI", "Hitung " + exType + " untuk pengukuran " + pengukuranId + ": " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    success = true;

                    // BACA RESPONSE UNTUK DAPATKAN DATA
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    String responseBody = sb.toString();
                    JSONObject response = new JSONObject(responseBody);

                    if (response.has("data")) {
                        resultData = response.getJSONObject("data");

                        // Jika perlu ditambahkan ke antrian popup
                        if (addToQueue && resultData != null) {
                            // BUAT VARIABLE FINAL COPY untuk digunakan di lambda
                            final JSONObject finalResultData = resultData;
                            String tanggal = spinnerPengukuran.getSelectedItem() != null ?
                                    spinnerPengukuran.getSelectedItem().toString() :
                                    "Tanggal tidak tersedia";

                            // Copy variabel untuk keamanan
                            final String finalTanggal = tanggal;
                            final String finalExType = exType;

                            runOnUiThread(() -> {
                                addToPopupQueue(finalTanggal, finalExType, finalResultData);
                            });
                        }
                    }

                    Log.d("HITUNG_DEFORMASI", "Berhasil hitung " + exType + " untuk pengukuran " + pengukuranId);
                } else {
                    Log.e("HITUNG_DEFORMASI", "Gagal hitung " + exType + ", response code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("HITUNG_DEFORMASI", "Error hitung " + exType + " untuk pengukuran " + pengukuranId + ": " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
                callback.onComplete(success, resultData);
            }
        }).start();
    }

    // ==================== MODIFIKASI sendToServerWithHitung ====================

    private void sendToServerWithHitung(Map<String, String> dataMap, String dataType) {
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
                Log.d("EXSTENSO_API", "JSON yang dikirim (" + dataType + "): " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("EXSTENSO_API", "Response Code (" + dataType + "): " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("EXSTENSO_API", "Response Body (" + dataType + "): " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            showToast("✅ " + message);

                            // HITUNG DEFORMASI SETELAH SIMPAN DATA PEMBACAAN
                            if (dataType.equals("Pembacaan")) {
                                hitungDeformasiOtomatis(); // Online langsung, tampil langsung
                            } else {
                                clearPembacaanSection();
                            }
                            break;
                        case "info":
                            showToast("ℹ️ " + message);
                            break;
                        case "error":
                        default:
                            showToast("❌ " + message);
                            if (!dataMap.containsKey("temp_id")) {
                                String localTempId = "local_" + System.currentTimeMillis();
                                dataMap.put("temp_id", localTempId);
                                saveOffline("data", localTempId, dataMap);
                            }
                            break;
                    }
                });

            } catch (Exception e) {
                Log.e("SEND_TO_SERVER", "Error (" + dataType + "): " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal kirim " + dataType + ": " + e.getMessage() + ". Data disimpan offline.");
                    if (!dataMap.containsKey("temp_id")) {
                        String localTempId = "local_" + System.currentTimeMillis();
                        dataMap.put("temp_id", localTempId);
                        saveOffline("data", localTempId, dataMap);
                    }
                });
            } finally {
                if (conn != null) conn.disconnect();
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

        int offlineCount = offlineDb.getOfflineDataCountExstenso();
        boolean hasPendingCalc = hasPendingCalculations();

        if (offlineCount > 0 || hasPendingCalc) {
            Log.d("EXSTENSO_AutoSync", "Found " + offlineCount + " offline data and " +
                    (hasPendingCalc ? "pending calculations" : "no pending calculations"));
            triggerAutoSync();
        }
    }

    private boolean hasPendingCalculations() {
        SharedPreferences prefs = getSharedPreferences("exstenso_pending_calc", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        return !allEntries.isEmpty();
    }

    private void triggerAutoSync() {
        if (isSyncInProgress) return;

        Log.d("EXSTENSO_AutoSync", "Triggering auto-sync for offline data");
        isSyncInProgress = true;
        showToast("🔄 Auto-sync data offline...");

        syncAllOfflineDataAuto(() -> {
            isSyncInProgress = false;
            Log.d("EXSTENSO_AutoSync", "Auto-sync completed");
            runOnUiThread(this::loadPengukuranData);
        });
    }

    private void syncAllOfflineDataAuto(Runnable onComplete) {
        int offlineCount = offlineDb.getOfflineDataCountExstenso();
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
        List<Map<String,String>> list = offlineDb.getUnsyncedDataExstenso(tableType);
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
            offlineDb.deleteByTempIdExstenso(tableType, tempId);
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
                        offlineDb.deleteByTempIdExstenso(tableType, tempId);
                        Log.d("EXSTENSO_AutoSync", "Synced " + tableType + " tempId=" + tempId);

                        if (tableType.equals("data") && json.has("mode")) {
                            String mode = json.optString("mode", "");
                            if (mode.startsWith("pembacaan_ex")) {
                                int pengukuranId = json.optInt("pengukuran_id", -1);
                                if (pengukuranId != -1) {
                                    tandaiPerluHitungDeformasi(tempId, pengukuranId, mode);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("EXSTENSO_AutoSync", "Failed to sync tempId=" + tempId + ": " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            } catch (Exception e) {
                Log.e("EXSTENSO_AutoSync", "JSON parse failed for tempId=" + tempId + ": " + e.getMessage());
                offlineDb.deleteByTempIdExstenso(tableType, tempId);
            }

            runOnUiThread(() -> syncDataItemAuto(tableType, dataList, index + 1, onFinish));
        }).start();
    }

    private boolean isAlreadySynced() {
        SharedPreferences prefs = getSharedPreferences("exstenso_app_prefs", MODE_PRIVATE);
        String lastSyncDate = prefs.getString("last_sync_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return today.equals(lastSyncDate);
    }

    private void markAsSynced() {
        SharedPreferences prefs = getSharedPreferences("exstenso_app_prefs", MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString("last_sync_date", today).apply();
    }

    // ==================== STRATEGI SINKRONISASI GROUP BY PENGUKURAN ====================

    private void syncAllOfflineData(Runnable onComplete) {
        boolean adaData = offlineDb.hasUnsyncedDataExstenso();
        if (!adaData) {
            prosesPerhitunganTertunda();
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerial("pengukuran", () -> {
            syncDataGroupedByPengukuran(onComplete);
        });
    }

    private void syncDataGroupedByPengukuran(Runnable onComplete) {
        new Thread(() -> {
            try {
                Map<Integer, List<Map<String, String>>> groupedData = offlineDb.getUnsyncedDataGroupedByPengukuran();

                if (groupedData.isEmpty()) {
                    runOnUiThread(() -> {
                        if (onComplete != null) onComplete.run();
                    });
                    return;
                }

                Log.d("EXSTENSO_Sync", "Syncing data for " + groupedData.size() + " pengukuran groups");

                List<Integer> pengukuranIds = new ArrayList<>(groupedData.keySet());
                syncPengukuranGroup(pengukuranIds, groupedData, 0, onComplete);

            } catch (Exception e) {
                Log.e("EXSTENSO_Sync", "Error in grouped sync: " + e.getMessage());
                runOnUiThread(() -> {
                    if (onComplete != null) onComplete.run();
                });
            }
        }).start();
    }

    private void syncPengukuranGroup(List<Integer> pengukuranIds,
                                     Map<Integer, List<Map<String, String>>> groupedData,
                                     int index, Runnable onComplete) {
        if (index >= pengukuranIds.size()) {
            runOnUiThread(() -> {
                if (onComplete != null) onComplete.run();
            });
            return;
        }

        int pengukuranId = pengukuranIds.get(index);
        List<Map<String, String>> dataList = groupedData.get(pengukuranId);

        Log.d("EXSTENSO_Sync", "Syncing group for pengukuran_id: " + pengukuranId + ", data count: " + dataList.size());

        syncDataItemsInGroup(dataList, pengukuranId, 0, () -> {
            syncPengukuranGroup(pengukuranIds, groupedData, index + 1, onComplete);
        });
    }

    private void syncDataItemsInGroup(List<Map<String, String>> dataList, int pengukuranId,
                                      int dataIndex, Runnable onGroupComplete) {
        if (dataIndex >= dataList.size()) {
            hitungDeformasiUntukPengukuran(pengukuranId, onGroupComplete);
            return;
        }

        Map<String, String> item = dataList.get(dataIndex);
        String tempId = item.get("temp_id");
        String jsonStr = item.get("json");
        String exType = item.get("ex_type");

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(jsonStr);

                HttpURLConnection conn = null;
                try {
                    URL url = new URL(INSERT_DATA_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");

                    OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        offlineDb.deleteByTempIdExstenso("data", tempId);
                        Log.d("EXSTENSO_Sync", "Synced data for pengukuran " + pengukuranId + ", exType: " + exType);

                        if (exType.startsWith("pembacaan_ex")) {
                            tandaiPerluHitungDeformasi(tempId, pengukuranId, exType);
                        }
                    }
                } catch (Exception e) {
                    Log.e("EXSTENSO_Sync", "Failed to sync tempId=" + tempId + ": " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            } catch (Exception e) {
                Log.e("EXSTENSO_Sync", "JSON parse failed for tempId=" + tempId + ": " + e.getMessage());
            }

            runOnUiThread(() -> syncDataItemsInGroup(dataList, pengukuranId, dataIndex + 1, onGroupComplete));
        }).start();
    }

    private void hitungDeformasiUntukPengukuran(int pengukuranId, Runnable onComplete) {
        new Thread(() -> {
            try {
                List<String> exTypes = offlineDb.getExTypesForPengukuran(pengukuranId);

                Log.d("EXSTENSO_Sync", "Calculating deformasi for pengukuran " + pengukuranId + ", types: " + exTypes);

                hitungDeformasiSequential(exTypes, pengukuranId, 0, onComplete);

            } catch (Exception e) {
                Log.e("EXSTENSO_Sync", "Error calculating deformasi for pengukuran " + pengukuranId + ": " + e.getMessage());
                runOnUiThread(onComplete);
            }
        }).start();
    }

    private void hitungDeformasiSequential(List<String> exTypes, int pengukuranId, int typeIndex, Runnable onComplete) {
        if (typeIndex >= exTypes.size()) {
            runOnUiThread(onComplete);
            return;
        }

        String exType = exTypes.get(typeIndex);
        if (!exType.startsWith("pembacaan_ex")) {
            hitungDeformasiSequential(exTypes, pengukuranId, typeIndex + 1, onComplete);
            return;
        }

        // Untuk sinkronisasi: hitung dan tambahkan ke antrian popup
        hitungDeformasiSingle(exType, pengukuranId, true, (success, data) -> {
            if (success) {
                Log.d("EXSTENSO_Sync", "Successfully calculated deformasi for " + exType + ", pengukuran " + pengukuranId);
            }
            hitungDeformasiSequential(exTypes, pengukuranId, typeIndex + 1, onComplete);
        });
    }

    // ==================== FITUR PERHITUNGAN DEFORMASI ====================

    private void tandaiPerluHitungDeformasi(String tempId, int pengukuranId, String exType) {
        SharedPreferences prefs = getSharedPreferences("exstenso_pending_calc", MODE_PRIVATE);
        String key = "pending_" + tempId;

        Map<String, String> pendingData = new HashMap<>();
        pendingData.put("pengukuran_id", String.valueOf(pengukuranId));
        pendingData.put("ex_type", exType);
        pendingData.put("temp_id", tempId);

        try {
            JSONObject json = new JSONObject(pendingData);
            prefs.edit().putString(key, json.toString()).apply();
            Log.d("PENDING_CALC", "Data ditandai perlu hitung: " + json.toString());
        } catch (Exception e) {
            Log.e("PENDING_CALC", "Gagal menyimpan pending calculation: " + e.getMessage());
        }
    }

    private void prosesPerhitunganTertunda() {
        if (!isInternetAvailable()) return;

        SharedPreferences prefs = getSharedPreferences("exstenso_pending_calc", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        if (allEntries.isEmpty()) return;

        Log.d("PENDING_CALC", "Processing " + allEntries.size() + " pending calculations");
        showToast("🔄 Memproses " + allEntries.size() + " perhitungan tertunda...");

        Map<Integer, List<String>> pendingByPengukuran = new HashMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("pending_")) {
                try {
                    String jsonStr = (String) entry.getValue();
                    JSONObject json = new JSONObject(jsonStr);

                    int pendingPengukuranId = json.getInt("pengukuran_id");
                    String pendingExType = json.getString("ex_type");
                    String pendingTempId = json.getString("temp_id");

                    if (!pendingByPengukuran.containsKey(pendingPengukuranId)) {
                        pendingByPengukuran.put(pendingPengukuranId, new ArrayList<String>());
                    }
                    pendingByPengukuran.get(pendingPengukuranId).add(pendingExType);

                } catch (Exception e) {
                    Log.e("PENDING_CALC", "Error processing pending calculation: " + e.getMessage());
                }
            }
        }

        prosesPendingByPengukuran(new ArrayList<>(pendingByPengukuran.keySet()), pendingByPengukuran, 0);
    }

    private void prosesPendingByPengukuran(List<Integer> pengukuranIds,
                                           Map<Integer, List<String>> pendingByPengukuran,
                                           int index) {
        if (index >= pengukuranIds.size()) {
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

        int pengukuranId = pengukuranIds.get(index);
        List<String> exTypes = pendingByPengukuran.get(pengukuranId);

        Log.d("PENDING_CALC", "Processing pending calculations for pengukuran " + pengukuranId + ": " + exTypes);

        hitungDeformasiSequential(exTypes, pengukuranId, 0, () -> {
            hapusPendingUntukPengukuran(pengukuranId);
            prosesPendingByPengukuran(pengukuranIds, pendingByPengukuran, index + 1);
        });
    }

    private void hapusPendingUntukPengukuran(int pengukuranId) {
        SharedPreferences prefs = getSharedPreferences("exstenso_pending_calc", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("pending_")) {
                try {
                    String jsonStr = (String) entry.getValue();
                    JSONObject json = new JSONObject(jsonStr);

                    int pendingPengukuranId = json.getInt("pengukuran_id");
                    if (pendingPengukuranId == pengukuranId) {
                        prefs.edit().remove(entry.getKey()).apply();
                    }
                } catch (Exception e) {
                    Log.e("PENDING_CALC", "Error removing pending calculation: " + e.getMessage());
                }
            }
        }
    }

    private String getHitungEndpoint(String exType) {
        switch (exType) {
            case "pembacaan_ex1": return "hitung-deformasi-ex1";
            case "pembacaan_ex2": return "hitung-deformasi-ex2";
            case "pembacaan_ex3": return "hitung-deformasi-ex3";
            case "pembacaan_ex4": return "hitung-deformasi-ex4";
            default: return "hitung-deformasi-ex1";
        }
    }

    // ==================== HANDLER INPUT DATA ====================

    private void handleSimpanPembacaan() {
        if (pengukuranId == -1) {
            showToast("❌ Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        if (!validatePembacaanFields()) {
            showToast("❌ Harap isi minimal satu field pembacaan");
            return;
        }

        if (isInternetAvailable()) {
            simpanDanHitungPembacaan();
        } else {
            simpanPembacaanOffline();
        }
    }

    private void simpanDanHitungPembacaan() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", selectedExType);
        data.put("pengukuran_id", String.valueOf(pengukuranId));

        if (!inputPembacaan10.getText().toString().trim().isEmpty()) {
            data.put("pembacaan_10", inputPembacaan10.getText().toString().trim());
        }
        if (!inputPembacaan20.getText().toString().trim().isEmpty()) {
            data.put("pembacaan_20", inputPembacaan20.getText().toString().trim());
        }
        if (!inputPembacaan30.getText().toString().trim().isEmpty()) {
            data.put("pembacaan_30", inputPembacaan30.getText().toString().trim());
        }

        Log.d("EXSTENSO_API", "Menyimpan & menghitung data " + selectedExType + ": " + data.toString());
        sendToServerWithHitung(data, "Pembacaan");
    }

    private void simpanPembacaanOffline() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", selectedExType);
        data.put("pengukuran_id", String.valueOf(pengukuranId));

        if (!inputPembacaan10.getText().toString().trim().isEmpty()) {
            data.put("pembacaan_10", inputPembacaan10.getText().toString().trim());
        }
        if (!inputPembacaan20.getText().toString().trim().isEmpty()) {
            data.put("pembacaan_20", inputPembacaan20.getText().toString().trim());
        }
        if (!inputPembacaan30.getText().toString().trim().isEmpty()) {
            data.put("pembacaan_30", inputPembacaan30.getText().toString().trim());
        }

        String localTempId = "local_" + System.currentTimeMillis() + "_" + selectedExType;
        data.put("temp_id", localTempId);

        boolean success = saveOffline("data", localTempId, data);
        if (success) {
            showToast("📱 Data " + selectedExType + " disimpan offline\n⚠️ Hitung deformasi akan dilakukan saat online");
            clearPembacaanSection();
            tandaiPerluHitungDeformasi(localTempId, pengukuranId, selectedExType);
        } else {
            showToast("❌ Gagal menyimpan data offline");
        }
    }

    private boolean saveOffline(String tableType, String tempId, Map<String, String> data) {
        try {
            JSONObject json = new JSONObject(data);

            if (tableType.equals("data") && data.containsKey("mode")) {
                String exType = data.get("mode");
                boolean success = offlineDb.insertDataExstensoWithType(tableType, tempId, json.toString(),
                        pengukuranId, exType);
                return success;
            } else {
                boolean success = offlineDb.insertDataExstenso(tableType, tempId, json.toString());
                return success;
            }

        } catch (Exception e) {
            Log.e("EXSTENSO_Offline", "Gagal simpan offline: " + e.getMessage());
            showToast("❌ Gagal simpan offline: " + e.getMessage());
            return false;
        }
    }

    // ==================== METHOD UI DAN UTILITAS ====================

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
        inputDMA = findViewById(R.id.inputDMA);
        inputPembacaan10 = findViewById(R.id.inputPembacaan10);
        inputPembacaan20 = findViewById(R.id.inputPembacaan20);
        inputPembacaan30 = findViewById(R.id.inputPembacaan30);

        btnSimpanDMA = findViewById(R.id.btnSimpanDMA);
        btnSimpanPembacaan = findViewById(R.id.btnSimpanPembacaan);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        spinnerExType = findViewById(R.id.spinnerExType);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
        titleExstenso = findViewById(R.id.titleExstenso);

        btnSimpanDMA.setOnClickListener(v -> handleSimpanDMA());
        btnSimpanPembacaan.setOnClickListener(v -> handleSimpanPembacaan());
    }

    private void initSpinnerComponents() {
        pengukuranAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        pengukuranAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(pengukuranAdapter);

        String[] exTypeOptions = {
                "Pembacaan Ex1", "Pembacaan Ex2", "Pembacaan Ex3", "Pembacaan Ex4"
        };
        exTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exTypeOptions);
        exTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExType.setAdapter(exTypeAdapter);

        btnPilihPengukuran.setOnClickListener(v -> {
            Object selected = spinnerPengukuran.getSelectedItem();
            if (selected != null && pengukuranMap.containsKey(selected.toString())) {
                pengukuranId = pengukuranMap.get(selected.toString());
                showToast("✅ Pengukuran dipilih: " + selected);
                loadExistingData();
                getPengukuranDataForDMA();
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
                    loadExistingData();
                    getPengukuranDataForDMA();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerExType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: selectedExType = "pembacaan_ex1"; break;
                    case 1: selectedExType = "pembacaan_ex2"; break;
                    case 2: selectedExType = "pembacaan_ex3"; break;
                    case 3: selectedExType = "pembacaan_ex4"; break;
                    default: selectedExType = "pembacaan_ex1";
                }
                updateTitle();
                loadExistingData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTitle() {
        if (titleExstenso != null) {
            titleExstenso.setText("Extensometer - " + selectedExType.toUpperCase());
        }
    }

    private void clearDMASection() {
        runOnUiThread(() -> {
            inputDMA.setText("");
        });
    }

    private void clearPembacaanSection() {
        runOnUiThread(() -> {
            inputPembacaan10.setText("");
            inputPembacaan20.setText("");
            inputPembacaan30.setText("");
        });
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
    }

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
                Log.d("EXSTENSO_API", "Response get-pengukuran: " + responseBody);

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
                                getPengukuranDataForDMA();
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
            List<Map<String,String>> rows = offlineDb.getPengukuranMasterExstenso();
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
            Log.e("EXSTENSO_Offline", "Error load offline master: " + e.getMessage());
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
                String url = GET_DATA_URL + "?pengukuran_id=" + pengukuranId + "&type=" + selectedExType;
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
        Map<String, String> data = offlineDb.getExstensoData(pengukuranId, selectedExType);
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
            if (data.has("dma")) inputDMA.setText(data.getString("dma"));
            if (data.has("pembacaan_10")) inputPembacaan10.setText(data.getString("pembacaan_10"));
            if (data.has("pembacaan_20")) inputPembacaan20.setText(data.getString("pembacaan_20"));
            if (data.has("pembacaan_30")) inputPembacaan30.setText(data.getString("pembacaan_30"));
        } catch (Exception e) {
            Log.e("POPULATE_FORM", "Error populating form: " + e.getMessage());
        }
    }

    private void getPengukuranDataForDMA() {
        new Thread(() -> {
            try {
                String url = GET_PENGUKURAN_URL;
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
                        JSONArray dataArray = response.getJSONArray("data");

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);
                            int id = item.getInt("id_pengukuran");

                            if (id == pengukuranId) {
                                currentTahun = item.getString("tahun");
                                currentTanggal = item.getString("tanggal");
                                currentPeriode = item.optString("periode", "TW-1");

                                Log.d("EXSTENSO_API", "Data pengukuran untuk DMA: tahun=" + currentTahun +
                                        ", tanggal=" + currentTanggal + ", periode=" + currentPeriode);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("GET_PENGUKURAN_DMA", "Error: " + e.getMessage());
            }
        }).start();
    }

    private void handleSimpanDMA() {
        if (pengukuranId == -1) {
            showToast("❌ Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        if (!validateDMAFields()) {
            showToast("❌ Harap isi field DMA");
            return;
        }

        if (currentTahun.isEmpty() || currentTanggal.isEmpty()) {
            showToast("❌ Data pengukuran belum lengkap, harap tunggu...");
            getPengukuranDataForDMA();
            return;
        }

        updateDMAPengukuran();
    }

    private boolean validateDMAFields() {
        return !inputDMA.getText().toString().trim().isEmpty();
    }

    private boolean validatePembacaanFields() {
        return !inputPembacaan10.getText().toString().trim().isEmpty() ||
                !inputPembacaan20.getText().toString().trim().isEmpty() ||
                !inputPembacaan30.getText().toString().trim().isEmpty();
    }

    private void updateDMAPengukuran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "update_dma");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("dma", inputDMA.getText().toString().trim());

        Log.d("EXSTENSO_API", "Mengupdate DMA pengukuran: " + data.toString());

        if (isInternetAvailable()) {
            sendToServer(data, "DMA");
        } else {
            String localTempId = "local_" + System.currentTimeMillis() + "_dma";
            data.put("temp_id", localTempId);
            saveOffline("data", localTempId, data);
            showToast("📱 Data DMA disimpan offline");
        }
    }

    private void sendToServer(Map<String, String> dataMap, String dataType) {
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
                Log.d("EXSTENSO_API", "JSON yang dikirim (" + dataType + "): " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("EXSTENSO_API", "Response Code (" + dataType + "): " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("EXSTENSO_API", "Response Body (" + dataType + "): " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            showToast("✅ " + message);
                            if (dataType.equals("DMA")) {
                                clearDMASection();
                            } else {
                                clearPembacaanSection();
                            }
                            break;
                        case "info":
                            showToast("ℹ️ " + message);
                            break;
                        case "error":
                        default:
                            showToast("❌ " + message);
                            if (!dataMap.containsKey("temp_id")) {
                                String localTempId = "local_" + System.currentTimeMillis();
                                dataMap.put("temp_id", localTempId);
                                saveOffline("data", localTempId, dataMap);
                            }
                            break;
                    }
                });

            } catch (Exception e) {
                Log.e("SEND_TO_SERVER", "Error (" + dataType + "): " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal kirim " + dataType + ": " + e.getMessage() + ". Data disimpan offline.");
                    if (!dataMap.containsKey("temp_id")) {
                        String localTempId = "local_" + System.currentTimeMillis();
                        dataMap.put("temp_id", localTempId);
                        saveOffline("data", localTempId, dataMap);
                    }
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
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

        Log.d("EXSTENSO_API", "Mengirim data pengukuran: " + data.toString());

        if (isInternetAvailable()) {
            sendPengukuranToServer(data);
        } else {
            String localTempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", localTempId);
            saveOffline("pengukuran", localTempId, data);
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

    private void sendPengukuranToServer(Map<String, String> dataMap) {
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
                Log.d("EXSTENSO_API", "JSON pengukuran: " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("EXSTENSO_API", "Response Code: " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("EXSTENSO_API", "Response Body: " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                if (response.has("pengukuran_id")) {
                    pengukuranId = response.optInt("pengukuran_id", -1);
                    Log.d("EXSTENSO_API", "Pengukuran ID diterima: " + pengukuranId);
                }

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            showToast("✅ " + message);
                            hideModal();
                            loadPengukuranData();
                            break;
                        case "info":
                            showToast("ℹ️ " + message);
                            if (response.has("pengukuran_id")) {
                                pengukuranId = response.optInt("pengukuran_id", -1);
                                hideModal();
                                loadPengukuranData();
                            }
                            break;
                        case "error":
                        default:
                            showToast("❌ " + message);
                            if (!dataMap.containsKey("temp_id")) {
                                String localTempId = "local_" + System.currentTimeMillis();
                                dataMap.put("temp_id", localTempId);
                                saveOffline("pengukuran", localTempId, dataMap);
                            }
                            break;
                    }
                });

            } catch (Exception e) {
                Log.e("SEND_PENGUKURAN", "Error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal kirim: " + e.getMessage() + ". Data disimpan offline.");
                    if (!dataMap.containsKey("temp_id")) {
                        String localTempId = "local_" + System.currentTimeMillis();
                        dataMap.put("temp_id", localTempId);
                        saveOffline("pengukuran", localTempId, dataMap);
                    }
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void syncDataSerial(String tableType, Runnable next) {
        List<Map<String, String>> dataList = offlineDb.getUnsyncedDataExstenso(tableType);
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
                        offlineDb.deleteByTempIdExstenso(tableType, tempId);
                        Log.d("EXSTENSO_Sync", "Data " + tableType + " tempId=" + tempId + " berhasil disinkronisasi");
                    }
                } catch (Exception e) {
                    Log.e("EXSTENSO_Sync", "Error sync " + tableType + " tempId=" + tempId, e);
                } finally {
                    if (conn != null) conn.disconnect();
                }

                runOnUiThread(() -> syncDataItem(tableType, dataList, index + 1, onFinish));
            }).start();

        } catch (Exception e) {
            Log.e("EXSTENSO_Sync", "JSON parse error untuk data " + tableType + " tempId=" + tempId, e);
            runOnUiThread(() -> syncDataItem(tableType, dataList, index + 1, onFinish));
        }
    }

    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d("EXSTENSO_TOAST", "Pesan: " + message);
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