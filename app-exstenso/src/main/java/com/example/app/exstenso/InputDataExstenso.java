package com.example.app.exstenso;

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

public class InputDataExstenso extends AppCompatActivity {

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

    // Form utama Exstenso
    private TextInputEditText inputDMA, inputPembacaan10, inputPembacaan20, inputPembacaan30;
    private Button btnSimpanDMA, btnSimpanPembacaan;
    private Spinner spinnerPengukuran, spinnerExType;
    private Button btnPilihPengukuran;
    private TextView titleExstenso;

    private Calendar calendar;
    private int pengukuranId = -1;
    private String selectedExType = "pembacaan_ex1";

    // API URL - Sesuaikan dengan routes exstenso
    private static final String BASE_URL = "http://192.168.1.12/GHW/api-apps/public/exstenso/";
    private static final String INSERT_DATA_URL = BASE_URL + "inputdata";
    private static final String GET_PENGUKURAN_URL = BASE_URL + "getpengukuran";

    // Data pengukuran
    private final Map<String, Integer> pengukuranMap = new HashMap<>();
    private final List<String> tanggalList = new ArrayList<>();
    private ArrayAdapter<String> pengukuranAdapter;
    private ArrayAdapter<String> exTypeAdapter;

    // Data pengukuran untuk DMA
    private String currentTahun = "";
    private String currentTanggal = "";
    private String currentPeriode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_data_exstenso);

        calendar = Calendar.getInstance();

        initModalComponents();
        initFormComponents();
        initSpinnerComponents();
        setupModalDropdowns();
        setupModalCalendar();

        // Load data pengukuran
        loadPengukuranData();

        // Tampilkan modal di awal
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

        // Set click listeners
        btnSimpanDMA.setOnClickListener(v -> handleSimpanDMA());
        btnSimpanPembacaan.setOnClickListener(v -> handleSimpanPembacaan());
    }

    private void initSpinnerComponents() {
        // Adapter untuk spinner pengukuran
        pengukuranAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tanggalList);
        pengukuranAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(pengukuranAdapter);

        // Adapter untuk spinner Ex Type
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
                // Ambil data pengukuran untuk DMA
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
                    // Ambil data pengukuran untuk DMA
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

    private void loadExistingData() {
        if (pengukuranId == -1) return;

        new Thread(() -> {
            try {
                String url = BASE_URL + "getdata?pengukuran_id=" + pengukuranId + "&type=" + selectedExType;
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
            if (data.has("dma")) inputDMA.setText(data.getString("dma"));
            if (data.has("pembacaan_10")) inputPembacaan10.setText(data.getString("pembacaan_10"));
            if (data.has("pembacaan_20")) inputPembacaan20.setText(data.getString("pembacaan_20"));
            if (data.has("pembacaan_30")) inputPembacaan30.setText(data.getString("pembacaan_30"));
        } catch (Exception e) {
            Log.e("POPULATE_FORM", "Error populating form: " + e.getMessage());
        }
    }

    // HANDLE SIMPAN DATA
    private void handleSimpanDMA() {
        if (pengukuranId == -1) {
            showToast("❌ Harap buat/pilih pengukuran terlebih dahulu");
            return;
        }

        if (!validateDMAFields()) {
            showToast("❌ Harap isi field DMA");
            return;
        }

        // Gunakan data pengukuran yang sudah ada untuk update DMA
        if (currentTahun.isEmpty() || currentTanggal.isEmpty()) {
            showToast("❌ Data pengukuran belum lengkap, harap tunggu...");
            getPengukuranDataForDMA();
            return;
        }

        updateDMAPengukuran();
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

                        // Cari data pengukuran dengan ID yang sesuai
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

    private void updateDMAPengukuran() {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "update_dma"); // GANTI: dari "pengukuran" ke "update_dma"
        data.put("pengukuran_id", String.valueOf(pengukuranId));
        data.put("dma", inputDMA.getText().toString().trim());
        // HAPUS: tahun, tanggal, periode karena tidak diperlukan

        Log.d("EXSTENSO_API", "Mengupdate DMA pengukuran: " + data.toString());
        sendToServer(data, "DMA");
    }

    private void simpanDataPembacaan() {
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

        Log.d("EXSTENSO_API", "Menyimpan data " + selectedExType + ": " + data.toString());
        sendToServer(data, "Pembacaan");
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

    // VALIDATION METHODS
    private boolean validateDMAFields() {
        return !inputDMA.getText().toString().trim().isEmpty();
    }

    private boolean validatePembacaanFields() {
        return !inputPembacaan10.getText().toString().trim().isEmpty() ||
                !inputPembacaan20.getText().toString().trim().isEmpty() ||
                !inputPembacaan30.getText().toString().trim().isEmpty();
    }

    // MODAL METHODS (sama seperti sebelumnya)
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

    // LOAD PENGUKURAN DATA (sama seperti sebelumnya)
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
                                // Ambil data pengukuran untuk DMA
                                getPengukuranDataForDMA();
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

    // HANDLE MODAL PENGUKURAN (sama seperti sebelumnya)
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