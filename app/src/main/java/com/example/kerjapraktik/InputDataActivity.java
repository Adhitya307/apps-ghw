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
    private static final String BASE_URL = "http://192.168.1.7/API_Android/public/rembesan/";
    private static final String INSERT_DATA_URL = BASE_URL + "input";
    private static final String CEK_DATA_URL = BASE_URL + "cek-data";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "get_pengukuran";
    private static final String HITUNG_SEMUA_URL = "http://192.168.1.7/API_Android/public/rembesan/Rumus-Rembesan";

    // Map untuk simpan pasangan tanggal → ID
    private final Map<String, Integer> pengukuranMap = new HashMap<>();

    // simpan list & adapter agar bisa refresh + notify
    private final List<String> tanggalList = new ArrayList<>();
    private ArrayAdapter<String> pengukuranAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data);

        offlineDb = new OfflineDataHelper(this);
        syncPrefs = getSharedPreferences("sync_prefs", MODE_PRIVATE);
        calendar = Calendar.getInstance();

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);

        initModalComponents();
        initFormComponents();
        setupSpinners();
        setupModalDropdowns();
        setupModalCalendar();

        // Ambil pengukuran_id dari SharedPreferences
        SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
        pengukuranId = prefs.getInt("pengukuran_id", -1);

        // Siapkan adapter spinner (awal kosong, nanti diisi saat load)
        pengukuranAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        pengukuranAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(pengukuranAdapter);

        // Saat user memilih item di spinner, simpan selection secara silent (tanpa toast)
        spinnerPengukuran.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean userSelect = false;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Kita tidak immediately show toast; hanya simpan pengukuranId
                String selected = position >= 0 && position < tanggalList.size() ? tanggalList.get(position) : null;
                if (selected != null && pengukuranMap.containsKey(selected)) {
                    pengukuranId = pengukuranMap.get(selected);
                    // update prefs silently
                    getSharedPreferences("pengukuran", MODE_PRIVATE).edit().putInt("pengukuran_id", pengukuranId).apply();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load data pengukuran untuk spinner
        loadPengukuranList();

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


        // Submit data
        btnSubmitTmaWaduk.setOnClickListener(v -> handleTmaWaduk());
        btnSubmitThomson.setOnClickListener(v -> handleThomson());
        btnSubmitSR.setOnClickListener(v -> handleSR());
        btnSubmitBocoran.setOnClickListener(v -> handleBocoran());

        // Hitung semua data
        btnHitungSemua.setOnClickListener(v -> handleHitungSemua());

        // Tampilkan modal pertama kali (jika mau paksa input pengukuran baru)
        showModal();
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

        btnCloseModal.setOnClickListener(v -> hideModal());
        modalOverlay.setOnClickListener(v -> hideModal());
        modalBtnSubmitPengukuran.setOnClickListener(v -> handleModalPengukuran());
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

        // Inisialisasi tombol Hitung Semua Data
        btnHitungSemua = findViewById(R.id.btnHitungSemua);

        elv624T1Kode = findViewById(R.id.elv_624_t1_kode);
        elv615T2Kode = findViewById(R.id.elv_615_t2_kode);
        pipaP1Kode = findViewById(R.id.pipa_p1_kode);

        for (int kode : srKodeArray) {
            int resId = getResources().getIdentifier("sr_" + kode + "_kode", "id", getPackageName());
            Spinner spinner = findViewById(resId);
            if (spinner != null) {
                srKodeSpinners.put(kode, spinner);
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
        String[] bulanArray = getResources().getStringArray(R.array.bulan_options);
        ArrayAdapter<String> bulanAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bulanArray);
        modalInputBulan.setAdapter(bulanAdapter);
        modalInputBulan.setOnClickListener(v -> modalInputBulan.showDropDown());

        String[] periodeArray = getResources().getStringArray(R.array.periode_options);
        ArrayAdapter<String> periodeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, periodeArray);
        modalInputPeriode.setAdapter(periodeAdapter);
        modalInputPeriode.setOnClickListener(v -> modalInputPeriode.showDropDown());
    }

    private void setupModalCalendar() {
        modalInputTanggal.setOnClickListener(v -> showModalDatePickerDialog());
        // safe get parent layout
        try {
            TextInputLayout tanggalLayout = (TextInputLayout) modalInputTanggal.getParent().getParent();
            if (tanggalLayout != null) {
                tanggalLayout.setEndIconOnClickListener(v -> showModalDatePickerDialog());
            }
        } catch (Exception ignored) {}
    }

    private void showModalDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    modalInputTanggal.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showModal() {
        modalPengukuran.setVisibility(View.VISIBLE);
        modalOverlay.setVisibility(View.VISIBLE);
        mainContent.setAlpha(0.5f);
        mainContent.setEnabled(false);
    }

    private void hideModal() {
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
            try {
                URL url = new URL(GET_PENGUKURAN_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                JSONArray dataArray = response.getJSONArray("data");

                // refresh map & list
                pengukuranMap.clear();
                List<String> newTanggalList = new ArrayList<>();
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject obj = dataArray.getJSONObject(i);
                    int id = obj.getInt("id");
                    String tanggal = obj.getString("tanggal");
                    newTanggalList.add(tanggal);
                    pengukuranMap.put(tanggal, id);
                }

                runOnUiThread(() -> {
                    tanggalList.clear();
                    tanggalList.addAll(newTanggalList);
                    pengukuranAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e("LOAD_PENGUKURAN", "error", e);
                runOnUiThread(() -> showToast("Gagal ambil data pengukuran: " + e.getMessage()));
            }
        }).start();
    }

    /** Fetch ulang daftar pengukuran setelah tambah baru */
    private void refreshPengukuranList() {
        if (!isInternetAvailable()) return;

        new Thread(() -> {
            try {
                URL url = new URL(GET_PENGUKURAN_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                JSONArray dataArray = response.getJSONArray("data");

                pengukuranMap.clear();
                List<String> newTanggalList = new ArrayList<>();
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject obj = dataArray.getJSONObject(i);
                    int id = obj.getInt("id");
                    String tanggal = obj.getString("tanggal");
                    newTanggalList.add(tanggal);
                    pengukuranMap.put(tanggal, id);
                }

                runOnUiThread(() -> {
                    tanggalList.clear();
                    tanggalList.addAll(newTanggalList);
                    pengukuranAdapter.notifyDataSetChanged();
                    // Tidak auto-select, biarkan user pilih manual
                });

            } catch (Exception e) {
                Log.e("REFRESH_PENGUKURAN", "error", e);
                runOnUiThread(() -> showToast("Gagal refresh pengukuran: " + e.getMessage()));
            }
        }).start();
    }

    /** Simpan pengukuran baru dari modal **/
    private void handleModalPengukuran() {
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
            sendToServer(data, "pengukuran", true); // di sini nanti auto-refresh spinner & pilih tanggal baru
        } else {
            tempId = "local_" + System.currentTimeMillis();
            data.put("temp_id", tempId);
            saveOffline("pengukuran", tempId, data);
            hideModal();
            showToast("Data pengukuran tersimpan offline");
        }
    }

    private void handleTmaWaduk() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "pengukuran");
        data.put("tma_waduk", inputTmaWaduk.getText().toString().trim());

        // Prefer gunakan pengukuran yang dipilih di spinner (user flow)
        String selected = spinnerPengukuran.getSelectedItem() != null ? spinnerPengukuran.getSelectedItem().toString() : null;
        if (selected != null && pengukuranMap.containsKey(selected)) {
            pengukuranId = pengukuranMap.get(selected);
            data.put("pengukuran_id", String.valueOf(pengukuranId));
        } else {
            // fallback: cek prefs / tempId
            pengukuranId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (pengukuranId != -1) {
                data.put("pengukuran_id", String.valueOf(pengukuranId));
            } else if (tempId != null) {
                data.put("temp_id", tempId);
            } else {
                showToast("Isi data pengukuran terlebih dahulu (pilih tanggal dari dropdown)!");
                return;
            }
        }

        if (isInternetAvailable() && data.containsKey("pengukuran_id")) cekDanSimpanData("pengukuran", data, false);
        else saveOffline("pengukuran", tempId, data);
    }

    private void handleThomson() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "thomson");
        data.put("a1_r", inputA1R.getText().toString().trim());
        data.put("a1_l", inputA1L.getText().toString().trim());
        data.put("b1", inputB1.getText().toString().trim());
        data.put("b3", inputB3.getText().toString().trim());
        data.put("b5", inputB5.getText().toString().trim());

        // same selection logic as TMA
        String selected = spinnerPengukuran.getSelectedItem() != null ? spinnerPengukuran.getSelectedItem().toString() : null;
        if (selected != null && pengukuranMap.containsKey(selected)) {
            data.put("pengukuran_id", String.valueOf(pengukuranMap.get(selected)));
        } else {
            int prefId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (prefId != -1) data.put("pengukuran_id", String.valueOf(prefId));
            else if (tempId != null) data.put("temp_id", tempId);
            else { showToast("Isi data pengukuran terlebih dahulu!"); return; }
        }

        if (isInternetAvailable() && data.containsKey("pengukuran_id")) cekDanSimpanData("thomson", data, false);
        else saveOffline("thomson", tempId, data);
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

        // same selection logic as Thomson
        String selected = spinnerPengukuran.getSelectedItem() != null ? spinnerPengukuran.getSelectedItem().toString() : null;
        if (selected != null && pengukuranMap.containsKey(selected)) {
            data.put("pengukuran_id", String.valueOf(pengukuranMap.get(selected)));
        } else {
            int prefId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (prefId != -1) data.put("pengukuran_id", String.valueOf(prefId));
            else if (tempId != null) data.put("temp_id", tempId);
            else { showToast("Isi data pengukuran terlebih dahulu!"); return; }
        }

        if (isInternetAvailable() && data.containsKey("pengukuran_id")) cekDanSimpanData("sr", data, false);
        else saveOffline("sr", tempId, data);
    }

    private void handleBocoran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "bocoran");
        data.put("elv_624_t1", inputElv624T1.getText().toString().trim());
        data.put("elv_624_t1_kode", elv624T1Kode.getSelectedItem().toString());
        data.put("elv_615_t2", inputElv615T2.getText().toString().trim());
        data.put("elv_615_t2_kode", elv615T2Kode.getSelectedItem().toString());
        data.put("pipa_p1", inputPipaP1.getText().toString().trim());
        data.put("pipa_p1_kode", pipaP1Kode.getSelectedItem().toString());

        // same selection logic
        String selected = spinnerPengukuran.getSelectedItem() != null ? spinnerPengukuran.getSelectedItem().toString() : null;
        if (selected != null && pengukuranMap.containsKey(selected)) {
            data.put("pengukuran_id", String.valueOf(pengukuranMap.get(selected)));
        } else {
            int prefId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (prefId != -1) data.put("pengukuran_id", String.valueOf(prefId));
            else if (tempId != null) data.put("temp_id", tempId);
            else { showToast("Isi data pengukuran terlebih dahulu!"); return; }
        }

        if (isInternetAvailable() && data.containsKey("pengukuran_id")) cekDanSimpanData("bocoran", data, false);
        else saveOffline("bocoran", tempId, data);
    }


    private void handleHitungSemua() {
        // Pastikan ada koneksi internet
        if (!isInternetAvailable()) {
            showToast("Tidak ada koneksi internet. Tidak dapat menghitung data.");
            return;
        }

        // Dapatkan pengukuran_id yang dipilih
        String selected = spinnerPengukuran.getSelectedItem() != null ? spinnerPengukuran.getSelectedItem().toString() : null;
        int selectedPengukuranId = -1;

        if (selected != null && pengukuranMap.containsKey(selected)) {
            selectedPengukuranId = pengukuranMap.get(selected);
        } else {
            // fallback: cek prefs
            selectedPengukuranId = getSharedPreferences("pengukuran", MODE_PRIVATE).getInt("pengukuran_id", -1);
            if (selectedPengukuranId == -1) {
                showToast("Pilih data pengukuran terlebih dahulu!");
                return;
            }
        }

        // BUAT VARIABLE FINAL COPY untuk digunakan dalam lambda
        final int finalPengukuranId = selectedPengukuranId;

        // Tampilkan progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Menghitung data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                // Buat koneksi ke server
                URL url = new URL(HITUNG_SEMUA_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                // Buat data JSON - GUNAKAN VARIABLE FINAL
                JSONObject jsonData = new JSONObject();
                jsonData.put("pengukuran_id", finalPengukuranId);

                // Kirim data
                OutputStream os = conn.getOutputStream();
                os.write(jsonData.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                // Dapatkan respons
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

                // Parse response JSON
                JSONObject response = new JSONObject(sb.toString());
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                // Ambil data detail jika ada
                JSONObject data = response.optJSONObject("data");
                if (data != null) {
                    // Anda bisa mengekstrak detail perhitungan di sini jika diperlukan
                    JSONObject thomson = data.optJSONObject("thomson");
                    JSONObject sr = data.optJSONObject("sr");
                    JSONObject bocoran = data.optJSONObject("bocoran");
                    JSONObject batasmaksimal = data.optJSONObject("batasmaksimal");

                    // Log detail untuk debugging
                    Log.d("HITUNG_SEMUA", "Thomson: " + (thomson != null ? thomson.toString() : "null"));
                    Log.d("HITUNG_SEMUA", "SR: " + (sr != null ? sr.toString() : "null"));
                    Log.d("HITUNG_SEMUA", "Bocoran: " + (bocoran != null ? bocoran.toString() : "null"));
                }

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if ("success".equals(status)) {
                        showToast("Perhitungan berhasil: " + message);

                        // Optional: Tampilkan detail hasil perhitungan
                        if (data != null) {
                            JSONObject batasmaksimal = data.optJSONObject("batasmaksimal");
                            if (batasmaksimal != null && batasmaksimal.optBoolean("success", false)) {
                                double tmaWaduk = batasmaksimal.optDouble("tma_waduk", 0);
                                double batasMaksimal = batasmaksimal.optDouble("batas_maksimal", 0);
                                showToast("TMA Waduk: " + tmaWaduk + ", Batas Maksimal: " + batasMaksimal);
                            }
                        }
                    } else {
                        showToast("Gagal menghitung: " + message);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showToast("Error: " + e.getMessage());
                    Log.e("HITUNG_SEMUA", "Error", e);
                });
            }
        }).start();
    }

    private void cekDanSimpanData(String table, Map<String, String> dataMap, boolean isPengukuran) {
        if (isPengukuran) {
            // Untuk pengukuran (TMA), langsung simpan tanpa cek
            sendToServer(dataMap, table, isPengukuran);
            return;
        }

        new Thread(() -> {
            try {
                // Cek status data terlebih dahulu
                URL urlCek = new URL(CEK_DATA_URL + "?pengukuran_id=" + dataMap.get("pengukuran_id"));
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
                        // Thomson ada tapi belum lengkap → boleh insert
                        sendToServer(dataMap, table, isPengukuran);
                    } else {
                        // Sudah ada & lengkap → block insert
                        runOnUiThread(() -> showToast("Data " + table + " sudah lengkap untuk pengukuran ini!"));
                    }
                    return;
                }

                // Jika data belum ada → lanjutkan penyimpanan
                sendToServer(dataMap, table, isPengukuran);

            } catch (Exception e) {
                Log.e("CEK_SAVE", "error", e);
                runOnUiThread(() -> showToast("Gagal cek data, mencoba simpan langsung..."));

                // Fallback: simpan langsung jika gagal cek
                sendToServer(dataMap, table, isPengukuran);
            }
        }).start();
    }


    private void syncAllOfflineData(Runnable onComplete) {
        boolean adaData = !offlineDb.getAllData("pengukuran").isEmpty()
                || !offlineDb.getAllData("tma_waduk").isEmpty()
                || !offlineDb.getAllData("thomson").isEmpty()
                || !offlineDb.getAllData("sr").isEmpty()
                || !offlineDb.getAllData("bocoran").isEmpty();

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

                // Simpan pengukuran_id jika dikembalikan
                if (isPengukuran && response.has("pengukuran_id")) {
                    pengukuranId = response.getInt("pengukuran_id");
                    SharedPreferences prefs = getSharedPreferences("pengukuran", MODE_PRIVATE);
                    prefs.edit().putInt("pengukuran_id", pengukuranId).apply();
                    // clear tempId because now we have real id
                    tempId = null;
                }

                runOnUiThread(() -> {
                    if ("success".equals(status)) {
                        showToast(message);
                        if (isPengukuran) {
                            // Tutup modal
                            hideModal();
                            Toast.makeText(InputDataActivity.this, "Data pengukuran tersimpan", Toast.LENGTH_SHORT).show();

                            // Setelah sukses insert pengukuran:
                            // refresh spinner & pilih tanggal yang baru diinput
                            String tanggalBaru = dataMap.get("tanggal"); // dari modal
                            refreshPengukuranList();
                        }
                    } else {
                        showToast("Error: " + message);
                    }
                });

            } catch (Exception e) {
                Log.e("SEND", "error", e);
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