package com.example.app_rightpiezo;

import android.app.DatePickerDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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

public class InputDataRightPiezo extends AppCompatActivity {

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

    // Form utama Right Piezo
    private TextInputEditText inputTMA, inputChHujan, inputFeet, inputInch;
    private Button btnSimpanTMA, btnSimpanPembacaan;
    private Spinner spinnerPengukuran, spinnerLokasi;
    private Button btnPilihPengukuran;
    private TextView titlePiezometer;

    private Calendar calendar;
    private int pengukuranId = -1;
    private String selectedLokasi = "R-01";

    // API URLs untuk Right Piezo
    private static final String BASE_URL = "http://192.168.1.12/GHW/api-apps/public/rightpiezo/";
    private static final String INSERT_DATA_URL = BASE_URL + "input";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "get/pengukuran";
    private static final String GET_DATA_URL = BASE_URL + "input/data";
    private static final String HITUNG_URL = BASE_URL + "hitung";

    // Data pengukuran
    private final Map<String, Integer> pengukuranMap = new HashMap<>();
    private final List<String> tanggalList = new ArrayList<>();
    private ArrayAdapter<String> pengukuranAdapter;
    private ArrayAdapter<String> lokasiAdapter;

    // Daftar lokasi piezometer RIGHT PIEZO - DIUBAH
    private final String[] LOKASI_PIEZOMETER = {
            "R-01", "R-02", "R-03", "R-04", "R-05", "R-06",
            "R-07", "R-08", "R-09", "R-10", "R-11", "R-12",
            "IPZ-01", "PZ-04"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data_rightpiezo);

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

        // Set click listeners untuk modal
        if (modalBtnSubmitPengukuran != null) modalBtnSubmitPengukuran.setOnClickListener(v -> handleModalPengukuran());
        if (btnCloseModal != null) btnCloseModal.setOnClickListener(v -> hideModal());
    }

    private void initFormComponents() {
        inputTMA = findViewById(R.id.inputTMA);
        inputChHujan = findViewById(R.id.inputChHujan);
        inputFeet = findViewById(R.id.inputFeet);
        inputInch = findViewById(R.id.inputInch);

        btnSimpanTMA = findViewById(R.id.btnSimpanTMA);
        btnSimpanPembacaan = findViewById(R.id.btnSimpanPembacaan);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        spinnerLokasi = findViewById(R.id.spinnerLokasi);
        btnPilihPengukuran = findViewById(R.id.btnPilihPengukuran);
        titlePiezometer = findViewById(R.id.titlePiezometer);

        // Set click listeners
        btnSimpanTMA.setOnClickListener(v -> handleSimpanTMA());
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
                loadExistingData(); // Hanya load ketika tombol ditekan
            } else {
                showToast("❌ Pilih tanggal pengukuran terlebih dahulu");
            }
        });

        // Auto-load saat spinner berubah - DIKOMENTARI
        spinnerPengukuran.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // JANGAN panggil loadExistingData() di sini
                String selected = position >= 0 && position < tanggalList.size() ? tanggalList.get(position) : null;
                if (selected != null && pengukuranMap.containsKey(selected)) {
                    pengukuranId = pengukuranMap.get(selected);
                    // loadExistingData(); // ⚠️ COMMENT BARIS INI
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
                // loadExistingData(); // ⚠️ COMMENT BARIS INI
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

    private void clearTMASection() {
        runOnUiThread(() -> {
            inputTMA.setText("");
            inputChHujan.setText("");
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

    private void populateForm(JSONObject data) {
        try {
            if (data.has("feet")) inputFeet.setText(data.getString("feet"));
            if (data.has("inch")) inputInch.setText(data.getString("inch"));
        } catch (Exception e) {
            Log.e("POPULATE_FORM", "Error populating form: " + e.getMessage());
        }
    }

    // HANDLE SIMPAN DATA
    private void handleSimpanTMA() {
        if (pengukuranId == -1) {
            showToast("❌ Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        if (!validateTMAFields()) {
            showToast("❌ Harap isi field TMA");
            return;
        }

        updateTMAPengukuran();
    }

    private void handleSimpanPembacaan() {
        if (pengukuranId == -1) {
            showToast("❌ Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        if (!validatePembacaanFields()) {
            showToast("❌ Harap isi minimal satu field pembacaan");
            return;
        }

        simpanDataPembacaan();
    }

    private void updateTMAPengukuran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "update_tma");
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("tma", inputTMA.getText().toString().trim());
        data.put("ch_hujan", inputChHujan.getText().toString().trim());

        Log.d("RIGHTPIEZO_API", "Mengupdate TMA pengukuran: " + data.toString());
        sendToServer(data, "TMA");
    }

    private void simpanDataPembacaan() {
        // CEK DATA EXISTING SEBELUM SIMPAN
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
                    if (response.getString("status").equals("success")) {
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
                    simpanDataPembacaanToServer();
                });

            } catch (Exception e) {
                Log.e("CEK_DATA", "Error cek data: " + e.getMessage());
                // Jika error saat cek, tetap lanjutkan penyimpanan
                runOnUiThread(() -> {
                    simpanDataPembacaanToServer();
                });
            }
        }).start();
    }

    private void simpanDataPembacaanToServer() {
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
            // Validasi inch harus angka (opsional)
            if (isNumeric(inchValue)) {
                data.put("inch", inchValue);
            } else {
                showToast("❌ Nilai inch harus angka");
                return;
            }
        }

        Log.d("RIGHTPIEZO_API", "Menyimpan data " + selectedLokasi + ": " + data.toString());
        sendToServerWithCalculation(data, "Pembacaan"); // GUNAKAN METHOD BARU
    }

    // METHOD BARU: Kirim data ke server dan trigger perhitungan setelah sukses
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
                Log.d("RIGHTPIEZO_API", "JSON yang dikirim (" + dataType + "): " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("RIGHTPIEZO_API", "Response Code (" + dataType + "): " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("RIGHTPIEZO_API", "Response Body (" + dataType + "): " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            showToast("✅ " + message);
                            if (dataType.equals("TMA")) {
                                clearTMASection();
                            } else if (dataType.equals("Pembacaan")) {
                                clearPembacaanSection();
                                // TRIGGER PERHITUNGAN SETELAH SIMPAN BERHASIL
                                triggerPerhitungan();
                            }
                            break;
                        case "info":
                            showToast("ℹ️ " + message);
                            if (dataType.equals("Pembacaan")) {
                                clearPembacaanSection();
                                // TRIGGER PERHITUNGAN MESKI DATA SUDAH ADA
                                triggerPerhitungan();
                            }
                            break;
                        case "error":
                        default:
                            showToast("❌ " + message);
                            break;
                    }
                });

            } catch (Exception e) {
                Log.e("SEND_TO_SERVER", "Error (" + dataType + "): " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal kirim " + dataType + ": " + e.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
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

    // METHOD: Kirim data ke server
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
                Log.d("RIGHTPIEZO_API", "JSON yang dikirim (" + dataType + "): " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("RIGHTPIEZO_API", "Response Code (" + dataType + "): " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("RIGHTPIEZO_API", "Response Body (" + dataType + "): " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            showToast("✅ " + message);
                            if (dataType.equals("TMA")) {
                                clearTMASection();
                            } else if (dataType.equals("Pembacaan")) {
                                clearPembacaanSection();
                                // Trigger perhitungan setelah simpan berhasil
                                triggerPerhitungan();
                            }
                            break;
                        case "info":
                            showToast("ℹ️ " + message);
                            if (dataType.equals("Pembacaan")) {
                                clearPembacaanSection();
                                triggerPerhitungan();
                            }
                            break;
                        case "error":
                        default:
                            showToast("❌ " + message);
                            break;
                    }
                });

            } catch (Exception e) {
                Log.e("SEND_TO_SERVER", "Error (" + dataType + "): " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal kirim " + dataType + ": " + e.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // METHOD: Trigger perhitungan setelah simpan pembacaan berhasil - DIUPDATE LENGKAP
    private void triggerPerhitungan() {
        if (pengukuranId == -1) {
            showToast("⚠️ Pengukuran ID tidak valid untuk perhitungan");
            return;
        }

        showToast("🧮 Memulai perhitungan " + selectedLokasi + "...");

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // Pilih salah satu endpoint perhitungan:
                // 1. Hitung semua lokasi sekaligus
                // String url = HITUNG_URL + "/all?pengukuran_id=" + pengukuranId;

                // 2. Hitung lokasi spesifik (RECOMMENDED)
                String url = HITUNG_URL + "/lokasi?pengukuran_id=" + pengukuranId + "&lokasi=" + selectedLokasi;

                Log.d("RIGHTPIEZO_HITUNG", "URL Perhitungan: " + url);

                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000); // Timeout lebih lama untuk perhitungan
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                Log.d("RIGHTPIEZO_HITUNG", "Response Code: " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("RIGHTPIEZO_HITUNG", "Response Body: " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            // Ambil hasil perhitungan jika ada
                            String hasilText = "✅ Perhitungan berhasil";
                            if (response.has("data")) {
                                JSONObject data = response.optJSONObject("data");
                                if (data != null && data.has("hasil")) {
                                    double hasil = data.optDouble("hasil", 0);
                                    hasilText = "✅ Hasil perhitungan " + selectedLokasi + ": " + String.format("%.2f", hasil);
                                }
                            }
                            showToast(hasilText);
                            break;
                        case "info":
                            showToast("ℹ️ " + message);
                            break;
                        case "error":
                        default:
                            showToast("❌ Gagal perhitungan: " + message);
                            break;
                    }
                });

            } catch (Exception e) {
                Log.e("TRIGGER_HITUNG", "Error perhitungan: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Error perhitungan: " + e.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // VALIDATION METHODS
    private boolean validateTMAFields() {
        return !inputTMA.getText().toString().trim().isEmpty() ||
                !inputChHujan.getText().toString().trim().isEmpty();
    }

    private boolean validatePembacaanFields() {
        // FEET bisa teks atau angka, INCH harus angka (tapi opsional)
        return !inputFeet.getText().toString().trim().isEmpty() ||
                !inputInch.getText().toString().trim().isEmpty();
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
            showToast("📱 Tidak ada internet");
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
                Log.d("RIGHTPIEZO_API", "Response get-pengukuran: " + responseBody);

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
                            String tempId = item.optString("temp_id", ""); // TAMBAHAN: baca temp_id

                            if (!tanggal.isEmpty() && id != -1) {
                                // Gunakan temp_id jika ada, jika tidak gunakan tanggal
                                String displayText = !tempId.isEmpty() ? tempId : tanggal;
                                tanggalList.add(displayText);
                                pengukuranMap.put(displayText, id);
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
                        });
                    }
                } else {
                    String message = response.optString("message", "Gagal load data");
                    runOnUiThread(() -> {
                        showToast("❌ Error: " + message);
                    });
                }

            } catch (Exception e) {
                Log.e("LOAD_PENGUKURAN", "Error: ", e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal load data pengukuran: " + e.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // HANDLE MODAL PENGUKURAN
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

        Log.d("RIGHTPIEZO_API", "Mengirim data pengukuran: " + data.toString());
        sendPengukuranToServer(data);
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

                // Generate temp_id unik
                String tempId = "RPZ_" + System.currentTimeMillis();
                dataMap.put("temp_id", tempId);

                JSONObject jsonData = new JSONObject();
                for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                    jsonData.put(entry.getKey(), entry.getValue());
                }

                String jsonString = jsonData.toString();
                Log.d("RIGHTPIEZO_API", "JSON pengukuran: " + jsonString);

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("RIGHTPIEZO_API", "Response Code: " + responseCode);

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String responseBody = sb.toString();
                Log.d("RIGHTPIEZO_API", "Response Body: " + responseBody);

                JSONObject response = new JSONObject(responseBody);
                String status = response.optString("status", "");
                String message = response.optString("message", "");

                if (response.has("pengukuran_id")) {
                    pengukuranId = response.optInt("pengukuran_id", -1);
                    Log.d("RIGHTPIEZO_API", "Pengukuran ID diterima: " + pengukuranId);
                }

                runOnUiThread(() -> {
                    switch (status.toLowerCase()) {
                        case "success":
                            showToast("✅ " + message);
                            hideModal();
                            break;
                        case "info":
                            showToast("ℹ️ " + message);
                            if (response.has("pengukuran_id")) {
                                pengukuranId = response.optInt("pengukuran_id", -1);
                                hideModal();
                            }
                            break;
                        case "error":
                        default:
                            showToast("❌ " + message);
                            break;
                    }
                });

            } catch (Exception e) {
                Log.e("SEND_PENGUKURAN", "Error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast("❌ Gagal kirim: " + e.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // UTILITY METHODS
    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d("RIGHTPIEZO_TOAST", "Pesan: " + message);
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