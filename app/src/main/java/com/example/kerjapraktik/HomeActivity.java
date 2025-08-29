package com.example.kerjapraktik;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout btnInput, btnInputHP2, btnHistory;
    private DatabaseHelper dbHelper;
    private ApiService api;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dbHelper = new DatabaseHelper(this);
        api = ApiClient.getClient().create(ApiService.class);

        btnInput = findViewById(R.id.btnInput);
        btnInputHP2 = findViewById(R.id.btnInputHP2);
        btnHistory = findViewById(R.id.btnHistory);

        if (isOnline()) {
            syncDataFromServer();
        } else {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
        }

        btnInput.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, InputDataActivity.class);
            intent.putExtra("device_mode", "hp1");
            startActivity(intent);
        });

        btnInputHP2.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, InputData2Activity.class);
            intent.putExtra("device_mode", "hp2");
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void syncDataFromServer() {
        Log.d(TAG, "Memulai sinkronisasi data dari server...");

        // === 1. Data Pengukuran ===
        api.getPengukuran().enqueue(new Callback<PengukuranResponse>() {
            @Override
            public void onResponse(Call<PengukuranResponse> call, Response<PengukuranResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PengukuranModel> pengukuranList = response.body().getData();
                    int successCount = 0;
                    int totalCount = pengukuranList.size();

                    for (PengukuranModel p : pengukuranList) {
                        try {
                            dbHelper.insertPengukuran(
                                    p.getId(),
                                    p.getTahun(),
                                    p.getBulan(),
                                    p.getPeriode(),
                                    p.getTanggal(),
                                    p.getTmaWaduk(),
                                    p.getCurahHujan(),
                                    p.getTempId()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan pengukuran: " + p.getTanggal(), e);
                        }
                    }

                    String message = "Pengukuran: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PengukuranResponse> call, Throwable t) {
                Log.e(TAG, "Error pengukuran: " + t.getMessage(), t);
            }
        });

        // === 2. Data Thomson Weir ===
        api.getThomson().enqueue(new Callback<ThomsonResponse>() {
            @Override
            public void onResponse(Call<ThomsonResponse> call, Response<ThomsonResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ThomsonWeirModel> thomsonList = response.body().getData();
                    int successCount = 0;
                    int totalCount = thomsonList.size();

                    for (ThomsonWeirModel t : thomsonList) {
                        try {
                            dbHelper.insertThomsonWeir(
                                    t.getPengukuran_id(),
                                    t.getA1_r(),
                                    t.getA1_l(),
                                    t.getB1(),
                                    t.getB3(),
                                    t.getB5()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan thomson, pengukuran ID=" + t.getPengukuran_id(), e);
                        }
                    }

                    String message = "Thomson: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ThomsonResponse> call, Throwable t) {
                Log.e(TAG, "Error thomson: " + t.getMessage(), t);
            }
        });

        // === 3. Data SR (pakai insertSRFull) ===
        api.getSR().enqueue(new Callback<SRResponse>() {
            @Override
            public void onResponse(Call<SRResponse> call, Response<SRResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SRModel> srList = response.body().getData();
                    int successCount = 0;
                    int totalCount = srList.size();

                    for (SRModel s : srList) {
                        try {
                            dbHelper.insertSRFull(
                                    s.getPengukuranId(),
                                    s.getSr1Kode(), s.getSr1Nilai(),
                                    s.getSr40Kode(), s.getSr40Nilai(),
                                    s.getSr66Kode(), s.getSr66Nilai(),
                                    s.getSr68Kode(), s.getSr68Nilai(),
                                    s.getSr70Kode(), s.getSr70Nilai(),
                                    s.getSr79Kode(), s.getSr79Nilai(),
                                    s.getSr81Kode(), s.getSr81Nilai(),
                                    s.getSr83Kode(), s.getSr83Nilai(),
                                    s.getSr85Kode(), s.getSr85Nilai(),
                                    s.getSr92Kode(), s.getSr92Nilai(),
                                    s.getSr94Kode(), s.getSr94Nilai(),
                                    s.getSr96Kode(), s.getSr96Nilai(),
                                    s.getSr98Kode(), s.getSr98Nilai(),
                                    s.getSr100Kode(), s.getSr100Nilai(),
                                    s.getSr102Kode(), s.getSr102Nilai(),
                                    s.getSr104Kode(), s.getSr104Nilai(),
                                    s.getSr106Kode(), s.getSr106Nilai()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan SR, pengukuran ID=" + s.getPengukuranId(), e);
                        }
                    }

                    String message = "SR: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SRResponse> call, Throwable t) {
                Log.e(TAG, "Error SR: " + t.getMessage(), t);
            }
        });

        // === 4. Data Bocoran Baru (pakai value + kode) ===
        api.getBocoran().enqueue(new Callback<BocoranResponse>() {
            @Override
            public void onResponse(Call<BocoranResponse> call, Response<BocoranResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BocoranBaruModel> bocoranList = response.body().getData();
                    int successCount = 0;
                    int totalCount = bocoranList.size();

                    for (BocoranBaruModel b : bocoranList) {
                        try {
                            dbHelper.insertBocoranFull(
                                    b.getPengukuran_id(),
                                    b.getElv_624_t1(), b.getElv_624_t1_kode(),
                                    b.getElv_615_t2(), b.getElv_615_t2_kode(),
                                    b.getPipa_p1(), b.getPipa_p1_kode()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan bocoran, pengukuran ID=" + b.getPengukuran_id(), e);
                        }
                    }

                    String message = "Bocoran: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BocoranResponse> call, Throwable t) {
                Log.e(TAG, "Error bocoran: " + t.getMessage(), t);
            }
        });
    }
}
