package com.example.kerjapraktik;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class InputDataActivity extends AppCompatActivity {

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
    private Spinner spinnerPengukuran;
    private Button btnPilihPengukuran, btnSubmitTmaWaduk, btnSubmitThomson, btnSubmitSR, btnSubmitBocoran;
    private EditText inputTmaWaduk, inputA1R, inputA1L, inputB1, inputB3, inputB5;
    private EditText inputElv624T1, inputElv615T2, inputPipaP1;
    private Spinner elv624T1Kode, elv615T2Kode, pipaP1Kode;

    // Tombol Hitung Semua Data
    private Button btnHitungSemua;

    private Calendar calendar;
    private String tempId = null;
    private int pengukuranId = -1;

    private final int[] srKodeArray = {1, 40, 66, 68, 70, 79, 81, 83, 85, 92, 94, 96, 98, 100, 102, 104, 106};
    private final Map<Integer, Spinner> srKodeSpinners = new HashMap<>();

    private OfflineDataHelper offlineDb;
    private SharedPreferences syncPrefs;

    // API URL
    private static final String BASE_URL = "http://10.30.52.217/API_Android/public/rembesan/";
    private static final String INSERT_DATA_URL = BASE_URL + "input";
    private static final String CEK_DATA_URL = BASE_URL + "cek-data";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "get_pengukuran";
    private static final String HITUNG_SEMUA_URL = BASE_URL + "Rumus-Rembesan";

    // Map untuk simpan pasangan tanggal → ID
    private final Map<String, Integer> pengukuranMap = new HashMap<>();

    // simpan list & adapter agar bisa refresh + notify
    private final List<String> tanggalList = new ArrayList<>();
    private ArrayAdapter<String> pengukuranAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data);

        // inisialisasi database offline + prefs
        offlineDb = new OfflineDataHelper(this);
        syncPrefs = getSharedPreferences("sync_prefs", MODE_PRIVATE);
        calendar = Calendar.getInstance();

        // init UI components dulu (important)
        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);

        initModalComponents();
        initFormComponents();
        setupSpinners();
        setupModalDropdowns();
        setupModalCalendar();

        // Sembunyikan field yang tidak diperlukan di HP 1
        hideUnnecessaryFieldsHP1();

        // Siapkan adapter spinner (awal kosong, nanti diisi saat load)
        pengukuranAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        pengukuranAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(pengukuranAdapter);

        // Ambil pengukuran_id dari SharedPreferences (jika ada)
        SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
        pengukuranId = prefs.getInt("pengukuran_id", -1);

        // set listener spinner
        spinnerPengukuran.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = position >= 0 && position < tanggalList.size() ? tanggalList.get(position) : null;
                if (selected != null && pengukuranMap.containsKey(selected)) {
                    pengukuranId = pengukuranMap.get(selected);
                    getSharedPreferences("pengukuran", MODE_PRIVATE).edit().putInt("pengukuran_id", pengukuranId).apply();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // sekarang sinkron pengukuran master (dipanggil setelah adapter ready & UI inisialisasi)
        syncPengukuranMaster();

        // tombol pilih pengukuran
        btnPilihPengukuran.setOnClickListener(v -> {
            Object sel = spinnerPengukuran.getSelectedItem();
            if (sel == null) {
                showElegantToast("Pilih tanggal pengukuran dulu.", "warning");
                return;
            }

            String selected = sel.toString();
            if (pengukuranMap.containsKey(selected)) {
                pengukuranId = pengukuranMap.get(selected);
                getSharedPreferences("pengukuran", MODE_PRIVATE)
                        .edit()
                        .putInt("pengukuran_id", pengukuranId)
                        .apply();

                showElegantToast("Tanggal terpilih: " + selected, "success");

            } else {
                showElegantToast("Tanggal tidak dikenali, coba sinkron ulang.", "error");
            }
        });

        // set click listeners (pastikan sudah di-init di initFormComponents)
        if (btnSubmitTmaWaduk != null) btnSubmitTmaWaduk.setOnClickListener(v -> handleTmaWaduk());
        if (btnSubmitThomson != null) btnSubmitThomson.setOnClickListener(v -> handleThomsonHP1());
        if (btnSubmitSR != null) btnSubmitSR.setOnClickListener(v -> handleSR());
        if (btnSubmitBocoran != null) btnSubmitBocoran.setOnClickListener(v -> handleBocoran());
        if (btnHitungSemua != null) btnHitungSemua.setOnClickListener(v -> handleHitungSemua());

        // tampilkan modal jika komponen ada
        if (modalPengukuran != null && modalOverlay != null && mainContent != null) {
            showModal();
        }
    }

    // METHOD BARU: Sembunyikan field yang tidak diperlukan di HP 1 (SIMPLE VERSION)
    private void hideUnnecessaryFieldsHP1() {
        // Sembunyikan B3 dan B5
        if (inputB3 != null) inputB3.setVisibility(View.GONE);
        if (inputB5 != null) inputB5.setVisibility(View.GONE);

        // Sembunyikan tombol SR
        if (btnSubmitSR != null) btnSubmitSR.setVisibility(View.GONE);

        // Update judul dan tombol Thomson
        if (findViewById(R.id.thomson_title) != null) {
            TextView thomsonTitle = findViewById(R.id.thomson_title);
            thomsonTitle.setText("Thomson Weir - GALLERY (A1 R, A1 L, B1)");
        }
        if (btnSubmitThomson != null) {
            btnSubmitThomson.setText("Simpan Thomson - Gallery");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // jalankan sinkronisasi offline -> online saat internet tersedia
        if (isInternetAvailable()) {
            if (offlineDb.hasUnsyncedData()) {
                syncAllOfflineData(() -> {
                    if (!isAlreadySynced()) {
                        showElegantToast("Sinkronisasi data offline selesai", "success");
                        markAsSynced();
                    }
                });
            } else {
                // jika tidak ada data unsynced, masih bisa update master
                syncPengukuranMaster();
            }
        }
    }

    /** Cek apakah sudah pernah sinkron hari ini */
    private boolean isAlreadySynced() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String lastSyncDate = prefs.getString("last_sync_date", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return today.equals(lastSyncDate);
    }

    /** Tandai sudah sinkron hari ini */
    private void markAsSynced() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString("last_sync_date", today).apply();
    }

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
        inputTmaWaduk = findViewById(R.id.inputTmaWaduk);
        inputA1R = findViewById(R.id.inputA1R);
        inputA1L = findViewById(R.id.inputA1L);
        inputB1 = findViewById(R.id.inputB1);
        inputB3 = findViewById(R.id.inputB3);
        inputB5 = findViewById(R.id.inputB5);
        inputElv624T1 = findViewById(R.id.inputElv624T1);
        inputElv615T2 = findViewById(R.id.inputElv615T2);
        inputPipaP1 = findViewById(R.id.inputPipaP1);

        btnSubmitTmaWaduk = findViewById(R.id.btnSubmitTmaWaduk);
        btnSubmitThomson = findViewById(R.id.btnSubmitThomson);
        btnSubmitSR = findViewById(R.id.btnSubmitSR);
        btnSubmitBocoran = findViewById(R.id.btnSubmitBocoran);

        btnHitungSemua = findViewById(R.id.btnHitungSemua);

        elv624T1Kode = findViewById(R.id.elv_624_t1_kode);
        elv615T2Kode = findViewById(R.id.elv_615_t2_kode);
        pipaP1Kode = findViewById(R.id.pipa_p1_kode);

        // inisialisasi SR spinners secara defensif
        for (int kode : srKodeArray) {
            try {
                int resId = getResources().getIdentifier("sr_" + kode + "_kode", "id", getPackageName());
                if (resId != 0) {
                    Spinner spinner = findViewById(resId);
                    if (spinner != null) {
                        srKodeSpinners.put(kode, spinner);
                    }
                }
            } catch (Exception e) {
                Log.w("INIT_FORM", "SR spinner tidak ditemukan untuk kode " + kode);
            }
        }
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.kode_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        for (int kode : srKodeArray) {
            Spinner s = srKodeSpinners.get(kode);
            if (s != null) s.setAdapter(adapter);
        }

        if (elv624T1Kode != null) elv624T1Kode.setAdapter(adapter);
        if (elv615T2Kode != null) elv615T2Kode.setAdapter(adapter);
        if (pipaP1Kode != null) pipaP1Kode.setAdapter(adapter);
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
            Log.w("SETUP_MODAL", "Gagal setup dropdown: " + e.getMessage());
        }
    }

    private void setupModalCalendar() {
        if (modalInputTanggal != null) {
            modalInputTanggal.setOnClickListener(v -> showModalDatePickerDialog());
            try {
                TextInputLayout tanggalLayout = (TextInputLayout) modalInputTanggal.getParent().getParent();
                if (tanggalLayout != null) {
                    tanggalLayout.setEndIconOnClickListener(v -> showModalDatePickerDialog());
                }
            } catch (Exception ignored) {}
        }
    }

    private void showModalDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // update calendar and format tanggal (yyyy-MM-dd)
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    if (modalInputTanggal != null) modalInputTanggal.setText(dateFormat.format(calendar.getTime()));

                    // set tahun otomatis
                    if (modalInputTahun != null) modalInputTahun.setText(String.valueOf(selectedYear));

                    // set bulan otomatis (gunakan nama dari resources bulan_options)
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

                    // hitung triwulan otomatis berdasarkan bulan (selectedMonth: 0..11)
                    String triwulan;
                    if (selectedMonth <= 2) { // Jan-Mar
                        triwulan = "TW-1";
                    } else if (selectedMonth <= 5) { // Apr-Jun
                        triwulan = "TW-2";
                    } else if (selectedMonth <= 8) { // Jul-Sep
                        triwulan = "TW-3";
                    } else { // Oct-Dec
                        triwulan = "TW-4";
                    }
                    // set di modalInputPeriode (AutoCompleteTextView) agar user masih bisa ubah manual
                    if (modalInputPeriode != null) {
                        // setText dengan filter = false agar tidak memicu filtering dropdown
                        try {
                            // AutoCompleteTextView punya setText(CharSequence, boolean)
                            ((android.widget.AutoCompleteTextView) modalInputPeriode).setText(triwulan, false);
                        } catch (Exception e) {
                            // fallback ke setText biasa
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
    }

    /** Load daftar pengukuran ke spinner (memperbarui list & notify adapter) **/
    private void loadPengukuranList() {
        if (!isInternetAvailable()) {
            showElegantToast("Offline: Tidak bisa ambil data pengukuran", "warning");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(GET_PENGUKURAN_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(7000);

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                JSONArray dataArray = response.optJSONArray("data");
                if (dataArray == null) dataArray = new JSONArray();

                pengukuranMap.clear();
                List<String> newTanggalList = new ArrayList<>();
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject obj = dataArray.getJSONObject(i);
                    int id = obj.optInt("id", -1);
                    String tanggal = obj.optString("tanggal", "");
                    if (!tanggal.isEmpty()) {
                        newTanggalList.add(tanggal);
                        pengukuranMap.put(tanggal, id);
                    }
                }

                runOnUiThread(() -> {
                    tanggalList.clear();
                    tanggalList.addAll(newTanggalList);
                    if (pengukuranAdapter != null) pengukuranAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e("LOAD_PENGUKURAN", "error", e);
                runOnUiThread(() -> showElegantToast("Gagal ambil data pengukuran: " + e.getMessage(), "error"));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /** Fetch ulang daftar pengukuran setelah tambah baru */
    private void refreshPengukuranList() {
        loadPengukuranList();
    }

    /** Simpan pengukuran baru dari modal **/
    private void handleModalPengukuran() {
        if (modalInputTahun == null || modalInputBulan == null || modalInputPeriode == null || modalInputTanggal == null) {
            showElegantToast("Form modal belum siap", "error");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("mode", "pengukuran");
        data.put("tahun", modalInputTahun.getText().toString().trim());
        data.put("bulan", modalInputBulan.getText().toString().trim());
        data.put("periode", modalInputPeriode.getText().toString().trim());
        data.put("tanggal", modalInputTanggal.getText().toString().trim());

        if (data.get("tahun").isEmpty() || data.get("bulan").isEmpty() || data.get("periode").isEmpty() || data.get("tanggal").isEmpty()) {
            showElegantToast("Harap isi semua field yang wajib", "warning");
            return;
        }

        if (isInternetAvailable()) {
            sendToServer(data, "pengukuran", true);
        } else {
            // buat tempId jika belum ada
            tempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", tempId);
            saveOffline("pengukuran", tempId, data);
            hideModal();
        }
    }

    private void handleTmaWaduk() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "pengukuran");
        data.put("tma_waduk", inputTmaWaduk != null ? inputTmaWaduk.getText().toString().trim() : "");

        String selected = spinnerPengukuran != null && spinnerPengukuran.getSelectedItem() != null
                ? spinnerPengukuran.getSelectedItem().toString() : null;

        if (selected != null && pengukuranMap.containsKey(selected)) {
            pengukuranId = pengukuranMap.get(selected);
            data.put("pengukuran_id", String.valueOf(pengukuranId));
        } else {
            pengukuranId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (pengukuranId != -1) {
                data.put("pengukuran_id", String.valueOf(pengukuranId));
            } else {
                // jika belum pilih pengukuran, buat tempId dan simpan offline
                if (tempId == null) tempId = "local_" + System.currentTimeMillis();
                data.put("temp_id", tempId);
            }
        }

        if (isInternetAvailable() && data.containsKey("pengukuran_id")) {
            cekDanSimpanData("tma_waduk", data, false);
        } else {
            saveOffline("tma_waduk", data.getOrDefault("temp_id", "local_" + System.currentTimeMillis()), data);
        }
    }

    // METHOD BARU: Handle Thomson khusus untuk HP 1 (hanya A1 R, A1 L, B1)
    private void handleThomsonHP1() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "thomson");

        // HP 1 hanya mengirim A1 R, A1 L, B1
        data.put("a1_r", inputA1R != null ? inputA1R.getText().toString().trim() : "");
        data.put("a1_l", inputA1L != null ? inputA1L.getText().toString().trim() : "");
        data.put("b1", inputB1 != null ? inputB1.getText().toString().trim() : "");

        // B3 dan B5 dikosongkan (akan diisi oleh HP 2)
        data.put("b3", "");
        data.put("b5", "");

        String selected = spinnerPengukuran != null && spinnerPengukuran.getSelectedItem() != null
                ? spinnerPengukuran.getSelectedItem().toString() : null;

        if (selected != null && pengukuranMap.containsKey(selected)) {
            data.put("pengukuran_id", String.valueOf(pengukuranMap.get(selected)));
        } else {
            int prefId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (prefId != -1) data.put("pengukuran_id", String.valueOf(prefId));
            else {
                if (tempId == null) tempId = "local_" + System.currentTimeMillis();
                data.put("temp_id", tempId);
            }
        }

        if (isInternetAvailable() && data.containsKey("pengukuran_id")) cekDanSimpanData("thomson", data, false);
        else saveOffline("thomson", data.getOrDefault("temp_id", "local_" + System.currentTimeMillis()), data);
    }

    private void handleSR() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "sr");

        for (int kode : srKodeArray) {
            Spinner spinner = srKodeSpinners.get(kode);
            if (spinner != null && spinner.getSelectedItem() != null) {
                data.put("sr_" + kode + "_kode", spinner.getSelectedItem().toString());
            } else {
                data.put("sr_" + kode + "_kode", "");
            }
            data.put("sr_" + kode + "_nilai", getTextFromId("sr_" + kode + "_nilai"));
        }

        String selected = spinnerPengukuran != null && spinnerPengukuran.getSelectedItem() != null
                ? spinnerPengukuran.getSelectedItem().toString() : null;

        if (selected != null && pengukuranMap.containsKey(selected)) {
            data.put("pengukuran_id", String.valueOf(pengukuranMap.get(selected)));
        } else {
            int prefId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (prefId != -1) data.put("pengukuran_id", String.valueOf(prefId));
            else {
                if (tempId == null) tempId = "local_" + System.currentTimeMillis();
                data.put("temp_id", tempId);
            }
        }

        if (isInternetAvailable() && data.containsKey("pengukuran_id")) cekDanSimpanData("sr", data, false);
        else saveOffline("sr", data.getOrDefault("temp_id", "local_" + System.currentTimeMillis()), data);
    }

    private void handleBocoran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "bocoran");
        data.put("elv_624_t1", inputElv624T1 != null ? inputElv624T1.getText().toString().trim() : "");
        data.put("elv_624_t1_kode", (elv624T1Kode != null && elv624T1Kode.getSelectedItem() != null) ? elv624T1Kode.getSelectedItem().toString() : "");
        data.put("elv_615_t2", inputElv615T2 != null ? inputElv615T2.getText().toString().trim() : "");
        data.put("elv_615_t2_kode", (elv615T2Kode != null && elv615T2Kode.getSelectedItem() != null) ? elv615T2Kode.getSelectedItem().toString() : "");
        data.put("pipa_p1", inputPipaP1 != null ? inputPipaP1.getText().toString().trim() : "");
        data.put("pipa_p1_kode", (pipaP1Kode != null && pipaP1Kode.getSelectedItem() != null) ? pipaP1Kode.getSelectedItem().toString() : "");

        String selected = spinnerPengukuran != null && spinnerPengukuran.getSelectedItem() != null
                ? spinnerPengukuran.getSelectedItem().toString() : null;
        if (selected != null && pengukuranMap.containsKey(selected)) {
            data.put("pengukuran_id", String.valueOf(pengukuranMap.get(selected)));
        } else {
            int prefId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (prefId != -1) data.put("pengukuran_id", String.valueOf(prefId));
            else {
                if (tempId == null) tempId = "local_" + System.currentTimeMillis();
                data.put("temp_id", tempId);
            }
        }

        if (isInternetAvailable() && data.containsKey("pengukuran_id")) cekDanSimpanData("bocoran", data, false);
        else saveOffline("bocoran", data.getOrDefault("temp_id", "local_" + System.currentTimeMillis()), data);
    }

    /* ---------- Hitung semua ---------- */

    private void handleHitungSemua() {
        if (!isInternetAvailable()) {
            showElegantToast("Tidak ada koneksi internet. Tidak dapat menghitung data.", "error");
            return;
        }

        String sel = spinnerPengukuran.getSelectedItem() != null ? spinnerPengukuran.getSelectedItem().toString() : null;
        int id = -1;
        if (sel != null && pengukuranMap.containsKey(sel)) {
            id = pengukuranMap.get(sel);
        } else {
            SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
            id = prefs.getInt("pengukuran_id", -1);
        }

        if (id == -1) {
            showElegantToast("Pilih data pengukuran terlebih dahulu!", "warning");
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
                conn.setConnectTimeout(300_000);
                conn.setReadTimeout(300_000);
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
                runOnUiThread(() -> {
                    pd.dismiss();
                    try {
                        String status = resp.optString("status", "");
                        JSONObject messages = resp.optJSONObject("messages");
                        JSONObject data = resp.optJSONObject("data");
                        String tanggal = resp.optString("tanggal", "-");

                        // 🔹 Ringkasan pesan perhitungan
                        StringBuilder msgBuilder = new StringBuilder();
                        if (messages != null) {
                            Iterator<String> keys = messages.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                String value = messages.optString(key, "");
                                msgBuilder.append("• ").append(key).append(": ").append(value).append("\n");
                            }
                        }

                        // 🔹 Hasil Look Burt
                        String lookBurtInfo = "";
                        String statusKeterangan = "aman"; // default
                        if (data != null) {
                            String rembBendungan = data.optString("rembesan_bendungan", "-");
                            String rembPerM = data.optString("rembesan_per_m", "-");
                            String ket = data.optString("keterangan", "-");
                            lookBurtInfo = "\n💧 Analisa Look Burt:\n"
                                    + "  - Rembesan Bendungan: " + rembBendungan + "\n"
                                    + "  - Rembesan per M: " + rembPerM + "\n"
                                    + "  - Keterangan: " + ket;

                            // Tentukan status berdasarkan keterangan
                            if (ket.toLowerCase().contains("bahaya")) {
                                statusKeterangan = "danger";
                            } else if (ket.toLowerCase().contains("peringatan") || ket.toLowerCase().contains("waspada")) {
                                statusKeterangan = "warning";
                            } else {
                                statusKeterangan = "success";
                            }
                        }

                        // 🔹 Tentukan notifikasi akhir
                        if ("success".equalsIgnoreCase(status)) {
                            showCalculationResultDialog(" Perhitungan Berhasil",
                                    "Semua perhitungan berhasil untuk tanggal " + tanggal + lookBurtInfo,
                                    statusKeterangan, tanggal);
                        } else if ("partial_error".equalsIgnoreCase(status)) {
                            showCalculationResultDialog("⚠️ Perhitungan Sebagian Berhasil",
                                    "Beberapa perhitungan gagal:\n\n" + msgBuilder.toString() + lookBurtInfo,
                                    "warning", tanggal);
                        } else if ("error".equalsIgnoreCase(status)) {
                            showElegantToast("❌ Gagal menghitung: " + resp.optString("message", "Terjadi kesalahan"), "error");
                        } else {
                            showElegantToast("ℹ️ Respon tidak dikenal dari server", "info");
                        }

                    } catch (Exception e) {
                        showElegantToast("Error parsing hasil: " + e.getMessage(), "error");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    showElegantToast("Error saat menghitung: " + e.getMessage(), "error");
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void showCalculationResultDialog(String title, String message, String status, String tanggal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate custom layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_calculation_result, null);
        builder.setView(dialogView);

        // Initialize views
        TextView titleText = dialogView.findViewById(R.id.dialog_title);
        TextView messageText = dialogView.findViewById(R.id.dialog_message);
        TextView tanggalText = dialogView.findViewById(R.id.dialog_tanggal);
        ImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        Button okButton = dialogView.findViewById(R.id.dialog_button_ok);
        LinearLayout headerLayout = dialogView.findViewById(R.id.dialog_header);

        // Set data berdasarkan status
        int colorRes = getColorForStatus(status);
        int iconRes = getIconForStatus(status);

        titleText.setText(title);
        messageText.setText(message);
        tanggalText.setText("📅 Tanggal: " + tanggal);
        iconView.setImageResource(iconRes);

        // 🔥 PERBAIKAN UI: Gradient background untuk header
        headerLayout.setBackgroundColor(ContextCompat.getColor(this, colorRes));

        // 🔥 PERBAIKAN UI: Tambahkan elevation/shadow
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            headerLayout.setElevation(8f);
            okButton.setElevation(4f);
        }

        // 🔥 PERBAIKAN UI: Animasi icon
        iconView.setAlpha(0f);
        iconView.animate().alpha(1f).setDuration(500).start();

        // 🔥 PERBAIKAN UI: Style button dengan ripple effect
        okButton.setBackgroundColor(ContextCompat.getColor(this, colorRes));
        okButton.setTextColor(Color.WHITE);

        // 🔥 PERBAIKAN UI: Format message dengan styling
        String formattedMessage = formatMessageWithIcons(message);
        messageText.setText(formattedMessage);

        // 🔥 PERBAIKAN UI: Animasi dialog masuk
        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.8f);
        dialogView.setScaleY(0.8f);

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // Tampilkan dialog dulu baru animasi
        dialog.show();

        // Animasi dialog masuk
        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();

        okButton.setOnClickListener(v -> {
            // 🔥 PERBAIKAN UI: Animasi tombol ketika ditekan
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }).start();

            // 🔥 PERBAIKAN UI: Animasi dialog keluar
            dialogView.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(200)
                    .withEndAction(dialog::dismiss)
                    .start();
        });
    }

    // 🔥 METHOD BARU: Format pesan dengan icon dan styling
    private String formatMessageWithIcons(String message) {
        // Ganti keyword dengan icon
        String formatted = message
                .replace("Analisa Look Burt", "🔍 Analisa Look Burt")
                .replace("Rembesan Bendungan", "💧 Rembesan Bendungan")
                .replace("Rembesan per M", "📏 Rembesan per M")
                .replace("Keterangan:", "📋 Keterangan:")
                .replace("Berhasil", "✅ Berhasil")
                .replace("Gagal", "❌ Gagal")
                .replace("Aman", "🟢 Aman")
                .replace("Peringatan", "🟡 Peringatan")
                .replace("Bahaya", "🔴 Bahaya");

        return formatted;
    }

    // Method untuk toast elegan dengan improvement
    private void showElegantToast(String message, String type) {
        runOnUiThread(() -> {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast_custom,
                    (android.view.ViewGroup) findViewById(R.id.custom_toast_container));

            TextView text = layout.findViewById(R.id.custom_toast_text);
            ImageView icon = layout.findViewById(R.id.custom_toast_icon);
            CardView card = layout.findViewById(R.id.custom_toast_card);

            // 🔥 PERBAIKAN: Format pesan toast
            String formattedMessage = formatMessageWithIcons(message);
            text.setText(formattedMessage);

            // Set warna dan icon berdasarkan type
            int colorRes = getColorForStatus(type);
            int iconRes = getIconForStatus(type);

            card.setCardBackgroundColor(ContextCompat.getColor(this, colorRes));
            icon.setImageResource(iconRes);

            // 🔥 PERBAIKAN: Animasi toast
            card.setAlpha(0f);
            card.setScaleX(0.8f);
            card.setScaleY(0.8f);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);

            toast.show();

            // Animasi toast masuk
            card.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start();
        });
    }

    // Helper method untuk mendapatkan warna berdasarkan status
    private int getColorForStatus(String status) {
        switch (status.toLowerCase()) {
            case "success":
            case "aman":
                return R.color.pln_success; // Hijau PLN
            case "warning":
            case "peringatan":
                return R.color.pln_warning; // Kuning/Oranye
            case "error":
            case "danger":
            case "bahaya":
                return R.color.pln_danger; // Merah
            case "info":
            default:
                return R.color.pln_info; // Biru PLN
        }
    }

    // Helper method untuk mendapatkan icon berdasarkan status
    private int getIconForStatus(String status) {
        switch (status.toLowerCase()) {
            case "success":
            case "aman":
                return R.drawable.ic_success;
            case "warning":
            case "peringatan":
                return R.drawable.ic_warning;
            case "error":
            case "danger":
            case "bahaya":
                return R.drawable.ic_danger;
            case "info":
            default:
                return R.drawable.ic_info;
        }
    }

    private void cekDanSimpanData(String table, Map<String, String> dataMap, boolean isPengukuran) {
        if (isPengukuran) {
            sendToServer(dataMap, table, isPengukuran);
            return;
        }

        new Thread(() -> {
            HttpURLConnection connCek = null;
            try {
                String pengukuranIdParam = dataMap.get("pengukuran_id");
                if (pengukuranIdParam == null) {
                    // langsung simpan jika tidak ada pengukuran_id (mungkin offline)
                    sendToServer(dataMap, table, isPengukuran);
                    return;
                }

                URL urlCek = new URL(CEK_DATA_URL + "?pengukuran_id=" + URLEncoder.encode(pengukuranIdParam, "UTF-8"));
                connCek = (HttpURLConnection) urlCek.openConnection();
                connCek.setRequestMethod("GET");
                connCek.setRequestProperty("Accept", "application/json");
                connCek.setConnectTimeout(7000);
                connCek.setReadTimeout(7000);

                int code = connCek.getResponseCode();
                InputStream is = (code == 200) ? connCek.getInputStream() : connCek.getErrorStream();
                BufferedReader readerCek = new BufferedReader(new InputStreamReader(is));
                StringBuilder sbCek = new StringBuilder();
                String lineCek;
                while ((lineCek = readerCek.readLine()) != null) sbCek.append(lineCek);
                readerCek.close();

                JSONObject responseCek = new JSONObject(sbCek.toString());
                JSONObject data = responseCek.has("data") ? responseCek.getJSONObject("data") : responseCek;

                boolean dataSudahAda = false;
                boolean dataLengkap = false;

                switch (table) {
                    case "tma_waduk":
                    case "pengukuran":
                        dataSudahAda = data.optBoolean("tma_waduk_ada", false);
                        break;
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
                        sendToServer(dataMap, table, isPengukuran);
                    } else {
                        runOnUiThread(() -> showElegantToast("Data " + table + " sudah lengkap untuk pengukuran ini!", "info"));
                    }
                    return;
                }

                sendToServer(dataMap, table, isPengukuran);

            } catch (Exception e) {
                Log.e("CEK_SAVE", "error", e);
                runOnUiThread(() -> showElegantToast("Gagal cek data, mencoba simpan langsung...", "warning"));
                sendToServer(dataMap, table, isPengukuran);
            } finally {
                if (connCek != null) connCek.disconnect();
            }
        }).start();
    }

    private void syncAllOfflineData(Runnable onComplete) {
        // gunakan hasUnsyncedData agar lebih efisien
        boolean adaData = offlineDb.hasUnsyncedData();

        if (!adaData) {
            if (onComplete != null) onComplete.run();
            return;
        }

        syncDataSerial("pengukuran", () ->
                syncDataSerial("tma_waduk", () ->
                        syncDataSerial("thomson", () ->
                                syncDataSerial("sr", () ->
                                        syncDataSerial("bocoran", onComplete)
                                )
                        )
                )
        );
    }

    private void syncDataSerial(String tableName, Runnable next) {
        // gunakan hanya unsynced data
        List<Map<String, String>> dataList = offlineDb.getUnsyncedData(tableName);
        if (dataList.isEmpty()) {
            if (next != null) next.run();
            return;
        }
        syncDataItem(tableName, dataList, 0, next);
    }

    private void syncDataItem(String tableName, List<Map<String, String>> dataList, int index, Runnable onFinish) {
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
                    InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    if (responseCode == 200) {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if ("success".equalsIgnoreCase(jsonResponse.optString("status"))) {
                            offlineDb.deleteByTempId(tableName, tempId);
                            Log.d("SYNC", "Data " + tableName + " tempId=" + tempId + " berhasil disinkronisasi");
                        } else {
                            Log.e("SYNC", "Gagal sinkron data " + tableName + " tempId=" + tempId + ": " + jsonResponse.optString("message"));
                        }
                    } else {
                        Log.e("SYNC", "Response error code " + responseCode + " untuk data " + tableName + " tempId=" + tempId);
                    }

                } catch (Exception e) {
                    Log.e("SYNC", "Error sync " + tableName + " tempId=" + tempId, e);
                } finally {
                    if (conn != null) conn.disconnect();
                }

                runOnUiThread(() -> syncDataItem(tableName, dataList, index + 1, onFinish));
            }).start();

        } catch (Exception e) {
            Log.e("SYNC", "JSON parse error untuk data " + tableName + " tempId=" + tempId, e);
            runOnUiThread(() -> syncDataItem(tableName, dataList, index + 1, onFinish));
        }
    }

    private void sendToServer(Map<String, String> dataMap, String table, boolean isPengukuran) {
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

                JSONObject jsonData = new JSONObject();
                for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                    jsonData.put(entry.getKey(), entry.getValue());
                }

                OutputStream os = conn.getOutputStream();
                os.write(jsonData.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                if (isPengukuran && response.has("pengukuran_id")) {
                    pengukuranId = response.optInt("pengukuran_id", -1);
                    SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
                    prefs.edit().putInt("pengukuran_id", pengukuranId).apply();
                    tempId = null;
                }

                runOnUiThread(() -> {
                    if ("success".equalsIgnoreCase(status)) {
                        showElegantToast(message, "success");
                        if (isPengukuran) {
                            hideModal();
                            refreshPengukuranList();
                        }
                    } else {
                        showElegantToast("Error: " + message, "error");
                    }
                });

            } catch (Exception e) {
                Log.e("SEND", "error", e);
                // jika gagal dikarenakan koneksi atau server, simpan offline agar aman
                runOnUiThread(() -> showElegantToast("Gagal kirim: " + e.getMessage() + ". Data akan disimpan offline.", "warning"));
                try {
                    // buat tempId jika belum ada
                    if (tempId == null) tempId = "local_" + System.currentTimeMillis();
                    saveOffline(table, tempId, dataMap);
                } catch (Exception ex) {
                    Log.e("SEND_SAVE_OFFLINE", "Gagal simpan offline", ex);
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void saveOffline(String table, String tempId, Map<String, String> data) {
        try {
            if (tempId == null) tempId = "local_" + System.currentTimeMillis();
            JSONObject json = new JSONObject(data);
            offlineDb.insertData(table, tempId, json.toString());
            showElegantToast("Tidak ada internet. Data disimpan offline.", "warning");
            syncPrefs.edit().putBoolean("toast_shown", false).apply();
        } catch (Exception e) {
            Log.e("SAVE_OFFLINE", "error", e);
            showElegantToast("Gagal simpan offline: " + e.getMessage(), "error");
        }
    }

    private String getTextFromId(String idName) {
        try {
            int id = getResources().getIdentifier(idName, "id", getPackageName());
            if (id == 0) return "";
            EditText et = findViewById(id);
            return et != null ? et.getText().toString() : "";
        } catch (Exception e) {
            return "";
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

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    private void syncPengukuranMaster() {
        if (!isInternetAvailable()) {
            showElegantToast("Tidak ada koneksi internet. Sinkronisasi gagal.", "warning");
            return;
        }

        Calendar cal = Calendar.getInstance();
        int bulanSekarang = cal.get(Calendar.MONTH) + 1;
        int tahunSekarang = cal.get(Calendar.YEAR);

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(GET_PENGUKURAN_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(7000);

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                JSONArray dataArray = response.optJSONArray("data");
                if (dataArray == null) dataArray = new JSONArray();

                pengukuranMap.clear();
                List<String> bulanIniList = new ArrayList<>();

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject obj = dataArray.getJSONObject(i);
                    String tanggal = obj.optString("tanggal", "");
                    int id = obj.optInt("id", -1);
                    if (tanggal.isEmpty()) continue;

                    String[] parts = tanggal.split("-");
                    if (parts.length < 2) continue;
                    int tahun = Integer.parseInt(parts[0]);
                    int bulan = Integer.parseInt(parts[1]);

                    if (tahun == tahunSekarang && bulan == bulanSekarang) {
                        bulanIniList.add(tanggal);
                        pengukuranMap.put(tanggal, id);
                    }
                }

                runOnUiThread(() -> {
                    tanggalList.clear();
                    tanggalList.addAll(bulanIniList);
                    if (pengukuranAdapter != null) pengukuranAdapter.notifyDataSetChanged();

                    if (tanggalList.isEmpty()) {
                        showElegantToast("Tidak ada data pengukuran untuk bulan ini.", "info");
                    } else {
                        spinnerPengukuran.setSelection(0);
                        pengukuranId = pengukuranMap.get(tanggalList.get(0));
                        getSharedPreferences("pengukuran", MODE_PRIVATE)
                                .edit().putInt("pengukuran_id", pengukuranId).apply();
                    }
                });

            } catch (Exception e) {
                Log.e("SYNC_MASTER", "Error syncPengukuranMaster", e);
                runOnUiThread(() -> showElegantToast("Sinkronisasi gagal: " + e.getMessage(), "error"));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}