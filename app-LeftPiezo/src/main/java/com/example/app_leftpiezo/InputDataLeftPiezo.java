package com.example.app_leftpiezo;

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

public class InputDataLeftPiezo extends AppCompatActivity {

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

    // Form utama Left Piezo
    private TextInputEditText inputDMA, inputFeet, inputInch;
    private Button btnSimpanDMA, btnSimpanPembacaan;
    private Spinner spinnerPengukuran, spinnerLokasi;
    private Button btnPilihPengukuran;
    private TextView titlePiezometer;

    private Calendar calendar;
    private int pengukuranId = -1;
    private String selectedLokasi = "L01";

    // API URLs untuk Left Piezo
    private static final String BASE_URL = "http://192.168.1.12/GHW/api-apps/public/leftpiez/";
    private static final String INSERT_DATA_URL = BASE_URL + "inputdata";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "getpengukuran";
    private static final String GET_DATA_URL = BASE_URL + "getdata";
    private static final String HITUNG_URL = BASE_URL + "hitung/hitunglokasi/";

    // Data pengukuran
    private final Map<String, Integer> pengukuranMap = new HashMap<>();
    private final List<String> tanggalList = new ArrayList<>();
    private ArrayAdapter<String> pengukuranAdapter;
    private ArrayAdapter<String> lokasiAdapter;

    // Daftar lokasi piezometer
    private final String[] LOKASI_PIEZOMETER = {
            "L01", "L02", "L03", "L04", "L05",
            "L06", "L07", "L08", "L09", "L10", "SPZ02"
    };

    // FITUR OFFLINE & SINKRONISASI BARU
    private OfflineDataHelperLeftPiezo offlineDb;
    private SharedPreferences syncPrefs;
    private boolean isSyncInProgress = false;
    private Handler networkCheckHandler = new Handler();
    private Runnable networkCheckRunnable;
    private boolean lastOnlineStatus = false;

    // Interface untuk callback
    interface HitungCallback {
        void onComplete(boolean success);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data_leftpiezo);

        // INISIALISASI FITUR OFFLINE BARU
        offlineDb = new OfflineDataHelperLeftPiezo(this);
        syncPrefs = getSharedPreferences("leftpiezo_sync_prefs", MODE_PRIVATE);
        calendar = Calendar.getInstance();

        initModalComponents();
        initFormComponents();
        initSpinnerComponents();
        setupModalDropdowns();
        setupModalCalendar();

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
            if (offlineDb.hasUnsyncedDataLeftPiezo()) {
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

        int offlineCount = offlineDb.getOfflineDataCountLeftPiezo();
        boolean hasPendingCalc = hasPendingCalculations();

        if (offlineCount > 0 || hasPendingCalc) {
            Log.d("LEFTPIEZO_AutoSync", "Found " + offlineCount + " offline data and " +
                    (hasPendingCalc ? "pending calculations" : "no pending calculations"));
            triggerAutoSync();
        }
    }

    private boolean hasPendingCalculations() {
        SharedPreferences prefs = getSharedPreferences("leftpiezo_pending_calc", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        return !allEntries.isEmpty();
    }

    private void triggerAutoSync() {
        if (isSyncInProgress) return;

        Log.d("LEFTPIEZO_AutoSync", "Triggering auto-sync for offline data");
        isSyncInProgress = true;
        showToast("🔄 Auto-sync data offline...");

        syncAllOfflineDataAuto(() -> {
            isSyncInProgress = false;
            Log.d("LEFTPIEZO_AutoSync", "Auto-sync completed");
            runOnUiThread(this::loadPengukuranData);
        });
    }

    private void syncAllOfflineDataAuto(Runnable onComplete) {
        int offlineCount = offlineDb.getOfflineDataCountLeftPiezo();
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
        List<Map<String,String>> list = offlineDb.getUnsyncedDataLeftPiezo(tableType);
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
            offlineDb.deleteByTempIdLeftPiezo(tableType, tempId);
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
                        offlineDb.deleteByTempIdLeftPiezo(tableType, tempId);
                        Log.d("LEFTPIEZO_AutoSync", "Synced " + tableType + " tempId=" + tempId);

                        // Tandai perlu perhitungan jika ini data pembacaan
                        if (tableType.equals("data") && json.has("mode")) {
                            String mode = json.optString("mode", "");
                            if (mode.startsWith("pembacaan_")) {
                                int pengukuranId = json.optInt("pengukuran_id", -1);
                                String lokasi = mode.replace("pembacaan_", "").toUpperCase();
                                if (pengukuranId != -1) {
                                    tandaiPerluHitungPiezo(tempId, pengukuranId, lokasi);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("LEFTPIEZO_AutoSync", "Failed to sync tempId=" + tempId + ": " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            } catch (Exception e) {
                Log.e("LEFTPIEZO_AutoSync", "JSON parse failed for tempId=" + tempId + ": " + e.getMessage());
                offlineDb.deleteByTempIdLeftPiezo(tableType, tempId);
            }

            runOnUiThread(() -> syncDataItemAuto(tableType, dataList, index + 1, onFinish));
        }).start();
    }

    // ==================== STRATEGI SINKRONISASI GROUP BY PENGUKURAN ====================

    private void syncAllOfflineData(Runnable onComplete) {
        boolean adaData = offlineDb.hasUnsyncedDataLeftPiezo();
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

                Log.d("LEFTPIEZO_Sync", "Syncing data for " + groupedData.size() + " pengukuran groups");

                List<Integer> pengukuranIds = new ArrayList<>(groupedData.keySet());
                syncPengukuranGroup(pengukuranIds, groupedData, 0, onComplete);

            } catch (Exception e) {
                Log.e("LEFTPIEZO_Sync", "Error in grouped sync: " + e.getMessage());
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

        Log.d("LEFTPIEZO_Sync", "Syncing group for pengukuran_id: " + pengukuranId + ", data count: " + dataList.size());

        syncDataItemsInGroup(dataList, pengukuranId, 0, () -> {
            syncPengukuranGroup(pengukuranIds, groupedData, index + 1, onComplete);
        });
    }

    private void syncDataItemsInGroup(List<Map<String, String>> dataList, int pengukuranId,
                                      int dataIndex, Runnable onGroupComplete) {
        if (dataIndex >= dataList.size()) {
            hitungPiezoUntukPengukuran(pengukuranId, onGroupComplete);
            return;
        }

        Map<String, String> item = dataList.get(dataIndex);
        String tempId = item.get("temp_id");
        String jsonStr = item.get("json");
        String mode = item.get("mode");

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
                        offlineDb.deleteByTempIdLeftPiezo("data", tempId);
                        Log.d("LEFTPIEZO_Sync", "Synced data for pengukuran " + pengukuranId + ", mode: " + mode);

                        // Tandai perlu perhitungan jika ini data pembacaan
                        if (mode.startsWith("pembacaan_")) {
                            String lokasi = mode.replace("pembacaan_", "").toUpperCase();
                            tandaiPerluHitungPiezo(tempId, pengukuranId, lokasi);
                        }
                    }
                } catch (Exception e) {
                    Log.e("LEFTPIEZO_Sync", "Failed to sync tempId=" + tempId + ": " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            } catch (Exception e) {
                Log.e("LEFTPIEZO_Sync", "JSON parse failed for tempId=" + tempId + ": " + e.getMessage());
            }

            runOnUiThread(() -> syncDataItemsInGroup(dataList, pengukuranId, dataIndex + 1, onGroupComplete));
        }).start();
    }

    private void hitungPiezoUntukPengukuran(int pengukuranId, Runnable onComplete) {
        new Thread(() -> {
            try {
                List<String> lokasiList = offlineDb.getLokasiForPengukuran(pengukuranId);

                Log.d("LEFTPIEZO_Sync", "Calculating piezometer for pengukuran " + pengukuranId + ", lokasi: " + lokasiList);

                hitungPiezoSequential(lokasiList, pengukuranId, 0, onComplete);

            } catch (Exception e) {
                Log.e("LEFTPIEZO_Sync", "Error calculating piezometer for pengukuran " + pengukuranId + ": " + e.getMessage());
                runOnUiThread(onComplete);
            }
        }).start();
    }

    private void hitungPiezoSequential(List<String> lokasiList, int pengukuranId, int lokasiIndex, Runnable onComplete) {
        if (lokasiIndex >= lokasiList.size()) {
            runOnUiThread(onComplete);
            return;
        }

        String lokasi = lokasiList.get(lokasiIndex);
        hitungPiezoSingle(lokasi, pengukuranId, (success) -> {
            if (success) {
                Log.d("LEFTPIEZO_Sync", "Successfully calculated piezometer for " + lokasi + ", pengukuran " + pengukuranId);
            }
            hitungPiezoSequential(lokasiList, pengukuranId, lokasiIndex + 1, onComplete);
        });
    }

    // ==================== FITUR PERHITUNGAN PIEZOMETER ====================

    private void tandaiPerluHitungPiezo(String tempId, int pengukuranId, String lokasi) {
        SharedPreferences prefs = getSharedPreferences("leftpiezo_pending_calc", MODE_PRIVATE);
        String key = "pending_" + tempId;

        Map<String, String> pendingData = new HashMap<>();
        pendingData.put("pengukuran_id", String.valueOf(pengukuranId));
        pendingData.put("lokasi", lokasi);
        pendingData.put("temp_id", tempId);

        try {
            JSONObject json = new JSONObject(pendingData);
            prefs.edit().putString(key, json.toString()).apply();
            Log.d("PENDING_CALC_PIEZO", "Data ditandai perlu hitung: " + json.toString());
        } catch (Exception e) {
            Log.e("PENDING_CALC_PIEZO", "Gagal menyimpan pending calculation: " + e.getMessage());
        }
    }

    private void prosesPerhitunganTertunda() {
        if (!isInternetAvailable()) return;

        SharedPreferences prefs = getSharedPreferences("leftpiezo_pending_calc", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        if (allEntries.isEmpty()) return;

        Log.d("PENDING_CALC_PIEZO", "Processing " + allEntries.size() + " pending calculations");
        showToast("🔄 Memproses " + allEntries.size() + " perhitungan tertunda...");

        Map<Integer, List<String>> pendingByPengukuran = new HashMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("pending_")) {
                try {
                    String jsonStr = (String) entry.getValue();
                    JSONObject json = new JSONObject(jsonStr);

                    int pendingPengukuranId = json.getInt("pengukuran_id");
                    String pendingLokasi = json.getString("lokasi");
                    String pendingTempId = json.getString("temp_id");

                    if (!pendingByPengukuran.containsKey(pendingPengukuranId)) {
                        pendingByPengukuran.put(pendingPengukuranId, new ArrayList<String>());
                    }
                    pendingByPengukuran.get(pendingPengukuranId).add(pendingLokasi);

                } catch (Exception e) {
                    Log.e("PENDING_CALC_PIEZO", "Error processing pending calculation: " + e.getMessage());
                }
            }
        }

        prosesPendingByPengukuran(new ArrayList<>(pendingByPengukuran.keySet()), pendingByPengukuran, 0);
    }

    private void prosesPendingByPengukuran(List<Integer> pengukuranIds,
                                           Map<Integer, List<String>> pendingByPengukuran,
                                           int index) {
        if (index >= pengukuranIds.size()) {
            showToast("✅ Semua perhitungan tertunda selesai");
            return;
        }

        int pengukuranId = pengukuranIds.get(index);
        List<String> lokasiList = pendingByPengukuran.get(pengukuranId);

        Log.d("PENDING_CALC_PIEZO", "Processing pending calculations for pengukuran " + pengukuranId + ": " + lokasiList);

        hitungPiezoSequential(lokasiList, pengukuranId, 0, () -> {
            hapusPendingUntukPengukuran(pengukuranId);
            prosesPendingByPengukuran(pengukuranIds, pendingByPengukuran, index + 1);
        });
    }

    private void hapusPendingUntukPengukuran(int pengukuranId) {
        SharedPreferences prefs = getSharedPreferences("leftpiezo_pending_calc", MODE_PRIVATE);
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
                    Log.e("PENDING_CALC_PIEZO", "Error removing pending calculation: " + e.getMessage());
                }
            }
        }
    }

    private void hitungPiezoSingle(String lokasi, int pengukuranId, HitungCallback callback) {
        new Thread(() -> {
            boolean success = false;
            HttpURLConnection conn = null;
            try {
                String url = HITUNG_URL + pengukuranId + "/" + lokasi;
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject response = new JSONObject(sb.toString());

                    // ✅ PERBAIKAN: Gunakan "success" bukan "status"
                    success = response.optBoolean("success", false);

                    if (success) {
                        // ✅ PERBAIKAN: Ambil result dari field yang benar
                        double result = response.optDouble("result", 0);
                        Log.d("HITUNG_PIEZO", "Berhasil hitung " + lokasi + ": " + result);
                    } else {
                        String message = response.optString("message", "Gagal menghitung");
                        Log.e("HITUNG_PIEZO", "Gagal hitung " + lokasi + ": " + message);
                    }
                } else {
                    Log.e("HITUNG_PIEZO", "Gagal hitung " + lokasi + ", response code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("HITUNG_PIEZO", "Error hitung " + lokasi + ": " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
                callback.onComplete(success);
            }
        }).start();
    }

    // ==================== HANDLER INPUT DATA DENGAN OFFLINE SUPPORT ====================

    private void handleSimpanPembacaan() {
        if (pengukuranId == -1) {
            showToast("❌ Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        if (!validatePembacaanFields()) {
            showToast("❌ Harap isi minimal satu field pembacaan");
            return;
        }

        // CEK DATA EXISTING SEBELUM SIMPAN
        cekDataExistingSebelumSimpan();
    }

    private void cekDataExistingSebelumSimpan() {
        if (!isInternetAvailable()) {
            // Jika offline, langsung simpan offline tanpa cek existing
            simpanPembacaanOffline();
            return;
        }

        new Thread(() -> {
            try {
                String url = GET_DATA_URL + "?pengukuran_id=" + pengukuranId + "&lokasi=" + selectedLokasi;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject response = new JSONObject(sb.toString());
                    // ✅ PERBAIKAN: Gunakan "success" bukan "status"
                    if (response.optBoolean("success", false)) {
                        JSONObject data = response.optJSONObject("data");

                        // Jika data sudah ada, tampilkan pesan error dan batalkan
                        if (data != null && (data.has("feet") || data.has("inch"))) {
                            runOnUiThread(() -> {
                                showToast("❌ Data pembacaan " + selectedLokasi + " sudah ada! Tidak dapat menambah data baru.");
                                clearPembacaanSection();
                            });
                            return;
                        }
                    }
                }

                // Jika data belum ada, lanjutkan dengan penyimpanan
                runOnUiThread(() -> {
                    if (isInternetAvailable()) {
                        simpanDanHitungPembacaan();
                    } else {
                        simpanPembacaanOffline();
                    }
                });

            } catch (Exception e) {
                Log.e("CEK_DATA", "Error cek data: " + e.getMessage());
                // Jika error saat cek, tetap lanjutkan penyimpanan
                runOnUiThread(() -> {
                    if (isInternetAvailable()) {
                        simpanDanHitungPembacaan();
                    } else {
                        simpanPembacaanOffline();
                    }
                });
            }
        }).start();
    }

    private void simpanDanHitungPembacaan() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "pembacaan_" + selectedLokasi.toLowerCase());
        data.put("pengukuran_id", String.valueOf(pengukuranId));

        // FEET: Bisa angka atau teks (tidak perlu validasi khusus)
        String feetValue = inputFeet.getText().toString().trim();
        if (!feetValue.isEmpty()) {
            data.put("feet", feetValue);
        }

        // INCH: Tetap angka (validasi opsional)
        String inchValue = inputInch.getText().toString().trim();
        if (!inchValue.isEmpty()) {
            if (isNumeric(inchValue)) {
                data.put("inch", inchValue);
            } else {
                showToast("❌ Nilai inch harus angka");
                return;
            }
        }

        Log.d("LEFTPIEZO_API", "Menyimpan & menghitung data " + selectedLokasi + ": " + data.toString());
        sendToServerWithCalculation(data, "Pembacaan");
    }

    private void simpanPembacaanOffline() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "pembacaan_" + selectedLokasi.toLowerCase());
        data.put("pengukuran_id", String.valueOf(pengukuranId));

        String feetValue = inputFeet.getText().toString().trim();
        if (!feetValue.isEmpty()) {
            data.put("feet", feetValue);
        }

        String inchValue = inputInch.getText().toString().trim();
        if (!inchValue.isEmpty() && isNumeric(inchValue)) {
            data.put("inch", inchValue);
        }

        String localTempId = "local_" + System.currentTimeMillis() + "_" + selectedLokasi.toLowerCase();
        data.put("temp_id", localTempId);

        boolean success = saveOffline("data", localTempId, data);
        if (success) {
            showToast("📱 Data " + selectedLokasi + " disimpan offline\n⚠️ Perhitungan akan dilakukan saat online");
            clearPembacaanSection();
            tandaiPerluHitungPiezo(localTempId, pengukuranId, selectedLokasi);
        } else {
            showToast("❌ Gagal menyimpan data offline");
        }
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

        if (isInternetAvailable()) {
            updateDMAPengukuran();
        } else {
            simpanDMAOffline();
        }
    }

    private void simpanDMAOffline() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "update_dma");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("dma", inputDMA.getText().toString().trim());

        String localTempId = "local_" + System.currentTimeMillis() + "_dma";
        data.put("temp_id", localTempId);

        boolean success = saveOffline("data", localTempId, data);
        if (success) {
            showToast("📱 Data DMA disimpan offline");
            clearDMASection();
        } else {
            showToast("❌ Gagal menyimpan data DMA offline");
        }
    }

    private boolean saveOffline(String tableType, String tempId, Map<String, String> data) {
        try {
            JSONObject json = new JSONObject(data);

            if (tableType.equals("data") && data.containsKey("mode")) {
                String mode = data.get("mode");
                boolean success = offlineDb.insertDataLeftPiezoWithMode(tableType, tempId, json.toString(),
                        pengukuranId, mode);
                return success;
            } else {
                boolean success = offlineDb.insertDataLeftPiezo(tableType, tempId, json.toString());
                return success;
            }

        } catch (Exception e) {
            Log.e("LEFTPIEZO_Offline", "Gagal simpan offline: " + e.getMessage());
            showToast("❌ Gagal simpan offline: " + e.getMessage());
            return false;
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

        Log.d("LEFTPIEZO_API", "Mengirim data pengukuran: " + data.toString());

        if (isInternetAvailable()) {
            sendPengukuranToServer(data);
        } else {
            String localTempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", localTempId);
            boolean success = saveOffline("pengukuran", localTempId, data);
            if (success) {
                showToast("📱 Data pengukuran disimpan offline");
                hideModal();
            } else {
                showToast("❌ Gagal menyimpan data pengukuran offline");
            }
        }
    }

    // ==================== METHOD UTILITAS SINKRONISASI ====================

    private boolean isAlreadySynced() {
        SharedPreferences prefs = getSharedPreferences("leftpiezo_app_prefs", MODE_PRIVATE);
        String lastSyncDate = prefs.getString("last_sync_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return today.equals(lastSyncDate);
    }

    private void markAsSynced() {
        SharedPreferences prefs = getSharedPreferences("leftpiezo_app_prefs", MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString("last_sync_date", today).apply();
    }

    private void syncDataSerial(String tableType, Runnable next) {
        List<Map<String, String>> dataList = offlineDb.getUnsyncedDataLeftPiezo(tableType);
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
                        offlineDb.deleteByTempIdLeftPiezo(tableType, tempId);
                        Log.d("LEFTPIEZO_Sync", "Data " + tableType + " tempId=" + tempId + " berhasil disinkronisasi");
                    }
                } catch (Exception e) {
                    Log.e("LEFTPIEZO_Sync", "Error sync " + tableType + " tempId=" + tempId, e);
                } finally {
                    if (conn != null) conn.disconnect();
                }

                runOnUiThread(() -> syncDataItem(tableType, dataList, index + 1, onFinish));
            }).start();

        } catch (Exception e) {
            Log.e("LEFTPIEZO_Sync", "JSON parse error untuk data " + tableType + " tempId=" + tempId, e);
            runOnUiThread(() -> syncDataItem(tableType, dataList, index + 1, onFinish));
        }
    }

    // ==================== METHOD-METHOD YANG SUDAH ADA (DIMODIFIKASI SEDIKIT) ====================

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

        // Set click listeners untuk modal
        if (modalBtnSubmitPengukuran != null) modalBtnSubmitPengukuran.setOnClickListener(v -> handleModalPengukuran());
        if (btnCloseModal != null) btnCloseModal.setOnClickListener(v -> hideModal());
        if (modalOverlay != null) modalOverlay.setOnClickListener(v -> hideModal());
    }

    private void initFormComponents() {
        inputDMA = findViewById(R.id.inputDMA);
        inputFeet = findViewById(R.id.inputFeet);
        inputInch = findViewById(R.id.inputInch);

        btnSimpanDMA = findViewById(R.id.btnSimpanDMA);
        btnSimpanPembacaan = findViewById(R.id.btnSimpanPembacaan);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        spinnerLokasi = findViewById(R.id.spinnerLokasi);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
        titlePiezometer = findViewById(R.id.titlePiezometer);

        // Set click listeners
        btnSimpanDMA.setOnClickListener(v -> handleSimpanDMA());
        btnSimpanPembacaan.setOnClickListener(v -> handleSimpanPembacaan());
    }

    private void initSpinnerComponents() {
        // Adapter untuk spinner pengukuran
        pengukuranAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        pengukuranAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(pengukuranAdapter);

        // Adapter untuk spinner lokasi
        lokasiAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, LOKASI_PIEZOMETER);
        lokasiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLokasi.setAdapter(lokasiAdapter);

        btnPilihPengukuran.setOnClickListener(v -> {
            Object selected = spinnerPengukuran.getSelectedItem();
            if (selected != null && pengukuranMap.containsKey(selected.toString())) {
                pengukuranId = pengukuranMap.get(selected.toString());
                showToast("✅ Pengukuran dipilih: " + selected);
                loadExistingData();
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

        spinnerLokasi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLokasi = LOKASI_PIEZOMETER[position];
                updateTitle();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTitle() {
        if (titlePiezometer != null) {
            titlePiezometer.setText("Piezometer - " + selectedLokasi);
        }
    }

    private void clearDMASection() {
        runOnUiThread(() -> {
            inputDMA.setText("");
        });
    }

    private void clearPembacaanSection() {
        runOnUiThread(() -> {
            inputFeet.setText("");
            inputInch.setText("");
        });
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
                String url = GET_DATA_URL + "?pengukuran_id=" + pengukuranId + "&lokasi=" + selectedLokasi;
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
                    // ✅ PERBAIKAN: Gunakan "success" bukan "status"
                    if (response.optBoolean("success", false)) {
                        JSONObject data = response.optJSONObject("data");
                        runOnUiThread(() -> populateForm(data));
                    }
                }
            } catch (Exception e) {
                Log.e("LOAD_DATA", "Error loading data: " + e.getMessage());
            }
        }).start();
    }

    private void loadDataFromOffline() {
        Map<String, String> data = offlineDb.getLeftPiezoData(pengukuranId, selectedLokasi);
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
            if (data.has("feet")) inputFeet.setText(data.getString("feet"));
            if (data.has("inch")) inputInch.setText(data.getString("inch"));
        } catch (Exception e) {
            Log.e("POPULATE_FORM", "Error populating form: " + e.getMessage());
        }
    }

    private void updateDMAPengukuran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "update_dma");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("dma", inputDMA.getText().toString().trim());

        Log.d("LEFTPIEZO_API", "Mengupdate DMA pengukuran: " + data.toString());
        sendToServer(data, "DMA");
    }

    // METHOD: Kirim data dengan perhitungan (untuk Pembacaan)
    private void sendToServerWithCalculation(Map<String, String> dataMap, String dataType) {
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
                Log.d("LEFTPIEZO_API", "JSON yang dikirim (" + dataType + "): " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("LEFTPIEZO_API", "Response Code (" + dataType + "): " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("LEFTPIEZO_API", "Response Body (" + dataType + "): " + responseBody);

                JSONObject response = new JSONObject(responseBody);

                // ✅ PERBAIKAN FINAL: Gunakan "status" bukan "success"
                String status = response.optString("status", "");
                String message = response.optString("message", "");
                boolean success = "success".equals(status);

                runOnUiThread(() -> {
                    if (success) {
                        showToast("✅ " + message);
                        clearPembacaanSection();

                        // ✅ PERBAIKAN: Trigger perhitungan hanya untuk data pembacaan
                        if (dataType.equals("Pembacaan")) {
                            Log.d("LEFTPIEZO_CALC", "Triggering calculation for " + selectedLokasi);
                            triggerPerhitungan();
                        }
                    } else {
                        showToast("❌ " + message);
                        // JIKA GAGAL, SIMPAN OFFLINE
                        if (!dataMap.containsKey("temp_id")) {
                            String localTempId = "local_" + System.currentTimeMillis();
                            dataMap.put("temp_id", localTempId);
                            saveOffline("data", localTempId, dataMap);
                        }
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

    // METHOD: Kirim data tanpa perhitungan (untuk DMA)
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
                Log.d("LEFTPIEZO_API", "JSON yang dikirim (" + dataType + "): " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("LEFTPIEZO_API", "Response Code (" + dataType + "): " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("LEFTPIEZO_API", "Response Body (" + dataType + "): " + responseBody);

                JSONObject response = new JSONObject(responseBody);

                // ✅ PERBAIKAN: Gunakan struktur response baru
                boolean success = response.optBoolean("success", false);
                String message = response.optString("message", "");

                runOnUiThread(() -> {
                    if (success) {
                        showToast("✅ " + message);
                        if (dataType.equals("DMA")) {
                            clearDMASection();
                        }
                    } else {
                        showToast("❌ " + message);
                        if (!dataMap.containsKey("temp_id")) {
                            String localTempId = "local_" + System.currentTimeMillis();
                            dataMap.put("temp_id", localTempId);
                            saveOffline("data", localTempId, dataMap);
                        }
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

    // METHOD: Trigger perhitungan setelah simpan pembacaan berhasil
    private void triggerPerhitungan() {
        if (pengukuranId == -1) {
            Log.e("LEFTPIEZO_CALC", "Pengukuran ID tidak valid");
            return;
        }

        new Thread(() -> {
            try {
                String url = HITUNG_URL + pengukuranId + "/" + selectedLokasi;
                Log.d("LEFTPIEZO_CALC", "Calculating URL: " + url);

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int responseCode = conn.getResponseCode();
                Log.d("LEFTPIEZO_CALC", "Calculation Response Code: " + responseCode);

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()
                ));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("LEFTPIEZO_CALC", "Calculation Response: " + responseBody);

                JSONObject response = new JSONObject(responseBody);

                // ✅ PERBAIKAN FINAL: Gunakan "status" bukan "success"
                String status = response.optString("status", "");
                String message = response.optString("message", "");
                boolean success = "success".equals(status);

                runOnUiThread(() -> {
                    if (success) {
                        // ✅ PERBAIKAN: Ambil result dari field data
                        double result = 0;
                        try {
                            JSONObject data = response.optJSONObject("data");
                            if (data != null && data.has("rumus")) {
                                JSONObject rumus = data.getJSONObject("rumus");
                                result = rumus.optDouble("hasil_rumus", 0);
                            }
                        } catch (Exception e) {
                            Log.e("LEFTPIEZO_CALC", "Error parsing result: " + e.getMessage());
                        }

                        showToast("🧮 Perhitungan berhasil: " + result);
                        Log.d("LEFTPIEZO_CALC", "Calculation successful - Result: " + result);
                    } else {
                        showToast("⚠️ " + message);
                        Log.e("LEFTPIEZO_CALC", "Calculation failed: " + message);
                    }
                });

            } catch (Exception e) {
                Log.e("TRIGGER_CALC", "Error trigger perhitungan: " + e.getMessage());
                runOnUiThread(() -> {
                    showToast("⚠️ Data tersimpan, tapi perhitungan gagal: " + e.getMessage());
                });
            }
        }).start();
    }

    // METHOD: Kirim data pengukuran
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
                Log.d("LEFTPIEZO_API", "JSON pengukuran: " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("LEFTPIEZO_API", "Response Code: " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("LEFTPIEZO_API", "Response Body: " + responseBody);

                JSONObject response = new JSONObject(responseBody);

                // ✅ BENAR - gunakan struktur response yang sesuai dengan log
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                // Cek berdasarkan "status": "success" bukan "success": true
                boolean success = "success".equals(status);

                if (response.has("pengukuran_id")) {
                    pengukuranId = response.optInt("pengukuran_id", -1);
                    Log.d("LEFTPIEZO_API", "Pengukuran ID diterima: " + pengukuranId);
                }

                runOnUiThread(() -> {
                    if (success) {
                        showToast("✅ " + message);
                        hideModal();
                    } else {
                        showToast("❌ " + message);
                        if (!dataMap.containsKey("temp_id")) {
                            String localTempId = "local_" + System.currentTimeMillis();
                            dataMap.put("temp_id", localTempId);
                            saveOffline("pengukuran", localTempId, dataMap);
                        }
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

    // VALIDATION METHODS
    private boolean validateDMAFields() {
        return !inputDMA.getText().toString().trim().isEmpty();
    }

    private boolean validatePembacaanFields() {
        return !inputFeet.getText().toString().trim().isEmpty() ||
                !inputInch.getText().toString().trim().isEmpty();
    }

    // METHOD BARU: Validasi angka untuk inch
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // MODAL METHODS
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

    // LOAD PENGUKURAN DATA
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
                Log.d("LEFTPIEZO_API", "Response get-pengukuran: " + responseBody);

                JSONObject response = new JSONObject(responseBody);

                // ✅ PERBAIKAN FINAL: Gunakan "status" bukan "success"
                String status = response.optString("status", "");
                boolean success = "success".equals(status);

                if (success) {
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
                                // ✅ Tampilkan pesan sukses, bukan error
                                showToast("📅 " + tanggalList.size() + " data pengukuran dimuat");
                            } else {
                                showToast("ℹ️ Tidak ada data pengukuran tersedia");
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
                        // ❌ Hanya tampilkan error jika benar-benar gagal
                        showToast("❌ " + message);
                        loadTanggalOffline();
                    });
                }

            } catch (Exception e) {
                Log.e("LOAD_PENGUKURAN", "Error: ", e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal load data: " + e.getMessage());
                    loadTanggalOffline();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void loadTanggalOffline() {
        try {
            List<Map<String,String>> rows = offlineDb.getPengukuranMasterLeftPiezo();
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
            Log.e("LEFTPIEZO_Offline", "Error load offline master: " + e.getMessage());
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

    // UTILITY METHODS
    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d("LEFTPIEZO_TOAST", "Pesan: " + message);
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