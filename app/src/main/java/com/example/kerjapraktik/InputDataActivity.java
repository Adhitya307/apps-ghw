package com.example.kerjapraktik;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
    private static final String BASE_URL = "http://192.168.1.28/API_Android/public/rembesan/";
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
                showToast("Pilih tanggal pengukuran dulu.");
                return;
            }

            String selected = sel.toString();
            if (pengukuranMap.containsKey(selected)) {
                pengukuranId = pengukuranMap.get(selected);
                getSharedPreferences("pengukuran", MODE_PRIVATE)
                        .edit()
                        .putInt("pengukuran_id", pengukuranId)
                        .apply();
                showToast("ID terpilih: " + pengukuranId);
            } else {
                showToast("Tanggal tidak dikenali, coba sinkron ulang.");
            }
        });

        // set click listeners (pastikan sudah di-init di initFormComponents)
        if (btnSubmitTmaWaduk != null) btnSubmitTmaWaduk.setOnClickListener(v -> handleTmaWaduk());
        if (btnSubmitThomson != null) btnSubmitThomson.setOnClickListener(v -> handleThomson());
        if (btnSubmitSR != null) btnSubmitSR.setOnClickListener(v -> handleSR());
        if (btnSubmitBocoran != null) btnSubmitBocoran.setOnClickListener(v -> handleBocoran());
        if (btnHitungSemua != null) btnHitungSemua.setOnClickListener(v -> handleHitungSemua());

        // tampilkan modal jika komponen ada
        if (modalPengukuran != null && modalOverlay != null && mainContent != null) {
            showModal();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // jalankan sinkronisasi offline -> online saat internet tersedia
        if (isInternetAvailable()) {
            // gunakan hasUnsyncedData untuk cek lebih aman
            if (offlineDb.hasUnsyncedData()) {
                syncAllOfflineData(() -> showToast("Sinkronisasi data offline selesai"));
            } else {
                // jika tidak ada data unsynced, masih bisa update master
                syncPengukuranMaster();
            }
        }
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
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    if (modalInputTanggal != null) modalInputTanggal.setText(dateFormat.format(calendar.getTime()));
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
            showToast("Offline: Tidak bisa ambil data pengukuran");
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
                runOnUiThread(() -> showToast("Gagal ambil data pengukuran: " + e.getMessage()));
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
            showToast("Form modal belum siap");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("mode", "pengukuran");
        data.put("tahun", modalInputTahun.getText().toString().trim());
        data.put("bulan", modalInputBulan.getText().toString().trim());
        data.put("periode", modalInputPeriode.getText().toString().trim());
        data.put("tanggal", modalInputTanggal.getText().toString().trim());

        if (data.get("tahun").isEmpty() || data.get("bulan").isEmpty() || data.get("periode").isEmpty() || data.get("tanggal").isEmpty()) {
            showToast("Harap isi semua field yang wajib");
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

    private void handleThomson() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "thomson");
        data.put("a1_r", inputA1R != null ? inputA1R.getText().toString().trim() : "");
        data.put("a1_l", inputA1L != null ? inputA1L.getText().toString().trim() : "");
        data.put("b1", inputB1 != null ? inputB1.getText().toString().trim() : "");
        data.put("b3", inputB3 != null ? inputB3.getText().toString().trim() : "");
        data.put("b5", inputB5 != null ? inputB5.getText().toString().trim() : "");

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

    private void handleHitungSemua() {
        if (!isInternetAvailable()) {
            showToast("Tidak ada koneksi internet. Tidak dapat menghitung data.");
            return;
        }

        String selected = spinnerPengukuran != null && spinnerPengukuran.getSelectedItem() != null
                ? spinnerPengukuran.getSelectedItem().toString() : null;
        int selectedPengukuranId = -1;

        if (selected != null && pengukuranMap.containsKey(selected)) {
            selectedPengukuranId = pengukuranMap.get(selected);
        } else {
            selectedPengukuranId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (selectedPengukuranId == -1) {
                showToast("Pilih data pengukuran terlebih dahulu!");
                return;
            }
        }

        final int finalPengukuranId = selectedPengukuranId;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Menghitung data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(HITUNG_SEMUA_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(9000);
                conn.setReadTimeout(9000);

                JSONObject jsonData = new JSONObject();
                jsonData.put("pengukuran_id", finalPengukuranId);

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
                JSONObject data = response.optJSONObject("data");

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (data != null) {
                        String statusServer = data.optString("status", "partial");
                        String messageServer = data.optString("message", "");
                        if ("success".equals(statusServer) || "partial".equals(statusServer)) {
                            showToast("Perhitungan selesai: " + messageServer);
                            JSONObject batasmaksimal = data.optJSONObject("batasmaksimal");
                            if (batasmaksimal != null) {
                                double tmaWaduk = batasmaksimal.optDouble("tma_waduk", -1);
                                double batasMaksimal = batasmaksimal.optDouble("batas_maksimal", -1);
                                String hasil = "";
                                if (tmaWaduk >= 0) hasil += "TMA Waduk: " + tmaWaduk;
                                if (batasMaksimal >= 0) {
                                    if (!hasil.isEmpty()) hasil += ", ";
                                    hasil += "Batas Maksimal: " + batasMaksimal;
                                }
                                if (!hasil.isEmpty()) showToast(hasil);
                            }
                        } else {
                            showToast("Gagal menghitung: " + messageServer);
                        }
                    } else {
                        showToast("Perhitungan selesai, silakan cek web.");
                    }
                });

            } catch (Exception e) {
                Log.e("HITUNG_SEMUA", "Error", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showToast("Error: " + e.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
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
                        runOnUiThread(() -> showToast("Data " + table + " sudah lengkap untuk pengukuran ini!"));
                    }
                    return;
                }

                sendToServer(dataMap, table, isPengukuran);

            } catch (Exception e) {
                Log.e("CEK_SAVE", "error", e);
                runOnUiThread(() -> showToast("Gagal cek data, mencoba simpan langsung..."));
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
                        showToast(message);
                        if (isPengukuran) {
                            hideModal();
                            Toast.makeText(InputDataActivity.this, "Data pengukuran tersimpan", Toast.LENGTH_SHORT).show();
                            refreshPengukuranList();
                        }
                    } else {
                        showToast("Error: " + message);
                    }
                });

            } catch (Exception e) {
                Log.e("SEND", "error", e);
                // jika gagal dikarenakan koneksi atau server, simpan offline agar aman
                runOnUiThread(() -> showToast("Gagal kirim: " + e.getMessage() + ". Data akan disimpan offline."));
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
            showToast("Tidak ada internet. Data disimpan offline.");
            syncPrefs.edit().putBoolean("toast_shown", false).apply();
        } catch (Exception e) {
            Log.e("SAVE_OFFLINE", "error", e);
            showToast("Gagal simpan offline: " + e.getMessage());
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
            showToast("Tidak ada koneksi internet. Sinkronisasi gagal.");
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
                        showToast("Tidak ada data pengukuran untuk bulan ini.");
                    } else {
                        spinnerPengukuran.setSelection(0);
                        pengukuranId = pengukuranMap.get(tanggalList.get(0));
                        getSharedPreferences("pengukuran", MODE_PRIVATE)
                                .edit().putInt("pengukuran_id", pengukuranId).apply();
                    }
                });

            } catch (Exception e) {
                Log.e("SYNC_MASTER", "Error syncPengukuranMaster", e);
                runOnUiThread(() -> showToast("Sinkronisasi gagal: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}
