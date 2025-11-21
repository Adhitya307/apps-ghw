package com.example.app_dambody;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.VolleyError;
import java.util.Iterator;


import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class InputdataElv600 extends AppCompatActivity {

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

    // Form utama
    private EditText inputDMA, inputHV1, inputHV2, inputHV3, inputHV4, inputHV5;
    private Button btnSubmitDMA, btnSubmitHVAll, btnHitung;
    private Spinner spinnerPengukuran;
    private Button btnPilihPengukuran;

    private Calendar calendar;
    private int pengukuranId = -1;
    private String tempId = null;

    // API URL
    private static final String BASE_URL = "https://sgl-geoteknik.darfataraproteksi.my.id/api/";
    private static final String INSERT_DATA_URL = BASE_URL + "dombody/input";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "dombody/get-pengukuran";

    // Data pengukuran
    private final Map<String, Integer> pengukuranMap = new HashMap<>();
    private final List<String> tanggalList = new ArrayList<>();
    private ArrayAdapter<String> pengukuranAdapter;

    // ✅ AUTO SYNC VARIABLES
    private OfflineDataHelper offlineDb;
    private SharedPreferences syncPrefs;
    private boolean isSyncInProgress = false;
    private Handler networkCheckHandler = new Handler();
    private Runnable networkCheckRunnable;
    private boolean lastOnlineStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inputdataelv600);

        // ✅ Inisialisasi database offline + prefs
        offlineDb = new OfflineDataHelper(this);
        syncPrefs = getSharedPreferences("elv600_sync_prefs", MODE_PRIVATE);
        calendar = Calendar.getInstance();

        initModalComponents();
        initFormComponents();
        initSpinnerComponents();
        setupModalDropdowns();
        setupModalCalendar();

        // Siapkan adapter spinner
        pengukuranAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        pengukuranAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(pengukuranAdapter);

        // ✅ AUTO SYNC: Cek status internet dan mulai monitoring
        checkInternetAndShowToast();
        startNetworkMonitoring();

        // Load data pengukuran (online/offline)
        loadPengukuranData();

        // Tampilkan modal di awal
        showModal();

        // Set click listeners untuk tombol form
        btnSubmitDMA.setOnClickListener(v -> handleDMA());
        btnSubmitHVAll.setOnClickListener(v -> handleHVAll());
        btnHitung.setOnClickListener(v -> handleHitung());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInternetAndShowToast();

        if (isInternetAvailable()) {
            if (offlineDb.hasUnsyncedDataELV600()) {
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

    // ✅ AUTO SYNC METHODS
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

        int offlineCount = offlineDb.getOfflineDataCountELV600();
        if (offlineCount > 0) {
            Log.d("ELV600_AutoSync", "Found " + offlineCount + " offline data, starting auto-sync");
            triggerAutoSync();
        }
    }

    private void triggerAutoSync() {
        if (isSyncInProgress) return;

        Log.d("ELV600_AutoSync", "Triggering auto-sync for offline data");
        isSyncInProgress = true;
        showToast("🔄 Auto-sync data offline...");

        syncAllOfflineDataAuto(() -> {
            isSyncInProgress = false;
            Log.d("ELV600_AutoSync", "Auto-sync completed");
            runOnUiThread(this::loadPengukuranData);
        });
    }

    private void syncAllOfflineDataAuto(Runnable onComplete) {
        int offlineCount = offlineDb.getOfflineDataCountELV600();
        if (offlineCount == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerialAuto("pengukuran", () ->
                syncDataSerialAuto("dma", () ->
                        syncDataSerialAuto("data", () -> {
                            showToast("✅ " + offlineCount + " data terkirim");
                            if (onComplete != null) onComplete.run();
                        })
                )
        );
    }

    private void syncDataSerialAuto(String tableType, Runnable next) {
        List<Map<String,String>> list = offlineDb.getUnsyncedDataELV600(tableType);
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
            offlineDb.deleteByTempIdELV600(tableType, tempId);
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
                        offlineDb.deleteByTempIdELV600(tableType, tempId);
                        Log.d("ELV600_AutoSync", "Synced " + tableType + " tempId=" + tempId);
                    }
                } catch (Exception e) {
                    Log.e("ELV600_AutoSync", "Failed to sync tempId=" + tempId + ": " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            } catch (Exception e) {
                Log.e("ELV600_AutoSync", "JSON parse failed for tempId=" + tempId + ": " + e.getMessage());
                offlineDb.deleteByTempIdELV600(tableType, tempId);
            }

            runOnUiThread(() -> syncDataItemAuto(tableType, dataList, index + 1, onFinish));
        }).start();
    }

    private boolean isAlreadySynced() {
        SharedPreferences prefs = getSharedPreferences("elv600_app_prefs", MODE_PRIVATE);
        String lastSyncDate = prefs.getString("last_sync_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return today.equals(lastSyncDate);
    }

    private void markAsSynced() {
        SharedPreferences prefs = getSharedPreferences("elv600_app_prefs", MODE_PRIVATE);
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
        inputDMA = findViewById(R.id.inputDMA);
        inputHV1 = findViewById(R.id.inputHV1);
        inputHV2 = findViewById(R.id.inputHV2);
        inputHV3 = findViewById(R.id.inputHV3);
        inputHV4 = findViewById(R.id.inputHV4);
        inputHV5 = findViewById(R.id.inputHV5);

        btnSubmitDMA = findViewById(R.id.btnSubmitDMA);
        btnSubmitHVAll = findViewById(R.id.btnSubmitHVAll);
        btnHitung = findViewById(R.id.btnHitung);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
    }

    private void initSpinnerComponents() {
        pengukuranAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        pengukuranAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(pengukuranAdapter);

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

    // ✅ NEW: Handle semua HV sekaligus
    private void handleHVAll() {
        if (pengukuranId == -1) {
            showToast("Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        // Validasi semua field HV harus terisi
        if (!validateHVFields()) {
            showToast("Harap isi semua field HV (1-5)");
            return;
        }

        // Kirim data HV
        handleHV("HV-1", inputHV1, "hv_1");
        handleHV("HV-2", inputHV2, "hv_2");
        handleHV("HV-3", inputHV3, "hv_3");
        handleHV("HV-4", inputHV4, "hv_4");
        handleHV("HV-5", inputHV5, "hv_5");

        showToast("✅ Semua data HV berhasil disimpan");
        clearHVFields();
    }

    // ✅ NEW: Validasi field HV saja
    private boolean validateHVFields() {
        return !inputHV1.getText().toString().trim().isEmpty() &&
                !inputHV2.getText().toString().trim().isEmpty() &&
                !inputHV3.getText().toString().trim().isEmpty() &&
                !inputHV4.getText().toString().trim().isEmpty() &&
                !inputHV5.getText().toString().trim().isEmpty();
    }

    // ✅ NEW: Clear field HV saja
    private void clearHVFields() {
        inputHV1.setText("");
        inputHV2.setText("");
        inputHV3.setText("");
        inputHV4.setText("");
        inputHV5.setText("");
    }

    // ✅ MODIFIED: handleHitung khusus untuk ELV600 dengan popup notifikasi lengkap
    private void handleHitung() {
        try {
            if (pengukuranId == -1) {
                showToast("❌ Harap pilih data pengukuran terlebih dahulu");
                return;
            }

            final String selectedTanggal = spinnerPengukuran.getSelectedItem() != null ?
                    spinnerPengukuran.getSelectedItem().toString() :
                    "Tanggal tidak diketahui";

            String url = BASE_URL + "dombody/hitung/elv600";

            JSONObject postData = new JSONObject();
            postData.put("pengukuran_id", pengukuranId);

            Log.d("ELV600_API", "Mengirim permintaan perhitungan untuk pengukuran_id: " + pengukuranId);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    postData,
                    response -> {
                        try {
                            String status = response.optString("status", "error");
                            String message = response.optString("message", "Tidak ada pesan dari server");

                            Log.d("ELV600_API", "Response dari server: " + response.toString());

                            if (status.equalsIgnoreCase("success")) {
                                JSONObject data = response.optJSONObject("data");

                                if (data != null) {
                                    double hv1 = data.optDouble("hv_1", 0.0);
                                    double hv2 = data.optDouble("hv_2", 0.0);
                                    double hv3 = data.optDouble("hv_3", 0.0);
                                    double hv4 = data.optDouble("hv_4", 0.0);
                                    double hv5 = data.optDouble("hv_5", 0.0);

                                    // ✅ TAMPILKAN DIALOG HASIL PERHITUNGAN LENGKAP
                                    showCalculationResultDialogELV600(selectedTanggal, hv1, hv2, hv3, hv4, hv5);
                                } else {
                                    showToast("✅ " + message);
                                }
                            } else {
                                showToast("⚠️ " + message);
                            }

                        } catch (Exception e) {
                            Log.e("ELV600_API", "Error parsing response: " + e.getMessage());
                            showToast("❌ Error parsing response dari server");
                        }
                    },
                    error -> {
                        Log.e("ELV600_API", "Network error: " + error.getMessage());
                        showToast("❌ Gagal terhubung ke server");
                    }
            );

            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            queue.add(request);

        } catch (Exception e) {
            showToast("❌ Error: " + e.getMessage());
        }
    }

    // ✅ MODIFIED: Method untuk menampilkan dialog hasil perhitungan ELV600 dengan layout custom
    private void showCalculationResultDialogELV600(String tanggal, double hv1, double hv2, double hv3, double hv4, double hv5) {
        runOnUiThread(() -> {
            try {
                // Inflate custom layout
                LayoutInflater inflater = LayoutInflater.from(this);
                View dialogView = inflater.inflate(R.layout.dialog_elv600_result, null);

                // Initialize views
                TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
                TextView tvTanggal = dialogView.findViewById(R.id.tvTanggal);
                TextView tvHvData = dialogView.findViewById(R.id.tvHvData);

                // Sections untuk H1-H5
                TextView tvAmbangBatasH1 = dialogView.findViewById(R.id.tvAmbangBatasH1);
                TextView tvPergerakanH1 = dialogView.findViewById(R.id.tvPergerakanH1);
                TextView tvStatusH1 = dialogView.findViewById(R.id.tvStatusH1);

                TextView tvAmbangBatasH2 = dialogView.findViewById(R.id.tvAmbangBatasH2);
                TextView tvPergerakanH2 = dialogView.findViewById(R.id.tvPergerakanH2);
                TextView tvStatusH2 = dialogView.findViewById(R.id.tvStatusH2);

                TextView tvAmbangBatasH3 = dialogView.findViewById(R.id.tvAmbangBatasH3);
                TextView tvPergerakanH3 = dialogView.findViewById(R.id.tvPergerakanH3);
                TextView tvStatusH3 = dialogView.findViewById(R.id.tvStatusH3);

                TextView tvAmbangBatasH4 = dialogView.findViewById(R.id.tvAmbangBatasH4);
                TextView tvPergerakanH4 = dialogView.findViewById(R.id.tvPergerakanH4);
                TextView tvStatusH4 = dialogView.findViewById(R.id.tvStatusH4);

                TextView tvAmbangBatasH5 = dialogView.findViewById(R.id.tvAmbangBatasH5);
                TextView tvPergerakanH5 = dialogView.findViewById(R.id.tvPergerakanH5);
                TextView tvStatusH5 = dialogView.findViewById(R.id.tvStatusH5);


                Button btnClose = dialogView.findViewById(R.id.btnClose);

                // Set data
                tvTitle.setText("📊 Hasil Perhitungan ELV600");
                tvTanggal.setText("Tanggal: " + tanggal);

                // Format HV data
                String hvData = "Data HV:\n" +
                        "• HV1: " + String.format("%.4f", hv1) + "\n" +
                        "• HV2: " + String.format("%.4f", hv2) + "\n" +
                        "• HV3: " + String.format("%.4f", hv3) + "\n" +
                        "• HV4: " + String.format("%.4f", hv4) + "\n" +
                        "• HV5: " + String.format("%.4f", hv5);
                tvHvData.setText(hvData);

                // Hitung pergerakan untuk masing-masing H
                double pergerakanH1 = hv1 * 10;
                double pergerakanH2 = hv2 * 10;
                double pergerakanH3 = hv3 * 10;
                double pergerakanH4 = hv4 * 10;
                double pergerakanH5 = hv5 * 10;

                // Ambang batas ELV600 yang benar untuk setiap HV
                // HV1
                double amanH1 = -44.29, peringatanH1 = -60.40, bahayaH1 = -51.11;
                // HV2
                double amanH2 = -39.75, peringatanH2 = -54.20, bahayaH2 = -45.86;
                // HV3
                double amanH3 = -40.63, peringatanH3 = -46.88, bahayaH3 = -55.40;
                // HV4
                double amanH4 = -24.86, peringatanH4 = -33.90, bahayaH4 = -28.68;
                // HV5
                double amanH5 = -11.22, peringatanH5 = -15.30, bahayaH5 = -12.95;

                // Data untuk H1
                String ambangBatasH1 = "AMBANG BATAS HV1:\n" +
                        "Aman: " + amanH1 + "\n" +
                        "Peringatan: " + peringatanH1 + "\n" +
                        "Bahaya: " + bahayaH1;
                tvAmbangBatasH1.setText(ambangBatasH1);
                tvPergerakanH1.setText("Pergerakan: " + String.format("%.4f", pergerakanH1));
                String statusH1 = analyzeStatusELV600(pergerakanH1, amanH1, peringatanH1, bahayaH1);
                tvStatusH1.setText("KONDISI: " + statusH1);

                // Data untuk H2
                String ambangBatasH2 = "AMBANG BATAS HV2:\n" +
                        "Aman: " + amanH2 + "\n" +
                        "Peringatan: " + peringatanH2 + "\n" +
                        "Bahaya: " + bahayaH2;
                tvAmbangBatasH2.setText(ambangBatasH2);
                tvPergerakanH2.setText("Pergerakan: " + String.format("%.4f", pergerakanH2));
                String statusH2 = analyzeStatusELV600(pergerakanH2, amanH2, peringatanH2, bahayaH2);
                tvStatusH2.setText("KONDISI: " + statusH2);

                // Data untuk H3
                String ambangBatasH3 = "AMBANG BATAS HV3:\n" +
                        "Aman: " + amanH3 + "\n" +
                        "Peringatan: " + peringatanH3 + "\n" +
                        "Bahaya: " + bahayaH3;
                tvAmbangBatasH3.setText(ambangBatasH3);
                tvPergerakanH3.setText("Pergerakan: " + String.format("%.4f", pergerakanH3));
                String statusH3 = analyzeStatusELV600(pergerakanH3, amanH3, peringatanH3, bahayaH3);
                tvStatusH3.setText("KONDISI: " + statusH3);

                // Data untuk H4
                String ambangBatasH4 = "AMBANG BATAS HV4:\n" +
                        "Aman: " + amanH4 + "\n" +
                        "Peringatan: " + peringatanH4 + "\n" +
                        "Bahaya: " + bahayaH4;
                tvAmbangBatasH4.setText(ambangBatasH4);
                tvPergerakanH4.setText("Pergerakan: " + String.format("%.4f", pergerakanH4));
                String statusH4 = analyzeStatusELV600(pergerakanH4, amanH4, peringatanH4, bahayaH4);
                tvStatusH4.setText("KONDISI: " + statusH4);

                // Data untuk H5
                String ambangBatasH5 = "AMBANG BATAS HV5:\n" +
                        "Aman: " + amanH5 + "\n" +
                        "Peringatan: " + peringatanH5 + "\n" +
                        "Bahaya: " + bahayaH5;
                tvAmbangBatasH5.setText(ambangBatasH5);
                tvPergerakanH5.setText("Pergerakan: " + String.format("%.4f", pergerakanH5));
                String statusH5 = analyzeStatusELV600(pergerakanH5, amanH5, peringatanH5, bahayaH5);
                tvStatusH5.setText("KONDISI: " + statusH5);

                // Rekomendasi berdasarkan status terburuk
                String overallStatus = getOverallStatusELV600(statusH1, statusH2, statusH3, statusH4, statusH5);


                // Setup dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(dialogView);
                builder.setCancelable(false);

                AlertDialog dialog = builder.create();
                dialog.show();

                // Close button listener
                btnClose.setOnClickListener(v -> dialog.dismiss());

            } catch (Exception e) {
                Log.e("ELV600_DIALOG", "Error showing calculation dialog: " + e.getMessage());
                showToast("✅ Perhitungan selesai untuk tanggal " + tanggal);
            }
        });
    }

    private String analyzeStatusELV600(double pergerakan, double aman, double peringatan, double bahaya) {
        // PERBAIKAN: Urutan yang benar untuk nilai negatif
        if (pergerakan >= peringatan) {
            return "✅ AMAN";
        } else if (pergerakan >= bahaya) {
            return "⚠️ PERINGATAN";
        } else {
            return "🚨 BAHAYA";
        }
    }

    // ✅ Helper method untuk mendapatkan status keseluruhan ELV600
    private String getOverallStatusELV600(String statusH1, String statusH2, String statusH3, String statusH4, String statusH5) {
        if (statusH1.contains("🚨") || statusH2.contains("🚨") || statusH3.contains("🚨") || statusH4.contains("🚨") || statusH5.contains("🚨")) {
            return "BAHAYA";
        } else if (statusH1.contains("⚠️") || statusH2.contains("⚠️") || statusH3.contains("⚠️") || statusH4.contains("⚠️") || statusH5.contains("⚠️")) {
            return "PERINGATAN";
        } else {
            return "AMAN";
        }
    }



    // ✅ Validasi semua field
    private boolean validateAllFields() {
        return !inputDMA.getText().toString().trim().isEmpty() &&
                !inputHV1.getText().toString().trim().isEmpty() &&
                !inputHV2.getText().toString().trim().isEmpty() &&
                !inputHV3.getText().toString().trim().isEmpty() &&
                !inputHV4.getText().toString().trim().isEmpty() &&
                !inputHV5.getText().toString().trim().isEmpty();
    }

    // ✅ MODIFIED: Handle DMA (dengan clear field)
    private void handleDMA() {
        String nilaiDMA = inputDMA.getText().toString().trim();
        if (nilaiDMA.isEmpty()) {
            showToast("Harap isi nilai DMA");
            return;
        }

        if (pengukuranId == -1) {
            showToast("Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("mode", "pengukuran");
        data.put("dma", nilaiDMA);
        data.put("pengukuran_id", String.valueOf(pengukuranId));

        Log.d("ELV600_API", "Mengirim data DMA: " + data.toString());

        if (isInternetAvailable()) {
            sendToServer(data, "dma", false);
        } else {
            String localTempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", localTempId);
            saveOffline("dma", localTempId, data);
        }
        inputDMA.setText("");
    }

    // ✅ MODIFIED: Handle HV (tanpa clear field)
    private void handleHV(String fieldName, EditText input, String fieldKey) {
        String nilai = input.getText().toString().trim();
        if (nilai.isEmpty()) {
            return; // Skip jika kosong, sudah divalidasi di validateHVFields
        }

        Map<String, String> data = new HashMap<>();
        data.put("mode", "elv600");
        data.put(fieldKey, nilai);
        data.put("pengukuran_id", String.valueOf(pengukuranId));

        Log.d("ELV600_API", "Mengirim data " + fieldName + ": " + data.toString());

        if (isInternetAvailable()) {
            sendToServer(data, "data", false);
        } else {
            String localTempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", localTempId);
            saveOffline("data", localTempId, data);
        }
    }

    // ✅ MODIFIED: Load data pengukuran dengan offline support
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
                Log.d("ELV600_API", "Response get-pengukuran: " + responseBody);

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
                            int id = item.optInt("id", -1);

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
            }
 finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ✅ NEW: Load data dari lokal ketika offline
    private void loadTanggalOffline() {
        try {
            List<Map<String,String>> rows = offlineDb.getPengukuranMasterELV600();
            List<String> list = new ArrayList<>();
            pengukuranMap.clear();

            if (rows != null && !rows.isEmpty()) {
                for (Map<String,String> r : rows) {
                    String tanggal = r.get("tanggal");
                    String idStr = r.get("id");
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
            Log.e("ELV600_Offline", "Error load offline master: " + e.getMessage());
        }
    }

    // ✅ MODIFIED: Handle modal dengan offline support
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

        Log.d("ELV600_API", "Mengirim data pengukuran: " + data.toString());

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

    // ✅ NEW: Save offline method
    private void saveOffline(String tableType, String tempId, Map<String, String> data) {
        try {
            JSONObject json = new JSONObject(data);
            boolean success = offlineDb.insertDataELV600(tableType, tempId, json.toString());
            if (success) {
                showToast("📱 Data disimpan offline (" + tableType + ")");
            } else {
                showToast("❌ Gagal simpan offline");
            }
        } catch (Exception e) {
            Log.e("ELV600_Offline", "Gagal simpan offline: " + e.getMessage());
            showToast("❌ Gagal simpan offline: " + e.getMessage());
        }
    }

    // ✅ MODIFIED: Send to server dengan offline fallback
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
                Log.d("ELV600_API", "JSON yang dikirim: " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("ELV600_API", "Response Code: " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("ELV600_API", "Response Body: " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                if (isPengukuran && response.has("pengukuran_id")) {
                    pengukuranId = response.optInt("pengukuran_id", -1);
                    tempId = null;
                    Log.d("ELV600_API", "Pengukuran ID diterima: " + pengukuranId);
                    runOnUiThread(this::loadPengukuranData);
                }

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            showToast("✅ " + message);
                            if (isPengukuran) hideModal();
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

    // ✅ NEW: Sync all offline data method
    private void syncAllOfflineData(Runnable onComplete) {
        boolean adaData = offlineDb.hasUnsyncedDataELV600();
        if (!adaData) {
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerial("pengukuran", () ->
                syncDataSerial("dma", () ->
                        syncDataSerial("data", onComplete)
                )
        );
    }

    private void syncDataSerial(String tableType, Runnable next) {
        List<Map<String, String>> dataList = offlineDb.getUnsyncedDataELV600(tableType);
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
                        offlineDb.deleteByTempIdELV600(tableType, tempId);
                        Log.d("ELV600_Sync", "Data " + tableType + " tempId=" + tempId + " berhasil disinkronisasi");
                    }
                } catch (Exception e) {
                    Log.e("ELV600_Sync", "Error sync " + tableType + " tempId=" + tempId, e);
                } finally {
                    if (conn != null) conn.disconnect();
                }

                runOnUiThread(() -> syncDataItem(tableType, dataList, index + 1, onFinish));
            }).start();

        } catch (Exception e) {
            Log.e("ELV600_Sync", "JSON parse error untuk data " + tableType + " tempId=" + tempId, e);
            runOnUiThread(() -> syncDataItem(tableType, dataList, index + 1, onFinish));
        }
    }

    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d("ELV600_TOAST", "Pesan: " + message);
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