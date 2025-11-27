package com.example.app.exstenso;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExstensoHomeActivity extends AppCompatActivity {
    private static final String TAG = "ExstensoHomeActivity";

    // UI Components
    private LinearLayout btnInputData, btnHistory;
    private TextView syncStatusText;

    // Sync System
    private Handler syncHandler = new Handler();
    private ExstensoDatabaseHelper dbHelper;
    private ExstensoApiService apiService;

    // Sync Tracking
    private AtomicInteger pendingSyncOperations = new AtomicInteger(0);
    private int totalSyncOperations = 13; // Total tables to sync

    // Statistics
    private int successCount = 0;
    private int failureCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_extenso);

        Log.d(TAG, "🚀 ExstensoHomeActivity created");

        initViews();
        setupDatabase();
        setupApiService();
        setupClickListeners();
        startAutoSync();
    }

    private void initViews() {
        try {
            btnInputData = findViewById(R.id.btnInputData);
            btnHistory = findViewById(R.id.btnHistory);


            Log.d(TAG, "✅ UI components initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing UI components: " + e.getMessage());
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDatabase() {
        try {
            dbHelper = new ExstensoDatabaseHelper(this);
            dbHelper.logAllTableCounts();
            Log.d(TAG, "✅ Database setup completed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting up database: " + e.getMessage());
        }
    }

    private void setupApiService() {
        try {
            apiService = RetrofitClient.getApiService();
            Log.d(TAG, "✅ API service setup completed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting up API service: " + e.getMessage());
        }
    }

    private void setupClickListeners() {
        // Input Data Button
        if (btnInputData != null) {
            btnInputData.setOnClickListener(v -> {
                animateButtonClick(v, this::goToInputDataActivity);
            });
            Log.d(TAG, "✅ Input Data button listener set");
        }

        // History Button
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                animateButtonClick(v, this::goToHistoryActivity);
            });
            Log.d(TAG, "✅ History button listener set");
        }
    }

    // ==================== SYNC SYSTEM ====================

    private void startAutoSync() {
        Log.d(TAG, "⏰ Starting auto-sync check...");
        syncHandler.postDelayed(() -> {
            if (isOnline()) {
                Log.d(TAG, "📱 Device online, memulai sinkronisasi otomatis...");
                syncAllDataFromServer();
            } else {
                Log.d(TAG, "📱 Device offline, sinkronisasi ditunda");
                updateSyncStatusText("📱 Mode Offline");
            }
        }, 1500);
    }

    private void syncAllDataFromServer() {
        Log.d(TAG, "🚀 ================================");
        Log.d(TAG, "🚀 Memulai sinkronisasi semua data Exstenso dari server...");
        Log.d(TAG, "🚀 ================================");

        Toast.makeText(this, "🔄 Sinkronisasi data dimulai...", Toast.LENGTH_SHORT).show();

        // Reset counters
        successCount = 0;
        failureCount = 0;
        pendingSyncOperations.set(totalSyncOperations);
        updateSyncStatusText("Memulai sinkronisasi...");

        // Sequential sync dengan delay bertahap
        syncHandler.postDelayed(this::syncPengukuran, 100);
        syncHandler.postDelayed(this::syncAllPembacaan, 300);
        syncHandler.postDelayed(this::syncAllDeformasi, 600);
        syncHandler.postDelayed(this::syncAllReadings, 900);
    }

    // ==================== INDIVIDUAL SYNC METHODS ====================

    private void syncPengukuran() {
        Log.d(TAG, "🔄 [1/13] Memulai sync Pengukuran...");
        if (apiService == null) {
            Log.e(TAG, "❌ API service not initialized");
            checkSyncCompletion();
            return;
        }

        apiService.getPengukuranEks().enqueue(new Callback<ApiResponse<List<PengukuranEksModel>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<PengukuranEksModel>>> call, Response<ApiResponse<List<PengukuranEksModel>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<PengukuranEksModel> pengukuranList = response.body().getData();
                    Log.d(TAG, "✅ [1/13] Dapat " + pengukuranList.size() + " data Pengukuran");

                    int inserted = 0;
                    for (PengukuranEksModel item : pengukuranList) {
                        long result = dbHelper.insertOrUpdatePengukuran(item);
                        if (result > 0) inserted++;
                    }
                    successCount++;
                    Log.d(TAG, "✅ [1/13] Sync Pengukuran selesai: " + inserted + " data diproses");
                } else {
                    failureCount++;
                    String errorMsg = response.body() != null ? response.body().getMessage() : "Unknown error";
                    Log.e(TAG, "❌ [1/13] Gagal sync Pengukuran: " + errorMsg);
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<PengukuranEksModel>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [1/13] Gagal sync Pengukuran: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncAllPembacaan() {
        syncPembacaanEx1();
        syncPembacaanEx2();
        syncPembacaanEx3();
        syncPembacaanEx4();
    }

    private void syncPembacaanEx1() {
        Log.d(TAG, "🔄 [2/13] Memulai sync Pembacaan Ex1...");
        apiService.getPembacaanEx1().enqueue(new Callback<ApiResponse<List<PembacaanEx1Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<PembacaanEx1Model>>> call, Response<ApiResponse<List<PembacaanEx1Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<PembacaanEx1Model> list = response.body().getData();
                    Log.d(TAG, "✅ [2/13] Dapat " + list.size() + " data Pembacaan Ex1");

                    for (PembacaanEx1Model item : list) {
                        dbHelper.insertOrUpdatePembacaanEx1(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [2/13] Sync Pembacaan Ex1 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [2/13] Gagal sync Pembacaan Ex1");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<PembacaanEx1Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [2/13] Gagal sync Pembacaan Ex1: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPembacaanEx2() {
        Log.d(TAG, "🔄 [3/13] Memulai sync Pembacaan Ex2...");
        apiService.getPembacaanEx2().enqueue(new Callback<ApiResponse<List<PembacaanEx2Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<PembacaanEx2Model>>> call, Response<ApiResponse<List<PembacaanEx2Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<PembacaanEx2Model> list = response.body().getData();
                    Log.d(TAG, "✅ [3/13] Dapat " + list.size() + " data Pembacaan Ex2");

                    for (PembacaanEx2Model item : list) {
                        dbHelper.insertOrUpdatePembacaanEx2(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [3/13] Sync Pembacaan Ex2 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [3/13] Gagal sync Pembacaan Ex2");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<PembacaanEx2Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [3/13] Gagal sync Pembacaan Ex2: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPembacaanEx3() {
        Log.d(TAG, "🔄 [4/13] Memulai sync Pembacaan Ex3...");
        apiService.getPembacaanEx3().enqueue(new Callback<ApiResponse<List<PembacaanEx3Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<PembacaanEx3Model>>> call, Response<ApiResponse<List<PembacaanEx3Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<PembacaanEx3Model> list = response.body().getData();
                    Log.d(TAG, "✅ [4/13] Dapat " + list.size() + " data Pembacaan Ex3");

                    for (PembacaanEx3Model item : list) {
                        dbHelper.insertOrUpdatePembacaanEx3(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [4/13] Sync Pembacaan Ex3 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [4/13] Gagal sync Pembacaan Ex3");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<PembacaanEx3Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [4/13] Gagal sync Pembacaan Ex3: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPembacaanEx4() {
        Log.d(TAG, "🔄 [5/13] Memulai sync Pembacaan Ex4...");
        apiService.getPembacaanEx4().enqueue(new Callback<ApiResponse<List<PembacaanEx4Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<PembacaanEx4Model>>> call, Response<ApiResponse<List<PembacaanEx4Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<PembacaanEx4Model> list = response.body().getData();
                    Log.d(TAG, "✅ [5/13] Dapat " + list.size() + " data Pembacaan Ex4");

                    for (PembacaanEx4Model item : list) {
                        dbHelper.insertOrUpdatePembacaanEx4(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [5/13] Sync Pembacaan Ex4 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [5/13] Gagal sync Pembacaan Ex4");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<PembacaanEx4Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [5/13] Gagal sync Pembacaan Ex4: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncAllDeformasi() {
        syncDeformasiEx1();
        syncDeformasiEx2();
        syncDeformasiEx3();
        syncDeformasiEx4();
    }

    private void syncDeformasiEx1() {
        Log.d(TAG, "🔄 [6/13] Memulai sync Deformasi Ex1...");
        apiService.getDeformasiEx1().enqueue(new Callback<ApiResponse<List<DeformasiEx1Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DeformasiEx1Model>>> call, Response<ApiResponse<List<DeformasiEx1Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<DeformasiEx1Model> list = response.body().getData();
                    Log.d(TAG, "✅ [6/13] Dapat " + list.size() + " data Deformasi Ex1");

                    for (DeformasiEx1Model item : list) {
                        dbHelper.insertOrUpdateDeformasiEx1(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [6/13] Sync Deformasi Ex1 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [6/13] Gagal sync Deformasi Ex1");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<DeformasiEx1Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [6/13] Gagal sync Deformasi Ex1: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncDeformasiEx2() {
        Log.d(TAG, "🔄 [7/13] Memulai sync Deformasi Ex2...");
        apiService.getDeformasiEx2().enqueue(new Callback<ApiResponse<List<DeformasiEx2Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DeformasiEx2Model>>> call, Response<ApiResponse<List<DeformasiEx2Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<DeformasiEx2Model> list = response.body().getData();
                    Log.d(TAG, "✅ [7/13] Dapat " + list.size() + " data Deformasi Ex2");

                    for (DeformasiEx2Model item : list) {
                        dbHelper.insertOrUpdateDeformasiEx2(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [7/13] Sync Deformasi Ex2 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [7/13] Gagal sync Deformasi Ex2");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<DeformasiEx2Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [7/13] Gagal sync Deformasi Ex2: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncDeformasiEx3() {
        Log.d(TAG, "🔄 [8/13] Memulai sync Deformasi Ex3...");
        apiService.getDeformasiEx3().enqueue(new Callback<ApiResponse<List<DeformasiEx3Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DeformasiEx3Model>>> call, Response<ApiResponse<List<DeformasiEx3Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<DeformasiEx3Model> list = response.body().getData();
                    Log.d(TAG, "✅ [8/13] Dapat " + list.size() + " data Deformasi Ex3");

                    for (DeformasiEx3Model item : list) {
                        dbHelper.insertOrUpdateDeformasiEx3(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [8/13] Sync Deformasi Ex3 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [8/13] Gagal sync Deformasi Ex3");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<DeformasiEx3Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [8/13] Gagal sync Deformasi Ex3: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncDeformasiEx4() {
        Log.d(TAG, "🔄 [9/13] Memulai sync Deformasi Ex4...");
        apiService.getDeformasiEx4().enqueue(new Callback<ApiResponse<List<DeformasiEx4Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DeformasiEx4Model>>> call, Response<ApiResponse<List<DeformasiEx4Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<DeformasiEx4Model> list = response.body().getData();
                    Log.d(TAG, "✅ [9/13] Dapat " + list.size() + " data Deformasi Ex4");

                    for (DeformasiEx4Model item : list) {
                        dbHelper.insertOrUpdateDeformasiEx4(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [9/13] Sync Deformasi Ex4 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [9/13] Gagal sync Deformasi Ex4");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<DeformasiEx4Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [9/13] Gagal sync Deformasi Ex4: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncAllReadings() {
        syncReadingsEx1();
        syncReadingsEx2();
        syncReadingsEx3();
        syncReadingsEx4();
    }

    private void syncReadingsEx1() {
        Log.d(TAG, "🔄 [10/13] Memulai sync Readings Ex1...");
        apiService.getReadingsEx1().enqueue(new Callback<ApiResponse<List<ReadingsEx1Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ReadingsEx1Model>>> call, Response<ApiResponse<List<ReadingsEx1Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<ReadingsEx1Model> list = response.body().getData();
                    Log.d(TAG, "✅ [10/13] Dapat " + list.size() + " data Readings Ex1");

                    for (ReadingsEx1Model item : list) {
                        dbHelper.insertOrUpdateReadingsEx1(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [10/13] Sync Readings Ex1 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [10/13] Gagal sync Readings Ex1");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ReadingsEx1Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [10/13] Gagal sync Readings Ex1: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncReadingsEx2() {
        Log.d(TAG, "🔄 [11/13] Memulai sync Readings Ex2...");
        apiService.getReadingsEx2().enqueue(new Callback<ApiResponse<List<ReadingsEx2Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ReadingsEx2Model>>> call, Response<ApiResponse<List<ReadingsEx2Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<ReadingsEx2Model> list = response.body().getData();
                    Log.d(TAG, "✅ [11/13] Dapat " + list.size() + " data Readings Ex2");

                    for (ReadingsEx2Model item : list) {
                        dbHelper.insertOrUpdateReadingsEx2(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [11/13] Sync Readings Ex2 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [11/13] Gagal sync Readings Ex2");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ReadingsEx2Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [11/13] Gagal sync Readings Ex2: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncReadingsEx3() {
        Log.d(TAG, "🔄 [12/13] Memulai sync Readings Ex3...");
        apiService.getReadingsEx3().enqueue(new Callback<ApiResponse<List<ReadingsEx3Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ReadingsEx3Model>>> call, Response<ApiResponse<List<ReadingsEx3Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<ReadingsEx3Model> list = response.body().getData();
                    Log.d(TAG, "✅ [12/13] Dapat " + list.size() + " data Readings Ex3");

                    for (ReadingsEx3Model item : list) {
                        dbHelper.insertOrUpdateReadingsEx3(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [12/13] Sync Readings Ex3 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [12/13] Gagal sync Readings Ex3");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ReadingsEx3Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [12/13] Gagal sync Readings Ex3: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncReadingsEx4() {
        Log.d(TAG, "🔄 [13/13] Memulai sync Readings Ex4...");
        apiService.getReadingsEx4().enqueue(new Callback<ApiResponse<List<ReadingsEx4Model>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ReadingsEx4Model>>> call, Response<ApiResponse<List<ReadingsEx4Model>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<ReadingsEx4Model> list = response.body().getData();
                    Log.d(TAG, "✅ [13/13] Dapat " + list.size() + " data Readings Ex4");

                    for (ReadingsEx4Model item : list) {
                        dbHelper.insertOrUpdateReadingsEx4(item);
                    }
                    successCount++;
                    Log.d(TAG, "✅ [13/13] Sync Readings Ex4 selesai");
                } else {
                    failureCount++;
                    Log.e(TAG, "❌ [13/13] Gagal sync Readings Ex4");
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ReadingsEx4Model>>> call, Throwable t) {
                failureCount++;
                Log.e(TAG, "❌ [13/13] Gagal sync Readings Ex4: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void checkSyncCompletion() {
        int completed = totalSyncOperations - pendingSyncOperations.decrementAndGet();

        Log.d(TAG, "📊 Progress: " + completed + "/" + totalSyncOperations +
                " | Success: " + successCount + " | Failed: " + failureCount);

        if (completed >= totalSyncOperations) {
            Log.d(TAG, "🎉 ================================");
            Log.d(TAG, "🎉 SINKRONISASI SEMUA DATA SELESAI!");
            Log.d(TAG, "🎉 Berhasil: " + successCount + " | Gagal: " + failureCount);
            Log.d(TAG, "🎉 ================================");

            runOnUiThread(() -> {
                String message = "✅ Sinkronisasi selesai (" + successCount + " berhasil, " + failureCount + " gagal)";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                updateSyncStatusText("Sinkronisasi selesai");

                // Update database counts
                if (dbHelper != null) {
                    dbHelper.logAllTableCounts();
                }
            });
        }
    }

    private void updateSyncStatusText(String text) {
        runOnUiThread(() -> {
            if (syncStatusText != null) {
                syncStatusText.setText(text);
            }
        });
    }

    // ==================== UTILITY METHODS ====================

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking network connectivity: " + e.getMessage());
        }
        return false;
    }

    private void animateButtonClick(View view, Runnable action) {
        try {
            view.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(80)
                                .withEndAction(action)
                                .start();
                    })
                    .start();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in button animation: " + e.getMessage());
            // Fallback: langsung execute action tanpa animation
            action.run();
        }
    }

    // ==================== NAVIGATION METHODS ====================

    private void goToInputDataActivity() {
        try {
            Intent intent = new Intent(this, InputDataExstenso.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            Log.d(TAG, "➡️ Navigated to InputDataExstenso");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error membuka Input Data Extenso: " + e.getMessage());
            Toast.makeText(this, "Error membuka Input Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHistoryActivity() {
        try {
            Intent intent = new Intent(this, HistoryExstensoActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            Log.d(TAG, "➡️ Navigated to HistoryExstensoActivity");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error membuka History: " + e.getMessage());
            Toast.makeText(this, "Error membuka History", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== LIFECYCLE METHODS ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 ExstensoHomeActivity destroyed");

        // Clean up handlers
        if (syncHandler != null) {
            syncHandler.removeCallbacksAndMessages(null);
        }

        // Close database
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "⬅️ Back pressed, exiting ExstensoHomeActivity");
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}